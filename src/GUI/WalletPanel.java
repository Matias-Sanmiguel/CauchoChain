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
    private final JComboBox<String> fromWalletCombo;
    private final JComboBox<String> toWalletCombo;
    private final JTextField amountField;

    public WalletPanel(Blockchain blockchain) {
        super(new BorderLayout());
        this.blockchain = blockchain;
        this.wallets = new ArrayList<>();

        String[] walletColumns = {"Dirección", "Alias", "Saldo"};
        String[] txColumns = {"De", "Para", "Monto", "Estado"};
        this.walletsTableModel = new DefaultTableModel(walletColumns, 0);
        this.transactionsTableModel = new DefaultTableModel(txColumns, 0);


        this.fromWalletCombo = new JComboBox<>();
        this.toWalletCombo = new JComboBox<>();
        this.amountField = new JTextField();

        // lista de wallets
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Wallets"));

        // tabla de wallets
        JTable walletsTable = new JTable(walletsTableModel);
        leftPanel.add(new JScrollPane(walletsTable), BorderLayout.CENTER);

        // crear wallet
        JButton createWalletButton = new JButton("Crear Nueva Wallet");
        createWalletButton.addActionListener(e -> createNewWallet());
        leftPanel.add(createWalletButton, BorderLayout.SOUTH);

        // transacciones
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Transacciones"));

        // envío
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

        // historial de transacciones
        JTable txTable = new JTable(transactionsTableModel);

        rightPanel.add(sendPanel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(txTable), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            leftPanel,
            rightPanel
        );
        splitPane.setDividerLocation(400);
        add(splitPane, BorderLayout.CENTER);

        //creo wallets iniciales y mino un bloque
        SwingUtilities.invokeLater(() -> {
            // creo wallet con saldo inicial
            Wallet firstWallet = new Wallet("Wallet-1");
            wallets.add(firstWallet);

            // creo transacción inicial y la mino
            Transaction initialTx = new Transaction(null, firstWallet.getAddress(), 50.0f);
            blockchain.createTransaction(initialTx);

            // mino el bloque inicial
            Miner initialMiner = new Miner(2.0f, "InitialMiner");
            blockchain.minePendingTransactions(initialMiner);

            // creo segunda wallet
            Wallet secondWallet = new Wallet("Wallet-2");
            wallets.add(secondWallet);

            updateWalletsList();
            updateTransactionsList();
        });

        Timer timer = new Timer(5000, e -> SwingUtilities.invokeLater(() -> {
            updateWalletsList();
            updateTransactionsList();
        }));
        timer.start();
    }

    private void createNewWallet() {
        String alias = "Wallet-" + (wallets.size() + 1);
        Wallet wallet = new Wallet(alias);
        wallets.add(wallet);
        updateWalletsList();
    }

    private void updateWalletsList() {
        walletsTableModel.setRowCount(0);
        fromWalletCombo.removeAllItems();
        toWalletCombo.removeAllItems();

        for (Wallet wallet : wallets) {
            Vector<Object> row = new Vector<>();
            row.add(wallet.getAddress());
            row.add(wallet.getAlias());
            row.add(wallet.getBalance(blockchain));
            walletsTableModel.addRow(row);

            fromWalletCombo.addItem(wallet.getAddress());
            toWalletCombo.addItem(wallet.getAddress());
        }
    }

    private void sendTransaction() {
        try {
            int fromIndex = fromWalletCombo.getSelectedIndex();
            int toIndex = toWalletCombo.getSelectedIndex();

            if (fromIndex < 0 || toIndex < 0 || fromIndex >= wallets.size() || toIndex >= wallets.size()) {
                JOptionPane.showMessageDialog(this,
                    "Por favor selecciona carteras válidas",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Wallet fromWallet = wallets.get(fromIndex);
            Wallet toWallet = wallets.get(toIndex);
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

            //creo y añado la transacción
            Transaction tx = fromWallet.createTransaction(toWallet.getAddress(), amount, blockchain);
            blockchain.createTransaction(tx); // Aseguramos que se añada al pool de transacciones

            JOptionPane.showMessageDialog(this,
                "Transacción enviada correctamente",
                "Éxito", JOptionPane.INFORMATION_MESSAGE);

            amountField.setText("");

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

        //transacciones confirmadas
        for (Block block : blockchain.getChain()) {
            for (Transaction tx : block.getTransactions()) {
                addTransactionToTable(tx, "Confirmada");
            }
        }

        // transacciones pendientes
        for (Transaction tx : blockchain.pendingTransactions) {
            addTransactionToTable(tx, "Pendiente");
        }
    }

    private void addTransactionToTable(Transaction tx, String status) {
        Vector<Object> row = new Vector<>();
        // para transacciones de mineria (recompensa)
        if (tx.fromAddress == null) {
            row.add("SISTEMA");
            row.add(tx.toAddress); // Dirección del minero
            row.add(tx.amount);
            row.add("Recompensa de minería");
        } else {
            // para transacciones normales
            row.add(tx.fromAddress);
            row.add(tx.toAddress);
            row.add(tx.amount);
            row.add(status);
        }
        transactionsTableModel.addRow(row);
    }

    private String getAliasForAddress(String address) {
        if (address == null) return "Sistema";
        //muestro el address de la wallet
        return address;
    }

}
