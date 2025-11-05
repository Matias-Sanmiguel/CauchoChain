import com.googlecode.lanterna.*;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.table.Table;
import java.util.concurrent.*;

import java.io.IOException;
import java.util.*;

import model.*;
import wallet.*;
import miner.*;

public class BlockchainTUI {
    private final Blockchain blockchain;
    private final Map<String, Wallet> wallets;
    private Screen screen;
    // GUI & components
    private MultiWindowTextGUI gui;
    private BasicWindow mainWindow;
    private TextBox blockList;
    private Table<String> walletsTable;
    private Table<String> pendingTable;
    private TextBox logsBox;
    private Label sparkLabel;
    // Historial pequeño para graficar tamaño del mempool en ascii
    private final Deque<Integer> mempoolHistory = new ArrayDeque<>();
    private final int mempoolHistoryMax = 60;
    private final ScheduledExecutorService updater = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean running = true;
    private final Queue<String> logs;

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
            if (logs.size() > 100) {
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
        Panel root = new Panel();
        root.setLayoutManager(new GridLayout(2));

        // LEFT: Blockchain panel (ListBox + sparkline)
        Panel left = new Panel();
        left.setLayoutManager(new GridLayout(1));
        left.addComponent(new Label("BLOCKCHAIN").setForegroundColor(TextColor.ANSI.CYAN));
        blockList = new TextBox().setReadOnly(true);
        blockList.setPreferredSize(new TerminalSize(40, 10));
        left.addComponent(blockList);
        sparkLabel = new Label("");
        left.addComponent(sparkLabel);

        // RIGHT TOP: Wallets
        Panel rightTop = new Panel();
        rightTop.setLayoutManager(new GridLayout(1));
        rightTop.addComponent(new Label("WALLETS").setForegroundColor(TextColor.ANSI.MAGENTA));
        walletsTable = new Table<>("Alias", "Address", "Balance");
        rightTop.addComponent(walletsTable.withBorder(Borders.singleLine()));

        // RIGHT MIDDLE: Pending txs
        rightTop.addComponent(new Label("TX PENDIENTES").setForegroundColor(TextColor.ANSI.YELLOW));
        pendingTable = new Table<>("From", "To", "Amount");
        rightTop.addComponent(pendingTable.withBorder(Borders.singleLine()));

        // RIGHT BOTTOM: Controls + logs
        Panel controls = new Panel();
        controls.setLayoutManager(new GridLayout(2));
        controls.addComponent(new Label("CONTROLES").setForegroundColor(TextColor.ANSI.GREEN));
        controls.addComponent(new Label("")); // spacer

        Button txButton = new Button("Crear TX", this::openTxDialog);
        Button mineButton = new Button("Minar", this::openMineDialog);
        Button statsButton = new Button("Estadísticas", this::showStats);
        Button helpButton = new Button("Help", this::showHelp);
        Button quitButton = new Button("Salir", () -> {
            addLog("Saliendo...");
            running = false;
            mainWindow.close();
        });
        controls.addComponent(txButton);
        controls.addComponent(mineButton);
        controls.addComponent(statsButton);
        controls.addComponent(helpButton);
        controls.addComponent(quitButton);

        logsBox = new TextBox().setReadOnly(true);
        logsBox.setPreferredSize(new TerminalSize(40, 8));

        // Layout: left full height, right contains wallet/pending/controls+logs
        root.addComponent(left.withBorder(Borders.singleLine()));
        root.addComponent(rightTop.withBorder(Borders.doubleLine()));
        root.addComponent(controls.withBorder(Borders.singleLine()));
        root.addComponent(logsBox);

        mainWindow.setComponent(root);

        addLog("TUI iniciada");
        addLog("Controles: Crear TX | Minar | Estadísticas | Help | Salir");

        // Schedule periodic updates
        updater.scheduleAtFixedRate(this::updateAllComponents, 0, 500, TimeUnit.MILLISECONDS);

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
        Panel p = new Panel();
        p.setLayoutManager(new GridLayout(1));
        p.addComponent(new Label("Formato: FROM TO AMOUNT"));
        TextBox box = new TextBox();
        p.addComponent(box);
        Panel buttons = new Panel();
        buttons.setLayoutManager(new GridLayout(2));
        Button ok = new Button("OK", () -> {
            String text = box.getText();
            dialog.close();
            processTxInput(text);
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
        Panel p = new Panel();
        p.setLayoutManager(new GridLayout(1));
        p.addComponent(new Label("Nombre del minero:"));
        TextBox box = new TextBox();
        box.setText("miner1");
        p.addComponent(box);
        Panel buttons = new Panel();
        buttons.setLayoutManager(new GridLayout(2));
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
                    String toAddress = wallets.containsKey(to) ? wallets.get(to).getAddress() : to;
                    Transaction tx = wallet.createTransaction(toAddress, amount, blockchain);
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
        addLog("Bloques: " + blockchain.getChain().size());
        addLog("Transacciones pendientes: " + blockchain.txPool.getPending().size());
        addLog("Dificultad: " + blockchain.getDifficulty());
        addLog("Recompensa: " + blockchain.getMiningReward());
    }

    private void showHelp() {
        addLog("=== AYUDA ===");
        addLog("Crear TX: alias1 alias2 monto");
        addLog("Minar: nombre del minero");
        addLog("Stats: ver estadísticas");
        addLog("Help: mostrar esta ayuda");
        addLog("Salir: cerrar la aplicación");
    }

    // Actualiza los componentes de la GUI con los datos actuales de la blockchain
    private synchronized void updateAllComponents() {
        try {
            // actualizar blocks (TextBox)
            List<Block> blocks = new ArrayList<>(blockchain.getChain());
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
            sparkLabel.setText("Mempool: " + buildSparkline(new ArrayList<>(mempoolHistory), 40));

        } catch (Exception e) {
            // Silently ignore UI update errors
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

    public void stop() throws IOException {
        running = false;
        if (updater != null) {
            updater.shutdownNow();
        }
        if (mainWindow != null) {
            try {
                mainWindow.close();
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
