import com.googlecode.lanterna.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import model.*;
import wallet.*;
import miner.*;
import utils.Logger;
import java.io.IOException;
import java.util.*;

public class BlockchainTUI {
    private final Blockchain blockchain;
    private final Map<String, Wallet> wallets = new LinkedHashMap<>();
    private final Logger logger;

    private Screen screen;
    private volatile boolean running = true;
    private String inputBuffer = "";
    private String inputMode = "";
    private String inputPrompt = "";
    private String lastState = "";

    public BlockchainTUI(Blockchain blockchain) {
        this.blockchain = blockchain;
        this.logger = Logger.getInstance();
    }

    // ------------------ START / STOP ------------------
    public void start() throws IOException {
        screen = new DefaultTerminalFactory().createScreen();
        screen.startScreen();
        screen.setCursorPosition(null);

        logger.info("TUI iniciada. Usa [T/M/S/H/Q].");

        while (running) {
            String state = buildStateHash();
            if (!state.equals(lastState)) {
                draw();
                lastState = state;
            }

            KeyStroke key = screen.pollInput();
            if (key != null) handleInput(key);

            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }

        screen.stopScreen();
    }

    public void stop() throws IOException {
        running = false;
        if (screen != null) {
            screen.stopScreen();
            screen.close();
        }
    }

    // ------------------ INPUT HANDLING ------------------
    private void handleInput(KeyStroke key) {
        KeyType type = key.getKeyType();
        Character c = key.getCharacter();

        if (!inputMode.isEmpty()) {
            if (type == KeyType.Escape) {
                inputMode = ""; inputBuffer = ""; inputPrompt = "";
                logger.info("Operación cancelada");
            } else if (type == KeyType.Backspace && inputBuffer.length() > 0) {
                inputBuffer = inputBuffer.substring(0, inputBuffer.length() - 1);
            } else if (type == KeyType.Enter) {
                processInput();
            } else if (c != null && c >= 32 && c < 127) {
                inputBuffer += c;
            }
            return;
        }

        if (c == null) return;
        switch (Character.toLowerCase(c)) {
            case 't':
                openTxInput();
                break;
            case 'm':
                openMineInput();
                break;
            case 'w':
                openWalletInput();
                break;
            case 's':
                showStats();
                break;
            case 'h':
                showHelp();
                break;
            case 'q':
                logger.info("Saliendo de la aplicación");
                running = false;
                break;
        }
    }

    private void openTxInput() {
        inputMode = "tx";
        inputPrompt = "FROM TO AMOUNT (ej: juan pancho 25)";
        logger.info("Modo TX activo");
    }

    private void openMineInput() {
        inputMode = "mine";
        inputPrompt = "Minero (nombre o alias):";
        inputBuffer = "miner1";
        logger.info("Modo MINA activo");
    }

    private void openWalletInput() {
        inputMode = "wallet";
        inputPrompt = "Nombre de la wallet (alias):";
        inputBuffer = "";
        logger.info("Modo WALLET activo");
    }

    private void processInput() {
        String text = inputBuffer.trim();
        String mode = inputMode;
        inputMode = ""; inputBuffer = ""; inputPrompt = "";

        if (mode.equals("tx")) handleTx(text);
        else if (mode.equals("mine")) handleMine(text);
        else if (mode.equals("wallet")) handleCreateWallet(text);
    }

    // ------------------ TX & MINE ------------------
    private void handleTx(String input) {
        try {
            String[] parts = input.split("\\s+");
            if (parts.length != 3) {
                logger.error("Formato inválido. Usa: FROM TO AMOUNT");
                return;
            }

            String from = parts[0], to = parts[1];
            float amount = Float.parseFloat(parts[2]);

            if (!wallets.containsKey(from)) {
                logger.error("Wallet inexistente: " + from);
                return;
            }
            Wallet w = wallets.get(from);
            String toAddr = wallets.containsKey(to) ? wallets.get(to).getAddress() : to;

            Transaction tx = w.createTransaction(toAddr, amount, blockchain);
            if (tx != null) {
                blockchain.addTransactionToPool(tx);
                logger.info("TX creada: " + from + " → " + to + " (" + amount + ") [PENDIENTE]");
            } else {
                logger.error("TX fallida (saldo insuficiente?)");
            }

        } catch (Exception e) {
            logger.error("Error TX: " + e.getMessage());
        }
    }

