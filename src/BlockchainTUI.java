import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.table.Table;
import com.googlecode.lanterna.TextColor;
import java.util.concurrent.*;

import java.io.IOException;
import java.util.*;

public class BlockchainTUI {
    private Blockchain blockchain;
    private Map<String, Wallet> wallets;
    private Screen screen;
    // GUI & components
    private MultiWindowTextGUI gui;
    private Window mainWindow;
    private TextBox blockList;
    private Table<String> walletsTable;
    private Table<String> pendingTable;
    private TextBox logsBox;
    private Label statusLabel;
    private Label sparkLabel;
    // Historial pequeño para graficar tamaño del mempool en ascii
    private Deque<Integer> mempoolHistory = new ArrayDeque<>();
    private int mempoolHistoryMax = 60;
    private ScheduledExecutorService updater = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean running = true;
    private Queue<String> logs;
    private int maxLogs = 100;
    private volatile String currentInput = "";
    private volatile int menuMode = 0;
    private volatile String inputPrompt = "";

    public BlockchainTUI(Blockchain blockchain) {
        this.blockchain = blockchain;
        this.wallets = new HashMap<>();
        this.logs = new LinkedList<>();
    }

    public void addLog(String message) {
        String timestamp = String.format("[%02d:%02d:%02d]",
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            Calendar.getInstance().get(Calendar.MINUTE),
            Calendar.getInstance().get(Calendar.SECOND));
        String logEntry = timestamp + " " + message;

        synchronized (logs) {
            logs.add(logEntry);
            if (logs.size() > maxLogs) {
                logs.poll();
            }
        }
    }

    public void addWallet(String alias, Wallet wallet) {
        wallets.put(alias, wallet);
        addLog("Wallet añadida: " + alias);
    }

