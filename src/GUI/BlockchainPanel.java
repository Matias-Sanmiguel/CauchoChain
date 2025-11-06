package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import model.Block;
import model.Blockchain;
import model.Transaction;

public class BlockchainPanel extends JPanel {
    private final Blockchain blockchain;
    private final JPanel chainView;
    private final SimpleDateFormat dateFormat;
    private Block selectedBlock;

    public BlockchainPanel(Blockchain blockchain) {
        this.blockchain = blockchain;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        setLayout(new BorderLayout());

        // Panel para la vista gráfica de la cadena
        chainView = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBlockchain(g);
            }
        };
        chainView.setBackground(Color.WHITE);
        chainView.addMouseListener(new BlockClickListener());

        // Scroll para la vista de la cadena
        JScrollPane scrollPane = new JScrollPane(chainView);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        // Botón de actualización
        JButton refreshButton = new JButton("Actualizar Vista");
        refreshButton.addActionListener(e -> refresh());
        add(refreshButton, BorderLayout.SOUTH);

        // Timer para actualización automática
        Timer timer = new Timer(5000, e -> refresh());
        timer.start();

        refresh();
    }

    private void refresh() {
        int totalBlocks = blockchain.getChain().size();
        int blocksPerRow = 6;
        int rows = (int) Math.ceil((double) totalBlocks / blocksPerRow);

        chainView.setPreferredSize(new Dimension(
            blocksPerRow * 200 + 100,
            Math.max(300, rows * 150 + 100)
        ));
        chainView.repaint();
    }

    private void drawBlockchain(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int blockWidth = 200;
        int blockHeight = 150;
        int blocksPerRow = 6;
        int x = 50;
        int y = 50;

        for (int i = 0; i < blockchain.getChain().size(); i++) {
            Block block = blockchain.getChain().get(i);

            int row = i / blocksPerRow;
            int col = i % blocksPerRow;
            int drawX = 50 + col * blockWidth;
            int drawY = 50 + row * blockHeight;

            if (block == selectedBlock) {
                g2d.setColor(new Color(200, 230, 255));
            } else {
                g2d.setColor(Color.WHITE);
            }
            g2d.fillRect(drawX, drawY, 150, 100);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(drawX, drawY, 150, 100);

            g2d.drawString("Bloque #" + block.getIndex(), drawX + 10, drawY + 20);
            String hash = block.getHash();
            g2d.drawString("Hash: " + hash.substring(0, 8) + "...", drawX + 10, drawY + 40);
            g2d.drawString("Tx: " + block.getTransactions().size(), drawX + 10, drawY + 60);

            if (i < blockchain.getChain().size() - 1) {
                int nextCol = (i + 1) % blocksPerRow;
                int nextRow = (i + 1) / blocksPerRow;

                if (nextCol == 0) {
                    g2d.drawLine(drawX + 150, drawY + 50, drawX + 150, drawY + 100 + 20);
                    g2d.drawLine(drawX + 150, drawY + 100 + 20, 50, drawY + 100 + 20);
                    g2d.drawLine(50, drawY + 100 + 20, 50, drawY + 150);
                } else {
                    g2d.drawLine(drawX + 150, drawY + 50, drawX + 150 + 30, drawY + 50);
                    g2d.drawLine(drawX + 150 + 30, drawY + 50, drawX + 150 + 20, drawY + 40);
                    g2d.drawLine(drawX + 150 + 30, drawY + 50, drawX + 150 + 20, drawY + 60);
                }
            }
        }
    }

    private class BlockClickListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            int clickX = e.getX();
            int clickY = e.getY();
            int blocksPerRow = 6;
            int blockWidth = 200;
            int blockHeight = 150;

            for (int i = 0; i < blockchain.getChain().size(); i++) {
                int row = i / blocksPerRow;
                int col = i % blocksPerRow;
                int drawX = 50 + col * blockWidth;
                int drawY = 50 + row * blockHeight;

                if (clickX >= drawX && clickX <= drawX + 150 &&
                    clickY >= drawY && clickY <= drawY + 100) {
                    Block block = blockchain.getChain().get(i);
                    showBlockDetails(block);
                    selectedBlock = block;
                    chainView.repaint();
                    return;
                }
            }
        }
    }

    private void showBlockDetails(Block block) {
        JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this),
                                   "Detalles del Bloque #" + block.getIndex(),
                                   false);
        dialog.setLayout(new BorderLayout());

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));

        // Información general del bloque
        detailsPanel.add(new JLabel("Hash: " + block.getHash()));
        detailsPanel.add(new JLabel("Hash Previo: " + block.getPrevHash()));
        detailsPanel.add(new JLabel("Nonce: " + block.getNonce()));
        detailsPanel.add(new JLabel("Timestamp: " +
            dateFormat.format(new Date(String.valueOf(block.getTimestamp())))));

        // Lista de transacciones
        JPanel txPanel = new JPanel(new BorderLayout());
        txPanel.setBorder(BorderFactory.createTitledBorder("Transacciones"));

        DefaultListModel<String> txListModel = new DefaultListModel<>();
        for (Transaction tx : block.getTransactions()) {
            txListModel.addElement(String.format("De: %s -> Para: %s | Monto: %.2f",
                tx.fromAddress, tx.toAddress, tx.amount));
        }
        JList<String> txList = new JList<>(txListModel);
        txPanel.add(new JScrollPane(txList), BorderLayout.CENTER);

        dialog.add(detailsPanel, BorderLayout.NORTH);
        dialog.add(txPanel, BorderLayout.CENTER);

        JButton closeButton = new JButton("Cerrar");
        closeButton.addActionListener(e -> dialog.dispose());
        dialog.add(closeButton, BorderLayout.SOUTH);

        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