    private void handleMine(String miner) {
        if (miner.isEmpty()) miner = "miner1";
        final String m = miner;

        new Thread(() -> {
            logger.info("⛏️ Minador iniciando...");
            try {
                List<Transaction> txFromPool = new ArrayList<>(blockchain.txPool.getPending());
                for (Transaction tx : txFromPool) {
                    if (!blockchain.pendingTransactions.contains(tx)) {
                        blockchain.pendingTransactions.add(tx);
                    }
                }

                Miner minerObj = new Miner(1.0f, m);
                minerObj.mine(blockchain);
            } catch (Exception e) {
                logger.error("Error minando: " + e.getMessage());
            }
        }).start();
    }

    // ------------------ DRAW ------------------
    private void draw() throws IOException {
        screen.clear();
        TerminalSize size = screen.getTerminalSize();
        int width = size.getColumns();
        int height = size.getRows();

        // Header
        drawBox(0, 0, width, 3, TextColor.ANSI.CYAN);
        drawCenter(1, "⚡ CAUCHOCHAIN TUI - Blockchain Demo ⚡", TextColor.ANSI.CYAN);

        int y = 4;
        int col1Width = width / 2 - 2;
        int col2Width = width / 2 - 2;
        int leftCol = 1;
        int rightCol = leftCol + col1Width + 2;

        // Left Panel: Blockchain + Wallets
        drawPanel(leftCol, y, col1Width, 12, "📦 Blockchain", TextColor.ANSI.BLUE);
        drawBlockchainContent(leftCol + 1, y + 2, col1Width - 2);

        drawPanel(leftCol, y + 14, col1Width, 12, "💳 Wallets", TextColor.ANSI.MAGENTA);
        drawWalletsContent(leftCol + 1, y + 16, col1Width - 2);

        // Right Panel: Transactions + Status
        drawPanel(rightCol, y, col2Width, 12, "📋 Transacciones Pendientes", TextColor.ANSI.YELLOW);
        drawTransactionsContent(rightCol + 1, y + 2, col2Width - 2);

        drawPanel(rightCol, y + 14, col2Width, 12, "📊 Estado", TextColor.ANSI.GREEN);
        drawStatusContent(rightCol + 1, y + 16, col2Width - 2);

        // Bottom: Input or Controls & Logs
        int bottomY = height - 12;
        if (!inputMode.isEmpty()) {
            drawPanel(1, bottomY, width - 2, 5, "⌨️  Entrada", TextColor.ANSI.WHITE);
            drawLine(bottomY + 2, 2, "➜ " + inputPrompt, TextColor.ANSI.YELLOW);
            drawLine(bottomY + 3, 4, "> " + inputBuffer + "_", TextColor.ANSI.WHITE);
            drawLine(bottomY + 4, 2, "(ESC=Cancelar, ENTER=Confirmar)", TextColor.ANSI.CYAN);
        } else {
            drawPanel(1, bottomY, width - 2, 5, "⌨️  Controles", TextColor.ANSI.GREEN);
            drawCenter(bottomY + 2, "[T]=Transacción  [M]=Minar  [W]=Wallet  [S]=Stats  [H]=Ayuda  [Q]=Salir", TextColor.ANSI.GREEN);
        }

        // Logs Panel
        drawPanel(1, bottomY + 6, width - 2, height - bottomY - 7, "📝 Logs", TextColor.ANSI.CYAN);
        drawLogsContent(3, bottomY + 8, width - 4);

        screen.refresh();
    }

