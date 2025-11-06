package GUI;

import model.Blockchain;
import javax.swing.*;
import java.awt.*;
import GUI.*;  // Asegura que todos los paneles estén disponibles


public class MainWindow extends JFrame {
    private final Blockchain blockchain;

    public MainWindow() {
        super("CauchoChain - Blockchain Simulator");
        this.blockchain = new Blockchain();

        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);

        // Paneles principales
        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Vista Blockchain", new BlockchainPanel(blockchain));
        tabs.add("Wallets", new WalletPanel(blockchain));
        tabs.add("Minería", new MinerPanel(blockchain));
        tabs.add("Transacciones", new TransactionPanel(blockchain));
        tabs.add("Configuración", new ConfigurationPanel(blockchain));

        // Panel superior con información general
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel statusLabel = new JLabel("Estado: Sincronizado");
        JLabel difficultyLabel = new JLabel("Dificultad: " + blockchain.getDifficulty());
        topPanel.add(statusLabel);
        topPanel.add(difficultyLabel);

        add(topPanel, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
