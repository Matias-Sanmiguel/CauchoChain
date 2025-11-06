package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import miner.Miner;
import model.Block;
import model.Blockchain;

public class MinerPanel extends JPanel {
    private final Blockchain blockchain;
    private final Miner miner;
    private JTextArea logArea;
    private JProgressBar miningProgress;
    private JLabel hashRateLabel;
    private JLabel rewardLabel;
    private DefaultListModel<String> minedBlocksModel;
    private Timer miningTimer;
    private boolean isMining = false;
    private int lastBlockCount = 0;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public MinerPanel(Blockchain blockchain) {
        super(new BorderLayout());
        this.blockchain = blockchain;

        // Inicializar componentes
        this.miner = new Miner(2.0f, "GUI_Miner");
        this.minedBlocksModel = new DefaultListModel<>();
        this.logArea = new JTextArea(10, 40);
        this.miningProgress = new JProgressBar(0, 100);
        this.hashRateLabel = new JLabel("Tasa de Hash: 0 H/s");
        this.rewardLabel = new JLabel("Recompensa Total: 0");

        // Panel superior con controles
        JPanel controlPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        JButton startButton = new JButton("Iniciar Mineria");
        JButton stopButton = new JButton("Detener Mineria");

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(hashRateLabel);
        controlPanel.add(rewardLabel);

        // Barra de progreso
        miningProgress.setStringPainted(true);
        miningProgress.setString("En espera...");

        // Panel central con progreso y log
        JPanel centerPanel = new JPanel(new BorderLayout());
        logArea.setEditable(false);
        centerPanel.add(miningProgress, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Panel derecho con bloques minados
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Bloques Minados"));
        JList<String> minedBlocksList = new JList<>(minedBlocksModel);
        rightPanel.add(new JScrollPane(minedBlocksList), BorderLayout.CENTER);

        // Layout principal
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            mainPanel,
            rightPanel
        );
        splitPane.setDividerLocation(600);
        add(splitPane);

        // Acciones de los botones
        startButton.addActionListener(this::startMining);
        stopButton.addActionListener(this::stopMining);

        // Timer para actualizar UI (solo UI, no mineria)
        Timer uiTimer = new Timer(1000, e -> SwingUtilities.invokeLater(() -> {
            updateStats();
            updateProgress();
            checkForNewBlocks();
        }));
        uiTimer.start();
    }

    private void startMining(ActionEvent e) {
        if (!isMining) {
            isMining = true;
            miningProgress.setString("Minando...");
            log("Iniciando proceso de mineria...");
            lastBlockCount = blockchain.getChain().size();

            // Ejecutar mineria en un thread separado
            Thread miningThread = new Thread(() -> {
                while (isMining) {
                    try {
                        synchronized (blockchain) {
                            if (!blockchain.pendingTransactions.isEmpty()) {
                                try {
                                    miner.mine(blockchain);
                                    SwingUtilities.invokeLater(() -> {
                                        log("Bloque minado exitosamente");
                                        updateStats();
                                    });
                                } catch (Exception ex) {
                                    final String error = ex.getMessage();
                                    SwingUtilities.invokeLater(() -> log("Error minando: " + error));
                                }
                            } else {
                                SwingUtilities.invokeLater(() -> log("Esperando transacciones..."));
                            }
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            miningThread.setDaemon(true);
            miningThread.start();
        }
    }

    private void stopMining(ActionEvent e) {
        if (isMining) {
            isMining = false;
            miningProgress.setString("Detenido");
            log("Mineria detenida.");
        }
    }

    private void updateStats() {
        try {
            if (miner != null) {
                float hashRate = miner.getHashRate();
                float totalReward = miner.getBalance(blockchain);
                hashRateLabel.setText(String.format("Tasa de Hash: %.1f H/s", hashRate));
                rewardLabel.setText(String.format("Recompensa Total: %.2f", totalReward));

                // Actualizar la barra de progreso si está minando
                if (isMining && !blockchain.pendingTransactions.isEmpty()) {
                    miningProgress.setString(String.format("Minando... (%d tx pendientes)",
                        blockchain.pendingTransactions.size()));
                }
            }
        } catch (Exception ex) {
            log("Error actualizando stats: " + ex.getMessage());
        }
    }

    private void updateProgress() {
        if (isMining) {
            int progress = (int) (Math.random() * 100);
            miningProgress.setValue(progress);
        }
    }

    private void checkForNewBlocks() {
        try {
            int currentBlockCount = blockchain.getChain().size();
            if (currentBlockCount > lastBlockCount) {
                Block lastBlock = blockchain.getLatestBlock();
                if (lastBlock != null) {
                    addMinedBlock(lastBlock);
                }
                lastBlockCount = currentBlockCount;
            }
        } catch (Exception ex) {
            log("Error checando bloques: " + ex.getMessage());
        }
    }

    private void addMinedBlock(Block block) {
        try {
            String blockInfo = String.format("Bloque #%d | %s | Tx: %d | Reward: %.2f",
                block.getIndex(),
                dateFormat.format(block.getTimestamp()),
                block.getTransactions().size(),
                blockchain.getMiningReward()
            );
            minedBlocksModel.add(0, blockInfo);
        } catch (Exception ex) {
            log("Error anadiendo bloque a lista: " + ex.getMessage());
        }
    }

    private void log(String message) {
        try {
            logArea.append("[" + dateFormat.format(new java.util.Date()) + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