    private void drawBox(int x, int y, int width, int height, TextColor color) throws IOException {
        // Top border
        for (int i = 0; i < width; i++) {
            screen.setCharacter(x + i, y, new TextCharacter('═', color, TextColor.ANSI.BLACK));
        }
        // Bottom border
        for (int i = 0; i < width; i++) {
            screen.setCharacter(x + i, y + height - 1, new TextCharacter('═', color, TextColor.ANSI.BLACK));
        }
        // Left & Right borders
        for (int i = 1; i < height - 1; i++) {
            screen.setCharacter(x, y + i, new TextCharacter('║', color, TextColor.ANSI.BLACK));
            screen.setCharacter(x + width - 1, y + i, new TextCharacter('║', color, TextColor.ANSI.BLACK));
        }
        // Corners
        screen.setCharacter(x, y, new TextCharacter('╔', color, TextColor.ANSI.BLACK));
        screen.setCharacter(x + width - 1, y, new TextCharacter('╗', color, TextColor.ANSI.BLACK));
        screen.setCharacter(x, y + height - 1, new TextCharacter('╚', color, TextColor.ANSI.BLACK));
        screen.setCharacter(x + width - 1, y + height - 1, new TextCharacter('╝', color, TextColor.ANSI.BLACK));
    }

    private void drawPanel(int x, int y, int width, int height, String title, TextColor color) throws IOException {
        drawBox(x, y, width, height, color);
        String titleStr = " " + title + " ";
        int titleX = x + (width - titleStr.length()) / 2;
        drawLineAt(titleX, y, titleStr, color);
    }

    private void drawBlockchainContent(int x, int y, int width) throws IOException {
        List<Block> blocks = blockchain.getChain();
        int maxRows = 9;
        int start = Math.max(0, blocks.size() - maxRows);

        for (int i = start; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            String line = String.format("Block #%d: %s", b.getIndex(), truncate(b.getHash(), width - 12));
            drawLine(y + (i - start), x, line, TextColor.ANSI.CYAN);
        }

        if (blocks.isEmpty()) {
            drawLine(y + 1, x, "Sin bloques aún...", TextColor.ANSI.WHITE);
        }
    }

    private void drawWalletsContent(int x, int y, int width) throws IOException {
        drawLine(y, x, "Alias          Balance", TextColor.ANSI.MAGENTA);
        drawLine(y + 1, x, "────────────────────────", TextColor.ANSI.MAGENTA);

        int idx = 0;
        for (var e : wallets.entrySet()) {
            if (idx >= 8) break;
            float bal = blockchain.getBalance(e.getValue().getAddress());
            String line = String.format("%-14s %10.2f", e.getKey(), bal);
            drawLine(y + 2 + idx, x, line, TextColor.ANSI.WHITE);
            idx++;
        }

        if (wallets.isEmpty()) {
            drawLine(y + 3, x, "Sin wallets...", TextColor.ANSI.WHITE);
        }
    }

    private void drawTransactionsContent(int x, int y, int width) throws IOException {
        List<Transaction> pending = blockchain.txPool.getPending();
        drawLine(y, x, "From      To        Monto", TextColor.ANSI.YELLOW);
        drawLine(y + 1, x, "─────────────────────────", TextColor.ANSI.YELLOW);

        int idx = 0;
        for (Transaction t : pending) {
            if (idx >= 8) break;
            String f = (t.fromAddress == null ? "GENESIS" : truncate(t.fromAddress, 7));
            String to = truncate(t.toAddress, 7);
            String line = String.format("%-7s → %-7s %8.2f", f, to, t.amount);
            drawLine(y + 2 + idx, x, line, TextColor.ANSI.WHITE);
            idx++;
        }

        if (pending.isEmpty()) {
            drawLine(y + 3, x, "Sin transacciones pendientes", TextColor.ANSI.WHITE);
        }
    }

