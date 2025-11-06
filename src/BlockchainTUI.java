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
        int y = 0;

        drawCenter(y++, "╔══════════════════════════════════════════════════════╗", TextColor.ANSI.CYAN);
        drawCenter(y++, "║      ⚡ CAUCHOCHAIN TUI - Blockchain Demo ⚡         ║", TextColor.ANSI.CYAN);
        drawCenter(y++, "╚══════════════════════════════════════════════════════╝", TextColor.ANSI.CYAN);
        y++;

        // Blockchain
        drawSection(y++, "Blockchain", TextColor.ANSI.BLUE);
        List<Block> blocks = blockchain.getChain();
        int start = Math.max(0, blocks.size() - 3);
        for (int i = start; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            String line = String.format("#%-2d %s", b.getIndex(), truncate(b.getHash(), 45));
            drawLine(y++, "| " + padRight(line, 55) + "|", TextColor.ANSI.WHITE);
        }
        drawLine(y++, "+-------------------------------------------------------+", TextColor.ANSI.BLUE);
        y++;

        // Wallets
        drawSection(y++, "Wallets", TextColor.ANSI.MAGENTA);
        drawLine(y++, "| Alias       Balance      |", TextColor.ANSI.MAGENTA);
        for (var e : wallets.entrySet()) {
            float bal = blockchain.getBalance(e.getValue().getAddress());
            drawLine(y++, String.format("| %-10s %10.2f |", e.getKey(), bal), TextColor.ANSI.WHITE);
        }
        drawLine(y++, "+---------------------------+", TextColor.ANSI.MAGENTA);
        y++;

        // TX pendientes
        drawSection(y++, "Pendientes", TextColor.ANSI.YELLOW);
        List<Transaction> pending = blockchain.txPool.getPending();
        if (pending.isEmpty()) drawLine(y++, "| No hay transacciones pendientes |", TextColor.ANSI.WHITE);
        else for (Transaction t : pending.subList(0, Math.min(3, pending.size()))) {
            String f = (t.fromAddress == null ? "GENESIS" : truncate(t.fromAddress, 8));
            String to = truncate(t.toAddress, 8);
            drawLine(y++, String.format("| %-8s → %-8s : %8.2f |", f, to, t.amount), TextColor.ANSI.WHITE);
        }
        drawLine(y++, "+--------------------------------+", TextColor.ANSI.YELLOW);
        y++;

        // Input o controles
        if (!inputMode.isEmpty()) {
            drawSection(y++, "Entrada", TextColor.ANSI.WHITE);
            drawLine(y++, "| " + padRight(inputPrompt, 46) + "|", TextColor.ANSI.YELLOW);
            drawLine(y++, "| > " + padRight(inputBuffer + "_", 44) + "|", TextColor.ANSI.WHITE);
            drawLine(y++, "+--------------------------------------------+", TextColor.ANSI.WHITE);
        } else {
            drawCenter(y++, "[T]=Tx   [M]=Mine   [W]=Wallet   [S]=Stats   [H]=Help   [Q]=Exit", TextColor.ANSI.GREEN);
        }
        y++;

        // Logs
        drawSection(y++, "Logs", TextColor.ANSI.CYAN);
        List<String> logs = logger.getLastLogs(5);
        for (String l : logs)
            drawLine(y++, " " + truncate(l, 55), TextColor.ANSI.WHITE);

        screen.refresh();
    }

    // ------------------ HELPERS ------------------
    private void drawSection(int row, String title, TextColor color) throws IOException {
        drawLine(row, "╔═ " + title + " ═══════════════════════════════════════════════╗", color);
    }

    private void drawLine(int row, String text, TextColor color) throws IOException {
        for (int i = 0; i < text.length() && i < screen.getTerminalSize().getColumns(); i++) {
            screen.setCharacter(i, row, new TextCharacter(text.charAt(i), color, TextColor.ANSI.BLACK));
        }
    }

    private void drawCenter(int row, String text, TextColor color) throws IOException {
        int width = screen.getTerminalSize().getColumns();
        int start = Math.max(0, (width - text.length()) / 2);
        for (int i = 0; i < text.length() && start + i < width; i++) {
            screen.setCharacter(start + i, row, new TextCharacter(text.charAt(i), color, TextColor.ANSI.BLACK));
        }
    }

    private String padRight(String s, int len) {
        return s.length() >= len ? s.substring(0, len) : s + " ".repeat(len - s.length());
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
