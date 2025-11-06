package GUI;

import model.Blockchain;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;

public class ConfigurationPanel extends JPanel {
    private final Blockchain blockchain;
    private final JSlider difficultySlider;
    private final JLabel difficultyLabel;
    private final DefaultListModel<String> nodesListModel;

    public ConfigurationPanel(Blockchain blockchain) {
        this.blockchain = blockchain;
        setLayout(new BorderLayout());

        //dificultad
        JPanel difficultyPanel = new JPanel();
        difficultyPanel.setBorder(BorderFactory.createTitledBorder("Configuración de Minería"));
        difficultyPanel.setLayout(new BoxLayout(difficultyPanel, BoxLayout.Y_AXIS));

        difficultyLabel = new JLabel("Dificultad actual: " + blockchain.getDifficulty());
        difficultySlider = new JSlider(1, 6, blockchain.getDifficulty());
        difficultySlider.setMajorTickSpacing(1);
        difficultySlider.setPaintTicks(true);
        difficultySlider.setPaintLabels(true);
        difficultySlider.addChangeListener(this::onDifficultyChange);

        difficultyPanel.add(difficultyLabel);
        difficultyPanel.add(difficultySlider);

        // nodos
        JPanel nodesPanel = new JPanel(new BorderLayout());
        nodesPanel.setBorder(BorderFactory.createTitledBorder("Nodos Conectados"));

        nodesListModel = new DefaultListModel<>();
        JList<String> nodesList = new JList<>(nodesListModel);
        nodesPanel.add(new JScrollPane(nodesList), BorderLayout.CENTER);

        // control de red
        JPanel networkControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addNodeButton = new JButton("Agregar Nodo");
        JButton removeNodeButton = new JButton("Eliminar Nodo");

        addNodeButton.addActionListener(e -> addSimulatedNode());
        removeNodeButton.addActionListener(e -> removeSimulatedNode());

        networkControlPanel.add(addNodeButton);
        networkControlPanel.add(removeNodeButton);

        nodesPanel.add(networkControlPanel, BorderLayout.SOUTH);

        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        mainPanel.add(difficultyPanel);
        mainPanel.add(nodesPanel);

        add(mainPanel, BorderLayout.CENTER);

        // nodos simulados
        addSimulatedNode();
        addSimulatedNode();
    }

    private void onDifficultyChange(ChangeEvent e) {
        if (!difficultySlider.getValueIsAdjusting()) {
            int newDifficulty = difficultySlider.getValue();
            blockchain.setDifficulty(newDifficulty);
            difficultyLabel.setText("Dificultad actual: " + newDifficulty);
        }
    }

    private void addSimulatedNode() {
        String nodeId = "Nodo-" + (nodesListModel.size() + 1);
        nodesListModel.addElement(nodeId + " (Sincronizado)");
    }

    private void removeSimulatedNode() {
        if (!nodesListModel.isEmpty()) {
            nodesListModel.remove(nodesListModel.size() - 1);
        }
    }
}