    public void start() throws IOException {
        // Inicializar Screen y GUI de Lanterna (MultiWindowTextGUI)
        screen = new DefaultTerminalFactory().createScreen();
        screen.startScreen();
        screen.setCursorPosition(null);

        gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLACK));

        // Construir ventana principal con GridLayout (2 columnas)
        mainWindow = new BasicWindow("CauchoChain TUI");
        Panel root = new Panel(new GridLayout(2));

        // LEFT: Blockchain panel (ListBox + sparkline)
        Panel left = new Panel(new GridLayout(1));
        left.addComponent(new Label("BLOCKCHAIN").setForegroundColor(TextColor.ANSI.CYAN));
        blockList = new TextBox(new TerminalSize(40, 10)).setReadOnly(true).setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, true));
        left.addComponent(blockList);
        sparkLabel = new Label("");
        left.addComponent(sparkLabel);

        // RIGHT TOP: Wallets
        Panel rightTop = new Panel(new GridLayout(1));
        rightTop.addComponent(new Label("WALLETS").setForegroundColor(TextColor.ANSI.MAGENTA));
        walletsTable = new Table<>("Alias", "Address", "Balance");
        walletsTable.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, true));
        rightTop.addComponent(walletsTable.withBorder(Borders.singleLine()));

        // RIGHT MIDDLE: Pending txs
        rightTop.addComponent(new Label("TX PENDIENTES").setForegroundColor(TextColor.ANSI.YELLOW));
        pendingTable = new Table<>("From", "To", "Amount");
        pendingTable.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, true));
        rightTop.addComponent(pendingTable.withBorder(Borders.singleLine()));

        // RIGHT BOTTOM: Controls + logs
        Panel controls = new Panel(new GridLayout(2));
        controls.addComponent(new Label("CONTROLES").setForegroundColor(TextColor.ANSI.GREEN), GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.BEGINNING, false, false));
        Button txButton = new Button("Crear TX", this::openTxDialog);
        Button mineButton = new Button("Minar", this::openMineDialog);
        Button statsButton = new Button("Estadísticas", () -> showStats());
        Button helpButton = new Button("Help", () -> showHelp());
        Button quitButton = new Button("Salir", () -> {
            addLog("Saliendo...");
            running = false;
            try {
                if (gui != null && gui.getActiveWindow() != null) {
                    gui.getActiveWindow().close();
                }
            } catch (Exception ignore) {}
        });
        controls.addComponent(txButton);
        controls.addComponent(mineButton);
        controls.addComponent(statsButton);
        controls.addComponent(helpButton);
        controls.addComponent(quitButton);

        logsBox = new TextBox(new TerminalSize(40, 8)).setReadOnly(true).setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, false));

        // Layout: left full height, right contains wallet/pending/controls+logs
        root.addComponent(left.withBorder(Borders.singleLine()), GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, true, 1, 3));
        root.addComponent(rightTop.withBorder(Borders.doubleLine()), GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, true));
        root.addComponent(controls.withBorder(Borders.singleLine()));
        root.addComponent(logsBox);

        mainWindow.setComponent(root);

        addLog("TUI iniciada");
        addLog("Controles: Crear TX | Minar | Estadísticas | Help | Salir");

        // Schedule periodic updates en el hilo de GUI para evitar parpadeo
        updater.scheduleAtFixedRate(() -> {
            gui.getGUIThread().invokeLater(() -> updateAllComponents());
        }, 0, 500, TimeUnit.MILLISECONDS);

        // Mostrar ventana (bloquea hasta que se cierre la ventana)
        gui.addWindowAndWait(mainWindow);

        // Cuando la ventana se cierra, detener updater y screen
        stop();
    }

    private void mempoolHistoryTrim() {
        while (mempoolHistory.size() > mempoolHistoryMax) mempoolHistory.removeFirst();
    }

    private void openTxDialog() {
        BasicWindow dialog = new BasicWindow("Crear Transacción");
        Panel p = new Panel(new GridLayout(1));
        p.addComponent(new Label("Formato: FROM TO AMOUNT"));
        TextBox box = new TextBox().setValidationPattern(null);
        p.addComponent(box);
        Panel buttons = new Panel(new GridLayout(2));
        Button ok = new Button("OK", () -> {
            String text = box.getText();
            dialog.close();
            gui.getGUIThread().invokeLater(() -> processTxInput(text));
        });
        Button cancel = new Button("Cancel", dialog::close);
        buttons.addComponent(ok);
        buttons.addComponent(cancel);
        p.addComponent(buttons);
        dialog.setComponent(p.withBorder(Borders.singleLine()));
        gui.addWindow(dialog);
    }

    private void openMineDialog() {
        BasicWindow dialog = new BasicWindow("Minar");
        Panel p = new Panel(new GridLayout(1));
        p.addComponent(new Label("Nombre del minero:"));
        TextBox box = new TextBox();
        p.addComponent(box);
        Panel buttons = new Panel(new GridLayout(2));
        Button ok = new Button("OK", () -> {
            String miner = box.getText();
            dialog.close();
            new Thread(() -> {
                addLog("Minando con " + miner + "...");
                try {
                    Miner m = new Miner(1.0f, miner);
                    m.mine(blockchain);
                    addLog("✓ Bloque minado por " + miner);
                } catch (Exception e) {
                    addLog("✗ Error minando: " + e.getMessage());
                }
            }).start();
        });
        Button cancel = new Button("Cancel", dialog::close);
        buttons.addComponent(ok);
        buttons.addComponent(cancel);
        p.addComponent(buttons);
        dialog.setComponent(p.withBorder(Borders.singleLine()));
        gui.addWindow(dialog);
    }

    private void processTxInput(String input) {
        try {
            String[] parts = input.trim().split("\\s+");
            if (parts.length == 3) {
                String from = parts[0];
                String to = parts[1];
                float amount = Float.parseFloat(parts[2]);

                if (wallets.containsKey(from)) {
                    Wallet wallet = wallets.get(from);
                    Transaction tx = wallet.createTransaction(to, amount, blockchain);
                    if (tx != null) {
                        blockchain.createTransaction(tx);
                        addLog("✓ TX: " + from + " -> " + to + " : " + amount);
                    } else {
                        addLog("✗ TX fallida (saldo insuficiente?)");
                    }
                } else {
                    addLog("✗ Wallet '" + from + "' no existe");
                }
            } else {
                addLog("✗ Formato: FROM TO AMOUNT");
            }
        } catch (NumberFormatException e) {
            addLog("✗ Monto inválido");
        } catch (Exception e) {
            addLog("✗ Error: " + e.getMessage());
        }
    }

    private void showStats() {
        addLog("=== ESTADÍSTICAS ===");
        addLog("Bloques: " + blockchain.chain.size());
        addLog("Transacciones pendientes: " + blockchain.txPool.getPending().size());
        addLog("Dificultad: " + blockchain.getDifficulty());
        addLog("Recompensa: " + blockchain.getMiningReward());
    }

    private void showHelp() {
        addLog("=== CONTROLES ===");
        addLog("[T] Crear transacción");
        addLog("[M] Minar bloque");
        addLog("[E] Ver estadísticas");
        addLog("[H] Mostrar ayuda");
        addLog("[Q] Salir");
    }

    // Actualiza los componentes de la GUI con los datos actuales de la blockchain
    private void updateAllComponents() {
        try {
            // actualizar blocks (TextBox)
            List<Block> blocks = new ArrayList<>(blockchain.chain);
            List<String> lines = new ArrayList<>();
            for (int i = Math.max(0, blocks.size() - 20); i < blocks.size(); i++) {
                Block b = blocks.get(i);
                lines.add(String.format("#%d %s", b.getIndex(), truncate(b.getHash(), 24)));
            }
            blockList.setText(String.join("\n", lines));

            // actualizar wallets
            walletsTable.getTableModel().clear();
            for (Map.Entry<String, Wallet> e : wallets.entrySet()) {
                String alias = e.getKey();
                Wallet w = e.getValue();
                float bal = blockchain.getBalance(w.getAddress());
                walletsTable.getTableModel().addRow(alias, truncate(w.getAddress(), 12), String.format("%.2f", bal));
            }

            // pending txs
            List<Transaction> pending = blockchain.txPool.getPending();
            pendingTable.getTableModel().clear();
            for (int i = 0; i < Math.min(20, pending.size()); i++) {
                Transaction t = pending.get(i);
                String from = t.fromAddress == null ? "GEN" : truncate(t.fromAddress, 8);
                String to = truncate(t.toAddress, 8);
                pendingTable.getTableModel().addRow(from, to, String.format("%.2f", t.amount));
            }

            // actualizar logs
            synchronized (logs) {
                logsBox.setText(String.join("\n", logs));
            }

            // actualizar sparkline
            int pendingSize = pending.size();
            mempoolHistory.addLast(pendingSize);
            if (mempoolHistory.size() > mempoolHistoryMax) mempoolHistoryTrim();
            sparkLabel.setText(buildSparkline(new ArrayList<>(mempoolHistory), 40));

        } catch (Exception e) {
            addLog("Error actualizando UI: " + e.getMessage());
        }
    }

    private String buildSparkline(List<Integer> values, int width) {
        if (values == null || values.isEmpty()) return "";
        int sz = Math.min(values.size(), width);
        List<Integer> tail = values.subList(Math.max(0, values.size() - sz), values.size());
        int max = tail.stream().mapToInt(Integer::intValue).max().orElse(1);
        int min = tail.stream().mapToInt(Integer::intValue).min().orElse(0);
        String blocks = "▁▂▃▄▅▆▇█";
        StringBuilder sb = new StringBuilder();
        for (int v : tail) {
            int level = 0;
            if (max != min) {
                double ratio = (double)(v - min) / (double)(max - min);
                level = (int)Math.round(ratio * (blocks.length() - 1));
            }
            sb.append(blocks.charAt(Math.max(0, Math.min(blocks.length() - 1, level))));
        }
        return sb.toString();
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen - 1) + "…" : str;
    }

    private String repeatChar(String ch, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    public void stop() throws IOException {
        running = false;
        if (updater != null) {
            updater.shutdownNow();
        }
        if (gui != null) {
            try {
                gui.getActiveWindow().close();
            } catch (Exception ignore) {}
        }
        if (screen != null) {
            try {
                screen.stopScreen();
                screen.close();
            } catch (IOException e) {
                // intentar cerrar sin lanzar
            }
        }
    }
}
