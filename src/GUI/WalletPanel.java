package GUI;

import miner.Miner;
import model.Block;
import model.Blockchain;
import model.Transaction;
import wallet.Wallet;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class WalletPanel extends JPanel {
    private final Blockchain blockchain;
    private final List<Wallet> wallets;
    private final DefaultTableModel walletsTableModel;
    private final DefaultTableModel transactionsTableModel;
    private final DefaultComboBoxModel<String> fromWalletModel;
    private final DefaultComboBoxModel<String> toWalletModel;
    private final JComboBox<String> fromWalletCombo;
    private final JComboBox<String> toWalletCombo;
    private final JTextField amountField;

    public WalletPanel(Blockchain blockchain) {
        super(new BorderLayout());
        this.blockchain = blockchain;
        this.wallets = new ArrayList<>();

        // Inicializar modelos de tabla primero
        String[] walletColumns = { "Dirección", "Alias", "Saldo" };
        String[] txColumns = { "De", "Para", "Monto", "Estado" };
        this.walletsTableModel = new DefaultTableModel(walletColumns, 0);
        this.transactionsTableModel = new DefaultTableModel(txColumns, 0);

        // Inicializar modelos de ComboBox
        this.fromWalletModel = new DefaultComboBoxModel<>();
        this.toWalletModel = new DefaultComboBoxModel<>();
        this.fromWalletCombo = new JComboBox<>(fromWalletModel);
        this.toWalletCombo = new JComboBox<>(toWalletModel);
        this.amountField = new JTextField();

        // Panel izquierdo: Lista de wallets y creación
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Wallets"));

        // Tabla de wallets
        JTable walletsTable = new JTable(walletsTableModel);
        leftPanel.add(new JScrollPane(walletsTable), BorderLayout.CENTER);

        // Botón crear wallet
        JButton createWalletButton = new JButton("Crear Nueva Wallet");
        createWalletButton.addActionListener(e -> createNewWallet());
        leftPanel.add(createWalletButton, BorderLayout.SOUTH);

        // Panel derecho: Transacciones
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Transacciones"));

        // Panel de envío
        JPanel sendPanel = new JPanel(new GridLayout(4, 2, 5, 5));

        sendPanel.add(new JLabel("Desde:"));
        sendPanel.add(fromWalletCombo);
        sendPanel.add(new JLabel("Para:"));
        sendPanel.add(toWalletCombo);
        sendPanel.add(new JLabel("Monto:"));
        sendPanel.add(amountField);

        JButton sendButton = new JButton("Enviar");
        sendButton.addActionListener(e -> sendTransaction());
        sendPanel.add(new JLabel(""));
        sendPanel.add(sendButton);

        // Historial de transacciones
        JTable txTable = new JTable(transactionsTableModel);

        rightPanel.add(sendPanel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(txTable), BorderLayout.CENTER);

        // Layout principal
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                leftPanel,
                rightPanel);
        splitPane.setDividerLocation(400);
        add(splitPane, BorderLayout.CENTER);

        // Crear algunas wallets de ejemplo y actualizar la UI inicialmente
        SwingUtilities.invokeLater(() -> {
            // Crear primera wallet con fondos iniciales
            Wallet firstWallet = new Wallet("Principal");
            wallets.add(firstWallet);

            // Crear transacción inicial y minarla
            Transaction initialTx = new Transaction(null, firstWallet.getAddress(), 50.0f);
            blockchain.createTransaction(initialTx);

            // Minar el bloque inicial
            Miner initialMiner = new Miner(2.0f, "InitialMiner");
            blockchain.minePendingTransactions(initialMiner);

            // Crear segunda wallet
            Wallet secondWallet = new Wallet("Secundaria");
            wallets.add(secondWallet);

            // Actualizar la UI
            updateWalletsList();
            updateTransactionsList();
        });

        // Timer para actualización periódica
        Timer timer = new Timer(5000, e -> SwingUtilities.invokeLater(() -> {
            updateWalletsList();
            updateTransactionsList();
        }));
        timer.start();
    }

    private void createNewWallet() {
        // Mostrar diálogo para ingresar el alias
        String alias = JOptionPane.showInputDialog(
                this,
                "Ingrese un alias para la nueva wallet:",
                "Crear Nueva Wallet",
                JOptionPane.PLAIN_MESSAGE);

        // Verificar si el usuario canceló o ingresó un alias vacío
        if (alias == null || alias.trim().isEmpty()) {
            return;
        }

        // Verificar si el alias ya existe
        for (Wallet w : wallets) {
            if (w.getAlias().equals(alias.trim())) {
                JOptionPane.showMessageDialog(
                        this,
                        "El alias ya está en uso. Por favor elija otro.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Wallet wallet = new Wallet(alias.trim());
        wallets.add(wallet);
        updateWalletsList();
        updateTransactionsList();

        JOptionPane.showMessageDialog(
                this,
                "Wallet creada exitosamente con alias: " + alias,
                "Wallet Creada",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateWalletsList() {
        walletsTableModel.setRowCount(0);
        fromWalletModel.removeAllElements();
        toWalletModel.removeAllElements();

        for (Wallet wallet : wallets) {
            String address = wallet.getAddress();
            Vector<Object> row = new Vector<>();
            row.add(address); // Dirección completa de la wallet
            row.add(wallet.getAlias());
            row.add(String.format("%.2f", wallet.getBalance(blockchain)));
            walletsTableModel.addRow(row);

            fromWalletModel.addElement(wallet.getAlias());
            toWalletModel.addElement(wallet.getAlias());
        }
    }

    private void sendTransaction() {
        try {
            // Obtener los índices seleccionados
            int fromIndex = fromWalletCombo.getSelectedIndex();
            int toIndex = toWalletCombo.getSelectedIndex();

            if (fromIndex < 0 || toIndex < 0 || fromIndex >= wallets.size() || toIndex >= wallets.size()) {
                JOptionPane.showMessageDialog(this,
                        "Por favor selecciona carteras válidas",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Obtener las wallets por sus alias
            String fromAlias = fromWalletCombo.getSelectedItem().toString();
            String toAlias = toWalletCombo.getSelectedItem().toString();

            Wallet fromWallet = null;
            Wallet toWallet = null;

            // Encontrar las wallets correspondientes
            for (Wallet w : wallets) {
                if (w.getAlias().equals(fromAlias))
                    fromWallet = w;
                if (w.getAlias().equals(toAlias))
                    toWallet = w;
            }

            if (fromWallet == null || toWallet == null) {
                JOptionPane.showMessageDialog(this,
                        "Error al encontrar las wallets seleccionadas",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            float amount = Float.parseFloat(amountField.getText());

            if (fromWallet == toWallet) {
                JOptionPane.showMessageDialog(this,
                        "No puedes enviar a la misma wallet",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (amount <= 0) {
                JOptionPane.showMessageDialog(this,
                        "El monto debe ser mayor a 0",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Crear y añadir la transacción
            Transaction tx = fromWallet.createTransaction(toWallet.getAddress(), amount, blockchain);
            blockchain.createTransaction(tx);

            JOptionPane.showMessageDialog(this,
                    "Transacción enviada correctamente",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);

            amountField.setText("");

            // Actualizar inmediatamente la UI
            SwingUtilities.invokeLater(() -> {
                updateWalletsList();
                updateTransactionsList();
            });

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Por favor ingresa un monto válido",
                    "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al enviar: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTransactionsList() {
        transactionsTableModel.setRowCount(0);

        // Transacciones confirmadas (primero las mostramos porque son más importantes)
        for (Block block : blockchain.getChain()) {
            for (Transaction tx : block.getTransactions()) {
                addTransactionToTable(tx, "Confirmada");
            }
        }

        // Transacciones pendientes
        for (Transaction tx : blockchain.pendingTransactions) {
            addTransactionToTable(tx, "Pendiente");
        }
    }

    private String getAliasForAddress(String address) {
        if (address == null)
            return "SISTEMA";
        for (Wallet w : wallets) {
            if (w.getAddress().equals(address)) {
                return w.getAlias();
            }
        }
        return utils.GUIUtils.truncate(address, 11);
    }

    private void addTransactionToTable(Transaction tx, String status) {
        Vector<Object> row = new Vector<>();
        // Para transacciones de recompensa (minería)
        if (tx.fromAddress == null) {
            row.add("SISTEMA");
            row.add(getAliasForAddress(tx.toAddress));
            row.add(String.format("%.2f", tx.amount));
            row.add("Recompensa de minería");
        } else {
            // Para transacciones normales
            row.add(getAliasForAddress(tx.fromAddress));
            row.add(getAliasForAddress(tx.toAddress));
            row.add(String.format("%.2f", tx.amount));
            row.add(status);
        }
        transactionsTableModel.addRow(row);
    }
}
