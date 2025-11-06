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
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public MinerPanel(Blockchain blockchain) {
        super(new BorderLayout());
        this.blockchain = blockchain;

        // Inicializar componentes antes de cualquier otra operación
        this.miner = new Miner(2.0f, "GUI_Miner");
        this.minedBlocksModel = new DefaultListModel<>();
        this.logArea = new JTextArea(10, 40);
        this.miningProgress = new JProgressBar(0, 100);
        this.hashRateLabel = new JLabel("Tasa de Hash: 0 H/s");
        this.rewardLabel = new JLabel("Recompensa Total: 0");

        // Panel superior con controles
        JPanel controlPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        JButton startButton = new JButton("Iniciar Minería");
        JButton stopButton = new JButton("Detener Minería");
        hashRateLabel = new JLabel("Tasa de Hash: 0 H/s");
        rewardLabel = new JLabel("Recompensa Total: 0");

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(hashRateLabel);
        controlPanel.add(rewardLabel);

        // Barra de progreso
        miningProgress = new JProgressBar(0, 100);
        miningProgress.setStringPainted(true);
        miningProgress.setString("En espera...");

        // Panel central con progreso y log
        JPanel centerPanel = new JPanel(new BorderLayout());
        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        centerPanel.add(miningProgress, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Panel derecho con bloques minados
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Bloques Minados"));
        minedBlocksModel = new DefaultListModel<>();
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

        // Timer para actualizar UI
        Timer uiTimer = new Timer(1000, e -> SwingUtilities.invokeLater(() -> {
            updateStats();
            updateProgress();
        }));
        uiTimer.start();
    }

    private void startMining(ActionEvent e) {
        if (!isMining) {
            isMining = true;
            miningProgress.setString("Minando...");
            log("Iniciando proceso de minería...");

            miningTimer = new Timer(100, evt -> {
                if (blockchain.pendingTransactions.isEmpty()) {
                    log("Esperando transacciones...");
                    return;
                }

                try {
                    miner.mine(blockchain);
                    Block lastBlock = blockchain.getLatestBlock();
                    if (lastBlock != null && lastBlock.getMinerAddress().equals(miner.getAddress())) {
                        addMinedBlock(lastBlock);
                    }
                } catch (Exception ex) {
                    log("Error durante la minería: " + ex.getMessage());
                }
            });
            miningTimer.start();
        }
    }

    private void stopMining(ActionEvent e) {
        if (isMining) {
            isMining = false;
            if (miningTimer != null) {
                miningTimer.stop();
            }
            miningProgress.setString("Detenido");
            log("Minería detenida.");
        }
    }

    private void updateStats() {
        if (miner != null) {
            float hashRate = miner.getHashRate();
            float totalReward = miner.getTotalMined();
            hashRateLabel.setText(String.format("Tasa de Hash: %.1f H/s", hashRate));
            rewardLabel.setText(String.format("Recompensa Total: %.2f", totalReward));
        }
    }

    private void updateProgress() {
        if (isMining) {
            int progress = (int)(Math.random() * 100);  // Simulación
            miningProgress.setValue(progress);
        }
    }

    private void addMinedBlock(Block block) {
        String blockInfo = String.format("Bloque #%d | %s | Tx: %d | Reward: %.2f",
            block.getIndex(),
            dateFormat.format(block.getTimestamp()),
            block.getTransactions().size(),
            blockchain.getMiningReward()
        );
        minedBlocksModel.add(0, blockInfo);
        log("¡Bloque minado exitosamente! " + blockInfo);
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + dateFormat.format(System.currentTimeMillis()) + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
