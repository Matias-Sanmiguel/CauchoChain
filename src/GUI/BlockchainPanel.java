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
        chainView.setPreferredSize(new Dimension(
            blockchain.getChain().size() * 200,
            300
        ));
        chainView.repaint();
    }

    private void drawBlockchain(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = 50;
        int y = 100;
        int width = 150;
        int height = 100;

        for (Block block : blockchain.getChain()) {
            // Dibujar el bloque
            if (block == selectedBlock) {
                g2d.setColor(new Color(200, 230, 255));
            } else {
                g2d.setColor(Color.WHITE);
            }
            g2d.fillRect(x, y, width, height);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, y, width, height);

            // Dibujar flecha al siguiente bloque
            if (blockchain.getChain().indexOf(block) < blockchain.getChain().size() - 1) {
                g2d.drawLine(x + width, y + height/2, x + width + 50, y + height/2);
                g2d.drawLine(x + width + 50, y + height/2, x + width + 40, y + height/2 - 5);
                g2d.drawLine(x + width + 50, y + height/2, x + width + 40, y + height/2 + 5);
            }

            // Información del bloque
            g2d.drawString("Bloque #" + block.getIndex(), x + 10, y + 20);
            String hash = block.getHash();
            g2d.drawString("Hash: " + hash.substring(0, 8) + "...", x + 10, y + 40);
            g2d.drawString("Tx: " + block.getTransactions().size(), x + 10, y + 60);

            x += 200;
        }
    }

    private class BlockClickListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            // Determinar qué bloque fue clickeado
            int blockX = 50;
            for (Block block : blockchain.getChain()) {
                if (x >= blockX && x <= blockX + 150 &&
                    y >= 100 && y <= 200) {
                    showBlockDetails(block);
                    selectedBlock = block;
                    chainView.repaint();
                    return;
                }
                blockX += 200;
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
