package GUI;

import model.Blockchain;
import model.Transaction;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TransactionPanel extends JPanel {
    private final Blockchain blockchain;
    private final JTable pendingTable;
    private final JTable confirmedTable;
    private final JLabel statsLabel;

    public TransactionPanel(Blockchain blockchain) {
        this.blockchain = blockchain;
        setLayout(new BorderLayout());

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsLabel = new JLabel();
        updateStats();
        statsPanel.add(statsLabel);
        add(statsPanel, BorderLayout.NORTH);

        JPanel tablesPanel = new JPanel(new GridLayout(2, 1));

        String[] columnNames = { "De", "Para", "Monto", "Estado" };
        pendingTable = new JTable(new Object[0][4], columnNames);
        JScrollPane pendingScroll = new JScrollPane(pendingTable);
        JPanel pendingPanel = new JPanel(new BorderLayout());
        pendingPanel.add(new JLabel("Transacciones Pendientes"), BorderLayout.NORTH);
        pendingPanel.add(pendingScroll, BorderLayout.CENTER);

        confirmedTable = new JTable(new Object[0][4], columnNames);
        JScrollPane confirmedScroll = new JScrollPane(confirmedTable);
        JPanel confirmedPanel = new JPanel(new BorderLayout());
        confirmedPanel.add(new JLabel("Transacciones Confirmadas"), BorderLayout.NORTH);
        confirmedPanel.add(confirmedScroll, BorderLayout.CENTER);

        tablesPanel.add(pendingPanel);
        tablesPanel.add(confirmedPanel);

        add(tablesPanel, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Actualizar");
        refreshButton.addActionListener(e -> updateTables());
        add(refreshButton, BorderLayout.SOUTH);

        Timer timer = new Timer(5000, e -> updateTables());
        timer.start();
    }

    private void updateStats() {
        int totalTx = blockchain.txPool.getValidTransactions().size();
        float totalAmount = calculateTotalAmount();
        float avgPerBlock = calculateAvgTransactionsPerBlock();

        statsLabel.setText(String.format(
                "<html>Total Transacciones: %d | " +
                        "Promedio por Bloque: %.2f | " +
                        "Monto Total: %.2f</html>",
                totalTx, avgPerBlock, totalAmount));
    }

    private float calculateTotalAmount() {
        float total = 0;
        List<Transaction> transactions = blockchain.txPool.getValidTransactions();
        for (Transaction tx : transactions) {
            total += tx.amount;
        }
        return total;
    }

    private float calculateAvgTransactionsPerBlock() {
        if (blockchain.getChain().isEmpty())
            return 0;
        int totalTx = 0;
        for (var block : blockchain.getChain()) {
            totalTx += block.getTransactions().size();
        }
        return (float) totalTx / blockchain.getChain().size();
    }

    private void updateTables() {
        List<Transaction> pending = blockchain.pendingTransactions;
        Object[][] pendingData = new Object[pending.size()][4];
        for (int i = 0; i < pending.size(); i++) {
            Transaction tx = pending.get(i);
            pendingData[i] = new Object[] {
                    tx.fromAddress != null ? truncate(tx.fromAddress, 15) : "SISTEMA",
                    truncate(tx.toAddress, 15),
                    String.format("%.2f", tx.amount),
                    "Pendiente"
            };
        }
        pendingTable.setModel(new javax.swing.table.DefaultTableModel(
                pendingData,
                new String[] { "De", "Para", "Monto", "Estado" }));

        List<Transaction> confirmed = getConfirmedTransactions();
        Object[][] confirmedData = new Object[confirmed.size()][4];
        for (int i = 0; i < confirmed.size(); i++) {
            Transaction tx = confirmed.get(i);
            confirmedData[i] = new Object[] {
                    tx.fromAddress != null ? truncate(tx.fromAddress, 15) : "SISTEMA",
                    truncate(tx.toAddress, 15),
                    String.format("%.2f", tx.amount),
                    "Confirmada"
            };
        }
        confirmedTable.setModel(new javax.swing.table.DefaultTableModel(
                confirmedData,
                new String[] { "De", "Para", "Monto", "Estado" }));

        updateStats();
    }

    private List<Transaction> getConfirmedTransactions() {
        java.util.ArrayList<Transaction> confirmed = new java.util.ArrayList<>();
        for (var block : blockchain.getChain()) {
            confirmed.addAll(block.getTransactions());
        }
        return confirmed;
    }

    private String truncate(String s, int len) {
        return utils.GUIUtils.truncate(s, len);
    }
}
