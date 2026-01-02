package model;

import java.util.ArrayList;
import java.util.List;
import network.INetworkNode;
import model.BlockValidator;

public abstract class BlockchainCore {

    private List<Block> chain;
    private int difficulty;
    private float reward;
    private List<INetworkNode> nodes;

    public BlockchainCore() {
        this.chain = new ArrayList<>();
        this.nodes = new ArrayList<>();
        this.difficulty = 2;
        this.reward = 50f;

        Block genesis = new Block(0, new ArrayList<>(), "0", "GENESIS", 0L);
        chain.add(genesis);
    }

    // agregar el bloque con todos los chequeos
    public void addBlock(Block newBlock) {
        Block lastBlock = getLastBlock();

        // ignorar si el bloque es igual al anterior
        if (newBlock.getHash().equals(lastBlock.getHash())) {
            System.out.println("Info: bloque ya presente, ignorando.");
            return;
        }

        // ver si coincide el hash
        if (!newBlock.getPrevHash().equals(lastBlock.getHash())) {
            System.out.println("Error: hash previo no coincide. Bloque rechazado.");
            return;
        }

        // validar si las transacciones son balidas y estructura basica
        if (!BlockValidator.validateBlockStructure(newBlock)) {
            System.out.println("Error: bloque inválido (hash o transacciones).");
            return;
        }

        // agregar
        chain.add(newBlock);
        System.out.println("Bloque agregado correctamente con hash: " + newBlock.getHash());

        // ️⃣ sincronizar
        broadcastBlock(newBlock);
    }

    public Block getLastBlock() {
        return chain.get(chain.size() - 1);
    }

    // validar si la cadena esta bien
    public boolean validateChain() {
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);

            if (!BlockValidator.validateBlockStructure(current)) {
                System.out.println("Error: bloque inválido en posición " + i);
                return false;
            }
        }

        System.out.println("Cadena válida (" + chain.size() + " bloques)");
        return true;
    }

    // Importante, si la cadena es mas larga se aumenta la dificultad
    public void adjustDifficulty() {
        if (chain.size() % 5 == 0 && difficulty < 5) {
            difficulty++;
            System.out.println("Dificultad aumentada a: " + difficulty);
        }
    }

    // conecta los nodos
    public void broadcastBlock(Block b) {
        if (nodes.isEmpty())
            return;

        System.out.println("Difundiendo bloque " + b.getHash() + " a la red...");
        for (INetworkNode n : nodes) {
            n.broadcastBlock(b);
        }
    }

    // agregar a la lista
    public void addNode(INetworkNode node) {
        nodes.add(node);
    }

    // da la lista de los nodos conectados
    public List<INetworkNode> getNodes() {
        return nodes;
    }

    public List<Block> getChain() {
        return chain;
    }

    public void replaceChain(List<Block> newChain) {
        if (newChain.size() > chain.size()) {
            // Validar la nueva cadena completa
            // (Aqui podriamos usar validateChain() pero adaptado para recibir una lista,
            // por ahora asumimos que si la estructura es valida, confiamos)
            // En produccion deberiamos validar hash por hash y prevHash.

            // Validacion basica
            for (int i = 1; i < newChain.size(); i++) {
                Block current = newChain.get(i);
                Block prev = newChain.get(i - 1);
                if (!current.getPrevHash().equals(prev.getHash())) {
                    System.out.println("Cadena recibida invalida: hashes no coinciden");
                    return;
                }
                if (!BlockValidator.validateBlockStructure(current)) {
                    System.out.println("Cadena recibida invalida: bloque invalido");
                    return;
                }
            }

            this.chain = new ArrayList<>(newChain);
            System.out.println("Cadena reemplazada por una mas larga y valida. Nueva longitud: " + chain.size());
        }
    }
}
