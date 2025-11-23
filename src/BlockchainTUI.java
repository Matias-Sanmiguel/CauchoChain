import com.googlecode.lanterna.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import model.*;
import wallet.*;
import miner.*;
import network.*;
import utils.Logger;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BlockchainTUI {
    private final Blockchain blockchain;
    private final Map<String, Wallet> wallets = new LinkedHashMap<>();
    private final Map<String, ValidatorNode> nodes = new LinkedHashMap<>();
    private final Logger logger;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    private P2PNetworkNode p2pNode;
    private int port;

    private Screen screen;
    private volatile boolean running = true;
    private String inputBuffer = "";
    private String inputMode = "";
    private String inputPrompt = "";
    private String lastState = "";

    private int currentTab = 0; // 0=Blockchain, 1=Wallets, 2=Mining, 3=Nodes, 4=Config, 5=Transactions
    private final String[] TAB_NAMES = { "Blockchain", "Wallets", "Mineria", "Nodos", "Config", "Transacciones" };

    private boolean isMining = false;
    private Miner activeMiner;
    private int minedBlocksCount = 0;

    public BlockchainTUI(Blockchain blockchain, int port) {
        this.blockchain = blockchain;
        this.port = port;
        this.logger = Logger.getInstance();
        this.p2pNode = new P2PNetworkNode("Node-" + port, port, blockchain);
    }

    public void start() throws IOException {
        screen = new DefaultTerminalFactory().createScreen();
        screen.startScreen();
        screen.setCursorPosition(null);

        p2pNode.connect(); // Inicia el servidor P2P
        logger.info("TUI iniciada en puerto " + port + ". Presiona [H] para ayuda.");
        initializeWallets();

        while (running) {
            String state = buildStateHash();
            if (!state.equals(lastState)) {
                draw();
                lastState = state;
            }

            KeyStroke key = screen.pollInput();
            if (key != null)
                handleInput(key);

            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        screen.stopScreen();
    }

    public void stop() throws IOException {
        running = false;
        isMining = false;
        if (p2pNode != null)
            p2pNode.disconnect();
        if (screen != null) {
            screen.stopScreen();
            screen.close();
        }
    }

    private void initializeWallets() {
        try {
            Wallet wallet1 = new Wallet("Wallet-1");
            wallets.put("Wallet-1", wallet1);

            Wallet wallet2 = new Wallet("Wallet-2");
            wallets.put("Wallet-2", wallet2);

            // Solo el nodo principal (puerto 5000) crea la oferta inicial
            if (port == 5000) {
                logger.info("Nodo Maestro detectado (5000). Generando oferta inicial...");
                Transaction initialTx = new Transaction(null, wallet1.getAddress(), 100.0f);
                blockchain.createTransaction(initialTx);

                Miner initialMiner = new Miner(2.0f, "GenesisMiner");
                blockchain.minePendingTransactions(initialMiner);
                minedBlocksCount++;

                // Broadcast del bloque minado para que otros nodos (si ya estan conectados) lo
                // reciban
                // Aunque al inicio no habra nadie conectado, esto deja el estado listo.
            }

            logger.info("Wallets iniciales creadas");
        } catch (Exception e) {
            logger.error("Error inicializando wallets: " + e.getMessage());
        }
    }

    private void handleInput(KeyStroke key) {
        KeyType type = key.getKeyType();
        Character c = key.getCharacter();

        if (!inputMode.isEmpty()) {
            handleInputMode(type, c);
            return;
        }

        if (c == null) {
            if (type == KeyType.ArrowLeft)
                currentTab = Math.max(0, currentTab - 1);
            if (type == KeyType.ArrowRight)
                currentTab = Math.min(5, currentTab + 1);
            return;
        }

        switch (Character.toLowerCase(c)) {
            case '1':
                currentTab = 0;
                break;
            case '2':
                currentTab = 1;
                break;
            case '3':
                currentTab = 2;
                break;
            case '4':
                currentTab = 3;
                break;
            case '5':
                currentTab = 4;
                break;
            case '6':
                currentTab = 5;
                break;
            case 't':
                openTxInput();
                break;
            case 'w':
                openWalletInput();
                break;
            case 'n':
                openNodeInput();
                break;
            case 'm':
                toggleMining();
                break;
            case 's':
                showStats();
                break;
            case 'c':
                openConfigInput();
                break;
            case 'h':
                showHelp();
                break;
            case 'q':
                running = false;
                logger.info("Saliendo...");
                break;
        }
    }

    private void handleInputMode(KeyType type, Character c) {
        if (type == KeyType.Escape) {
            inputMode = "";
            inputBuffer = "";
            inputPrompt = "";
            logger.info("Operacion cancelada");
        } else if (type == KeyType.Backspace && inputBuffer.length() > 0) {
            inputBuffer = inputBuffer.substring(0, inputBuffer.length() - 1);
        } else if (type == KeyType.Enter) {
            processInput();
        } else if (c != null && c >= 32 && c < 127) {
            inputBuffer += c;
        }
    }

    private void openTxInput() {
        inputMode = "tx";
        inputPrompt = "FROM TO AMOUNT (ej: Wallet-1 Wallet-2 10.5)";
        inputBuffer = "";
    }

    private void openWalletInput() {
        inputMode = "wallet";
        inputPrompt = "Nombre de la wallet (ej: MiWallet):";
        inputBuffer = "";
    }

    private void openNodeInput() {
        inputMode = "node";
        inputPrompt = "IP PUERTO (ej: 127.0.0.1 5001):";
        inputBuffer = "";
    }

    private void openConfigInput() {
        inputMode = "config";
        inputPrompt = "DIFICULTAD RECOMPENSA (ej: 4 50.0):";
        inputBuffer = blockchain.getDifficulty() + " " + blockchain.getMiningReward();
    }

    private void processInput() {
        String text = inputBuffer.trim();
        String mode = inputMode;
        inputMode = "";
        inputBuffer = "";
        inputPrompt = "";

        if (mode.equals("tx"))
            handleTx(text);
        else if (mode.equals("wallet"))
            handleCreateWallet(text);
        else if (mode.equals("node"))
            handleCreateNode(text);
        else if (mode.equals("config"))
            handleConfig(text);
    }

    private void handleTx(String input) {
        try {
            String[] parts = input.split("\\s+");
            if (parts.length != 3) {
                logger.error("Formato invalido. Usa: FROM TO AMOUNT");
                return;
            }

            String from = parts[0], to = parts[1];
            float amount = Float.parseFloat(parts[2]);

            if (!wallets.containsKey(from)) {
                logger.error("Wallet origen no existe: " + from);
                return;
            }

            Wallet fromWallet = wallets.get(from);
            String toAddress = wallets.containsKey(to) ? wallets.get(to).getAddress() : to;

            Transaction tx = fromWallet.createTransaction(toAddress, amount, blockchain);
            blockchain.createTransaction(tx);
            p2pNode.broadcastTransaction(tx); // Broadcast P2P

            logger.info("TX creada y difundida: " + from + " -> " + to + " (" + amount + ")");
        } catch (NumberFormatException e) {
            logger.error("Monto invalido: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error en TX: " + e.getMessage());
        }
    }

    private void handleCreateWallet(String alias) {
        if (alias.isEmpty()) {
            logger.error("El alias no puede estar vacio");
            return;
        }
        if (wallets.containsKey(alias)) {
            logger.error("Wallet ya existe: " + alias);
            return;
        }

        try {
            Wallet newWallet = new Wallet(alias);
            wallets.put(alias, newWallet);
            logger.info("Wallet creada: " + alias);
        } catch (Exception e) {
            logger.error("Error creando wallet: " + e.getMessage());
        }
    }

    private void handleCreateNode(String input) {
        try {
            String[] parts = input.split("\\s+");
            if (parts.length != 2) {
                logger.error("Formato invalido. Usa: IP PUERTO");
                return;
            }

            String ip = parts[0];
            int targetPort = Integer.parseInt(parts[1]);

            p2pNode.connectToPeer(ip, targetPort);

            // Agregamos visualmente a la lista de nodos (aunque P2PNode maneja la conexion
            // real)
            String alias = "Peer-" + targetPort;
            ValidatorNode newNode = new ValidatorNode(alias, ip + ":" + targetPort);
            nodes.put(alias, newNode);

            logger.info("Conectando a peer: " + ip + ":" + targetPort);
        } catch (NumberFormatException e) {
            logger.error("Puerto invalido");
        } catch (Exception e) {
            logger.error("Error conectando nodo: " + e.getMessage());
        }
    }

    private void handleConfig(String input) {
        try {
            String[] parts = input.split("\\s+");
            if (parts.length != 2) {
                logger.error("Formato: DIFICULTAD RECOMPENSA");
                return;
            }

            int difficulty = Integer.parseInt(parts[0]);
            float reward = Float.parseFloat(parts[1]);

            blockchain.setDifficulty(difficulty);
            blockchain.setMiningReward(reward);

            logger.info("Configuracion actualizada: Dif=" + difficulty + ", Reward=" + reward);
        } catch (Exception e) {
            logger.error("Error en configuracion: " + e.getMessage());
        }
    }

    private void toggleMining() {
        if (!isMining) {
            startMining();
        } else {
            stopMining();
        }
    }

    private void startMining() {
        if (isMining)
            return;

        isMining = true;
        try {
            activeMiner = new Miner(2.0f, "TUI_Miner_" + System.currentTimeMillis());
            logger.info("Mineria iniciada con: " + activeMiner.getAddress());

            Thread miningThread = new Thread(() -> {
                while (isMining && running) {
                    try {
                        if (!blockchain.pendingTransactions.isEmpty()) {
                            try {
                                blockchain.minePendingTransactions(activeMiner);
                                minedBlocksCount++;
                                Block minedBlock = blockchain.getLatestBlock();
                                p2pNode.broadcastBlock(minedBlock); // Broadcast P2P
                                updateAllWalletBalances();
                                logger.info(
                                        "Bloque minado y difundido. TX pendientes ahora: "
                                                + blockchain.pendingTransactions.size());
                            } catch (Exception e) {
                                logger.error("Error en mineria: " + e.getMessage());
                                e.printStackTrace();
                            }
                        } else {
                            Thread.sleep(100);
                        }
                    } catch (InterruptedException ex) {
                        logger.info("Thread de mineria interrumpido");
                        break;
                    } catch (Exception ex) {
                        logger.error("Error en thread de mineria: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
                logger.info("Thread de mineria terminado");
            }, "MiningThread");

            miningThread.setDaemon(false);
            miningThread.start();

        } catch (Exception e) {
            logger.error("Error iniciando mineria: " + e.getMessage());
            isMining = false;
        }
    }

    private void stopMining() {
        isMining = false;
        logger.info("Mineria detenida");
    }

    private void updateAllWalletBalances() {
        for (Wallet wallet : wallets.values()) {
            wallet.getBalance(blockchain);
        }
    }

    private void draw() throws IOException {
        try {
            screen.clear();
            TerminalSize size = screen.getTerminalSize();
            if (size == null)
                return;

            int width = Math.max(40, size.getColumns());
            int height = Math.max(20, size.getRows());

            drawHeader(width, height);
            drawTabs(width);

            // Si estamos en modo input, dibujar input prominentemente
            if (!inputMode.isEmpty()) {
                int inputHeight = 8;
                int inputY = Math.max(5, (height - inputHeight) / 2);
                drawInputPanel(1, inputY, Math.max(40, width - 2), inputHeight);
            } else {
                // Modo normal: dibujar contenido
                int contentStartY = 5;
                int contentHeight = Math.max(5, height - contentStartY - 14);

                try {
                    switch (currentTab) {
                        case 0:
                            drawBlockchainTab(1, contentStartY, Math.max(20, width - 2), contentHeight);
                            break;
                        case 1:
                            drawWalletsTab(1, contentStartY, Math.max(20, width - 2), contentHeight);
                            break;
                        case 2:
                            drawMiningTab(1, contentStartY, Math.max(20, width - 2), contentHeight);
                            break;
                        case 3:
                            drawNodesTab(1, contentStartY, Math.max(20, width - 2), contentHeight);
                            break;
                        case 4:
                            drawConfigTab(1, contentStartY, Math.max(20, width - 2), contentHeight);
                            break;
                        case 5:
                            drawTransactionsTab(1, contentStartY, Math.max(20, width - 2), contentHeight);
                            break;
                    }
                } catch (Exception e) {
                    logger.error("Error dibujando tab: " + e.getMessage());
                }

                int footerY = contentStartY + contentHeight + 1;
                int logsY = footerY + 6;

                drawFooter(1, footerY, Math.max(20, width - 2));
                drawLogs(1, logsY, Math.max(20, width - 2), Math.max(3, height - logsY - 1));
            }

            screen.refresh();
        } catch (Exception ex) {
            logger.error("Error en draw: " + ex.getMessage());
        }
    }

    private void drawInputPanel(int x, int y, int width, int height) {
        try {
            drawPanel(x, y, width, height, "ENTRADA DE DATOS", TextColor.ANSI.YELLOW);

            // Instruccion del input
            String instructionLine = "Modo: " + inputMode.toUpperCase();
            drawLineAt(x + 2, y + 2, instructionLine, TextColor.ANSI.MAGENTA);

            // Prompt
            drawLineAt(x + 2, y + 3, "-> " + inputPrompt, TextColor.ANSI.YELLOW);

            // Input del usuario con cursor parpadeante
            String inputLine = "> " + inputBuffer + "_";
            drawLineAt(x + 2, y + 4, inputLine, TextColor.ANSI.GREEN);

            // Instrucciones de control
            String controlLine = "[ENTER] Confirmar  |  [ESC] Cancelar  |  [BACKSPACE] Borrar";
            drawLineAt(x + 2, y + 5, controlLine, TextColor.ANSI.CYAN);

            String infoLine = "Caracteres: " + inputBuffer.length();
            drawLineAt(x + 2, y + 6, infoLine, TextColor.ANSI.WHITE);
        } catch (Exception ex) {
            logger.error("Error dibujando panel de input: " + ex.getMessage());
        }
    }

    private void drawHeader(int width, int height) throws IOException {
        drawBox(0, 0, width, 3, TextColor.ANSI.CYAN);
        drawCenter(1, "═══ CAUCHOCHAIN TUI ═══", TextColor.ANSI.CYAN);
    }

    private void drawTabs(int width) throws IOException {
        String tabs = "  [1]Blockchain  [2]Wallets  [3]Mineria  [4]Config  [5]Transacciones  ";
        int activeX = 1;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            String tabLabel = "[" + (i + 1) + "]" + TAB_NAMES[i];
            TextColor color = (i == currentTab) ? TextColor.ANSI.WHITE : TextColor.ANSI.BLUE;
            drawLineAt(activeX, 4, tabLabel, color);
            activeX += tabLabel.length() + 2;
        }
    }

    private void drawBlockchainTab(int x, int y, int width, int height) throws IOException {
        drawPanel(x, y, width, height, "Cadena de Bloques", TextColor.ANSI.BLUE);

        List<Block> blocks = blockchain.getChain();
        int maxRows = height - 3;
        int startIdx = Math.max(0, blocks.size() - maxRows);

        drawLineAt(x + 2, y + 2, "Indice | Hash", TextColor.ANSI.CYAN);
        drawLineAt(x + 2, y + 3, "--------------------------------------------", TextColor.ANSI.CYAN);

        for (int i = startIdx; i < blocks.size() && (i - startIdx) < maxRows; i++) {
            Block b = blocks.get(i);
            String line = String.format("%4d   | %s", b.getIndex(), truncate(b.getHash(), width - 15));
            drawLineAt(x + 2, y + 4 + (i - startIdx), line, TextColor.ANSI.WHITE);
        }
    }

    private void drawWalletsTab(int x, int y, int width, int height) throws IOException {
        drawPanel(x, y, width / 2 - 1, height, "Wallets", TextColor.ANSI.MAGENTA);
        drawPanel(x + width / 2 + 1, y, width / 2 - 1, height, "Informacion", TextColor.ANSI.GREEN);

        // Wallets list (izquierda)
        drawLineAt(x + 2, y + 2, "Alias", TextColor.ANSI.MAGENTA);
        int rowCount = 0;
        for (String alias : wallets.keySet()) {
            if (rowCount >= height - 5)
                break;
            drawLineAt(x + 2, y + 3 + rowCount, "• " + alias, TextColor.ANSI.WHITE);
            rowCount++;
        }

        // Wallet balances (derecha)
        int rightX = x + width / 2 + 3;
        drawLineAt(rightX, y + 2, "Alias          Balance", TextColor.ANSI.GREEN);
        drawLineAt(rightX, y + 3, "──────────────────────", TextColor.ANSI.GREEN);

        rowCount = 0;
        for (Map.Entry<String, Wallet> e : wallets.entrySet()) {
            if (rowCount >= height - 5)
                break;
            float balance = blockchain.getBalance(e.getValue().getAddress());
            String line = String.format("%-14s %10.2f", e.getKey(), balance);
            drawLineAt(rightX, y + 4 + rowCount, line, TextColor.ANSI.WHITE);
            rowCount++;
        }
    }

    private void drawMiningTab(int x, int y, int width, int height) throws IOException {
        drawPanel(x, y, width, height, "Control de Mineria", TextColor.ANSI.YELLOW);

        String miningStatus = isMining ? "EN PROGRESO ▓▓▓" : "DETENIDO";
        TextColor statusColor = isMining ? TextColor.ANSI.GREEN : TextColor.ANSI.RED;

        drawLineAt(x + 2, y + 2, "Estado: " + miningStatus, statusColor);
        drawLineAt(x + 2, y + 3, "Bloques minados: " + minedBlocksCount, TextColor.ANSI.CYAN);

        if (activeMiner != null) {
            String minerAddr = truncate(activeMiner.getAddress(), 30);
            drawLineAt(x + 2, y + 4, "Minero: " + minerAddr, TextColor.ANSI.WHITE);
            drawLineAt(x + 2, y + 5, "Recompensa total: " + String.format("%.2f", activeMiner.getTotalMined()),
                    TextColor.ANSI.WHITE);
        } else {
            drawLineAt(x + 2, y + 4, "Minero: No iniciado", TextColor.ANSI.WHITE);
            drawLineAt(x + 2, y + 5, "Recompensa total: 0.00", TextColor.ANSI.WHITE);
        }

        drawLineAt(x + 2, y + 7, "TX Pendientes: " + blockchain.pendingTransactions.size(), TextColor.ANSI.MAGENTA);
        drawLineAt(x + 2, y + 8, "Dificultad: " + blockchain.getDifficulty(), TextColor.ANSI.MAGENTA);
        drawLineAt(x + 2, y + 9, "Recompensa por bloque: " + blockchain.getMiningReward(), TextColor.ANSI.MAGENTA);

        drawLineAt(x + 2, y + height - 3, "Presiona [M] para " + (isMining ? "PAUSAR" : "INICIAR"),
                TextColor.ANSI.YELLOW);
    }

    private void drawNodesTab(int x, int y, int width, int height) throws IOException {
        drawPanel(x, y, width, height, "Nodos de Validacion", TextColor.ANSI.CYAN);

        // Encabezados
        drawLineAt(x + 2, y + 2, "Alias                Direccion                 Estado", TextColor.ANSI.CYAN);
        drawLineAt(x + 2, y + 3, "──────────────────────────────────────────────────", TextColor.ANSI.CYAN);

        int rowCount = 0;
        for (ValidatorNode node : nodes.values()) {
            if (rowCount >= height - 5)
                break;
            String line = String.format("%-20s %-25s %s", node.getAlias(), node.getAddress(),
                    node.isActive() ? "Activo" : "Inactivo");
            drawLineAt(x + 2, y + 4 + rowCount, line, TextColor.ANSI.WHITE);
            rowCount++;
        }

        drawLineAt(x + 2, y + height - 3, "Presiona [N] para agregar nodo", TextColor.ANSI.YELLOW);
    }

    private void drawConfigTab(int x, int y, int width, int height) throws IOException {
        drawPanel(x, y, width, height, "Configuracion", TextColor.ANSI.GREEN);

        drawLineAt(x + 2, y + 2, "Parametros de Blockchain", TextColor.ANSI.GREEN);
        drawLineAt(x + 2, y + 3, "─────────────────────────", TextColor.ANSI.GREEN);

        drawLineAt(x + 2, y + 5, "Dificultad (PoW): " + blockchain.getDifficulty(), TextColor.ANSI.WHITE);
        drawLineAt(x + 2, y + 6, "Recompensa por bloque: " + blockchain.getMiningReward(), TextColor.ANSI.WHITE);
        drawLineAt(x + 2, y + 7, "Bloques en cadena: " + blockchain.getChain().size(), TextColor.ANSI.WHITE);
        drawLineAt(x + 2, y + 8, "Wallets activas: " + wallets.size(), TextColor.ANSI.WHITE);

        drawLineAt(x + 2, y + 10, "Cadena valida: " + (blockchain.isChainValid() ? "SI ✓" : "NO ✗"),
                blockchain.isChainValid() ? TextColor.ANSI.GREEN : TextColor.ANSI.RED);

        drawLineAt(x + 2, y + height - 3, "Presiona [C] para editar", TextColor.ANSI.YELLOW);
    }

    private void drawTransactionsTab(int x, int y, int width, int height) throws IOException {
        int leftWidth = width / 2 - 1;
        int rightWidth = width / 2 - 1;

        drawPanel(x, y, leftWidth, height, "Transacciones Confirmadas", TextColor.ANSI.BLUE);
        drawPanel(x + leftWidth + 2, y, rightWidth, height, "Transacciones Pendientes", TextColor.ANSI.YELLOW);

        // Confirmado
        drawLineAt(x + 2, y + 2, "De           Para         Monto", TextColor.ANSI.BLUE);
        int rowCount = 0;
        for (Block block : blockchain.getChain()) {
            for (Transaction tx : block.getTransactions()) {
                if (rowCount >= height - 5)
                    break;
                String from = tx.fromAddress == null ? "SISTEMA" : truncate(tx.fromAddress, 8);
                String to = truncate(tx.toAddress, 8);
                String line = String.format("%-12s %-12s %8.2f", from, to, tx.amount);
                drawLineAt(x + 2, y + 3 + rowCount, line, TextColor.ANSI.WHITE);
                rowCount++;
            }
            if (rowCount >= height - 5)
                break;
        }

        // Pendiente
        drawLineAt(x + leftWidth + 4, y + 2, "De           Para         Monto", TextColor.ANSI.YELLOW);
        rowCount = 0;
        for (Transaction tx : blockchain.pendingTransactions) {
            if (rowCount >= height - 5)
                break;
            String from = tx.fromAddress == null ? "SISTEMA" : truncate(tx.fromAddress, 8);
            String to = truncate(tx.toAddress, 8);
            String line = String.format("%-12s %-12s %8.2f", from, to, tx.amount);
            drawLineAt(x + leftWidth + 4, y + 3 + rowCount, line, TextColor.ANSI.WHITE);
            rowCount++;
        }
    }

    private void drawFooter(int x, int y, int width) throws IOException {
        drawPanel(x, y, width, 6, "Controles", TextColor.ANSI.GREEN);
        drawLineAt(x + 2, y + 2, "[T]=Transaccion  [W]=Wallet  [M]=Minar  [C]=Config", TextColor.ANSI.GREEN);
        drawLineAt(x + 2, y + 3, "[S]=Stats  [H]=Ayuda  [Q]=Salir  [←→]=Navegar", TextColor.ANSI.GREEN);
    }

    private void drawLogs(int x, int y, int width, int height) throws IOException {
        drawPanel(x, y, width, height, "Logs", TextColor.ANSI.CYAN);

        List<String> logs = logger.getLastLogs(height - 2);
        int idx = 0;
        for (String log : logs) {
            if (idx >= height - 2)
                break;
            drawLineAt(x + 2, y + 1 + idx, truncate(log, width - 4), TextColor.ANSI.CYAN);
            idx++;
        }
    }

    private void drawBox(int x, int y, int width, int height, TextColor color) throws IOException {
        for (int i = 0; i < width; i++) {
            screen.setCharacter(x + i, y, new TextCharacter('═', color, TextColor.ANSI.BLACK));
            screen.setCharacter(x + i, y + height - 1, new TextCharacter('═', color, TextColor.ANSI.BLACK));
        }
        for (int i = 1; i < height - 1; i++) {
            screen.setCharacter(x, y + i, new TextCharacter('║', color, TextColor.ANSI.BLACK));
            screen.setCharacter(x + width - 1, y + i, new TextCharacter('║', color, TextColor.ANSI.BLACK));
        }
        screen.setCharacter(x, y, new TextCharacter('╔', color, TextColor.ANSI.BLACK));
        screen.setCharacter(x + width - 1, y, new TextCharacter('╗', color, TextColor.ANSI.BLACK));
        screen.setCharacter(x, y + height - 1, new TextCharacter('╚', color, TextColor.ANSI.BLACK));
        screen.setCharacter(x + width - 1, y + height - 1, new TextCharacter('╝', color, TextColor.ANSI.BLACK));
    }

    private void drawPanel(int x, int y, int width, int height, String title, TextColor color) throws IOException {
        drawBox(x, y, width, height, color);
        String titleStr = " " + title + " ";
        int titleX = x + 2;
        drawLineAt(titleX, y, titleStr, color);
    }

    private void drawCenter(int row, String text, TextColor color) throws IOException {
        int width = screen.getTerminalSize().getColumns();
        int start = Math.max(0, (width - text.length()) / 2);
        drawLineAt(start, row, text, color);
    }

    private void drawLineAt(int x, int y, String text, TextColor color) throws IOException {
        try {
            if (text == null || text.isEmpty())
                return;

            TerminalSize size = screen.getTerminalSize();
            if (size == null)
                return;

            int maxX = size.getColumns();
            int maxY = size.getRows();

            // Validar coordenadas
            if (x < 0 || y < 0 || x >= maxX || y >= maxY)
                return;

            // Escribir solo caracteres validos dentro de los limites
            for (int i = 0; i < text.length() && (x + i) < maxX; i++) {
                char ch = text.charAt(i);
                if (ch >= 32 && ch < 127) {
                    screen.setCharacter(x + i, y, new TextCharacter(ch, color, TextColor.ANSI.BLACK));
                }
            }
        } catch (Exception ex) {
        }
    }

    private String truncate(String s, int len) {
        return utils.GUIUtils.truncate(s, len);
    }

    private String buildStateHash() {
        StringBuilder sb = new StringBuilder();
        sb.append(blockchain.getChain().size()).append("|");
        for (Wallet w : wallets.values())
            sb.append(blockchain.getBalance(w.getAddress())).append(",");
        sb.append("|").append(blockchain.pendingTransactions.size());
        sb.append("|").append(currentTab).append("|").append(isMining);
        sb.append("|").append(logger.getLogs().size());
        // Incluye el estado del input para que se redibuje en tiempo real
        sb.append("|").append(inputMode).append("|").append(inputBuffer);
        return sb.toString();
    }

    private void showStats() {
        logger.info("════════════ ESTADISTICAS ════════════");
        logger.info("Bloques: " + blockchain.getChain().size());
        logger.info(
                "TX Confirmadas: " + blockchain.getChain().stream().mapToInt(b -> b.getTransactions().size()).sum());
        logger.info("TX Pendientes: " + blockchain.pendingTransactions.size());
        logger.info("Dificultad: " + blockchain.getDifficulty());
        logger.info("Recompensa: " + blockchain.getMiningReward());
        logger.info("Wallets: " + wallets.size());
        logger.info("Cadena valida: " + blockchain.isChainValid());
        lastState = "";
    }

    private void showHelp() {
        logger.info("════════════ AYUDA ════════════");
        logger.info("[1-5] Cambiar tab");
        logger.info("[←→] Navegar tabs");
        logger.info("[T] Crear transaccion");
        logger.info("[W] Crear wallet");
        logger.info("[N] Crear nodo");
        logger.info("[M] Iniciar/pausar mineria");
        logger.info("[C] Configurar parametros");
        logger.info("[S] Ver estadisticas");
        logger.info("[H] Mostrar esta ayuda");
        logger.info("[Q] Salir");
        lastState = "";
    }

    public void addWallet(String alias, Wallet w) {
        wallets.put(alias, w);
        logger.info("Wallet anadida: " + alias);
    }
}