    private void drawStatusContent(int x, int y, int width) throws IOException {
        drawLine(y, x, "Bloques: " + blockchain.getChain().size(), TextColor.ANSI.GREEN);
        drawLine(y + 1, x, "TX Pendientes: " + blockchain.txPool.getPending().size(), TextColor.ANSI.GREEN);
        drawLine(y + 2, x, "Dificultad: " + blockchain.getDifficulty(), TextColor.ANSI.GREEN);
        drawLine(y + 3, x, "Recompensa: " + blockchain.getMiningReward(), TextColor.ANSI.GREEN);
        drawLine(y + 4, x, "Wallets: " + wallets.size(), TextColor.ANSI.GREEN);
    }

    private void drawLogsContent(int x, int y, int width) throws IOException {
        List<String> logs = logger.getLastLogs(5);
        for (String l : logs) {
            drawLine(y++, x, truncate(l, width), TextColor.ANSI.CYAN);
        }
    }

    private void drawCenter(int row, String text, TextColor color) throws IOException {
        int width = screen.getTerminalSize().getColumns();
        int start = Math.max(0, (width - text.length()) / 2);
        drawLineAt(start, row, text, color);
    }

    private void drawLine(int row, int col, String text, TextColor color) throws IOException {
        drawLineAt(col, row, text, color);
    }

    private void drawLineAt(int x, int y, String text, TextColor color) throws IOException {
        int maxX = screen.getTerminalSize().getColumns();
        for (int i = 0; i < text.length() && x + i < maxX; i++) {
            screen.setCharacter(x + i, y, new TextCharacter(text.charAt(i), color, TextColor.ANSI.BLACK));
        }
    }

    private String truncate(String s, int len) {
        return (s == null) ? "" : s.length() > len ? s.substring(0, len - 2) + ".." : s;
    }

    private String buildStateHash() {
        StringBuilder sb = new StringBuilder();
        sb.append(blockchain.getChain().size()).append("|");
        for (Wallet w : wallets.values()) sb.append(blockchain.getBalance(w.getAddress())).append(",");
        sb.append("|").append(blockchain.txPool.getPending().size());
        sb.append("|").append(inputMode).append("|").append(inputBuffer);
        sb.append("|").append(logger.getLogs().size());
        return sb.toString();
    }


    // ------------------ WALLET MGMT ------------------
    public void addWallet(String alias, Wallet w) {
        wallets.put(alias, w);
        logger.info("Wallet añadida: " + alias);
    }

    private void showStats() {
        logger.info("=== ESTADISTICAS ===");
        logger.info("Bloques: " + blockchain.getChain().size());
        logger.info("TX pendientes: " + blockchain.txPool.getPending().size());
        logger.info("Dificultad: " + blockchain.getDifficulty());
        logger.info("Recompensa: " + blockchain.getMiningReward());
        lastState = "";
    }

    private void showHelp() {
        logger.info("=== AYUDA ===");
        logger.info("[T] Crear TX: FROM TO AMOUNT");
        logger.info("[M] Minar: procesa TX pendientes");
        logger.info("[W] Crear Wallet: define una nueva wallet");
        logger.info("[S] Ver estadisticas blockchain");
        logger.info("[H] Mostrar esta ayuda");
        logger.info("[Q] Salir de la aplicacion");
        lastState = "";
    }

    private void handleCreateWallet(String alias) {
        if (alias.isEmpty()) {
            logger.error("El alias de la wallet no puede estar vacío.");
            return;
        }
        if (wallets.containsKey(alias)) {
            logger.error("Ya existe una wallet con ese alias: " + alias);
            return;
        }

        try {
            Wallet newWallet = Wallet.createWallet(alias);
            addWallet(alias, newWallet);
            logger.info("Wallet creada: " + alias);
        } catch (Exception e) {
            logger.error("Error creando wallet: " + e.getMessage());
        }
    }
}
