package model;
import java.util.ArrayList;
import java.util.List;
import network.INetworkNode;

public abstract class BlockchainCore {

    // ATRIBUTOS BÁSICOS
    private List<Block> chain;
    private int difficulty;
    private float reward;
    private List<INetworkNode> nodes;

    // CONSTRUCTOR

    public BlockchainCore() {
        this.chain = new ArrayList<>();
        this.nodes = new ArrayList<>();
        this.difficulty = 2; // nivel inicial de dificultad
        this.reward = 50f;   // recompensa por bloque minado


        Block genesis = new Block(0, new ArrayList<>(), "0", "GENESIS", 0L);
        chain.add(genesis);
    }


    // agregar el bloque con todos los chequeos
    public void addBlock(Block newBlock) {
        Block lastBlock = getLastBlock();

        // ignorar si el bloque es igal al anterior
        if (newBlock.getHash().equals(lastBlock.getHash())) {
            System.out.println("Info: bloque ya presente, ignorando.");
            return;
        }

        // ver si coincide el hash
        if (!newBlock.getPrevHash().equals(lastBlock.getHash())) {
            System.out.println("Error: hash previo no coincide. Bloque rechazado.");
            return;
        }

        // validar si las transacciones son balidas
        if (!newBlock.hasValidTransactions()) {
            System.out.println("Error: transacciones inválidas en el bloque.");
            return;
        }

        // versi es valido el bloque
        String recalculated = newBlock.calculateHash();
        if (!recalculated.equals(newBlock.getHash())) {
            System.out.println("Error: hash inválido para el bloque.");
            return;
        }

        // agregar
        chain.add(newBlock);
        System.out.println("Bloque agregado correctamente con hash: " + newBlock.getHash());

        //️⃣ sincronizar
        broadcastBlock(newBlock);
    }


    public Block getLastBlock() {
        return chain.get(chain.size() - 1);
    }

 // validar si la cadena esta bien
    public boolean validateChain() {
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            if (!current.getHash().equals(current.calculateHash())) {
                System.out.println("Error: hash incorrecto en bloque " + i);
                return false;
            }

            if (!current.getPrevHash().equals(previous.getHash())) {
                System.out.println("Error: hash previo incorrecto en bloque " + i);
                return false;
            }

            if (!current.hasValidTransactions()) {
                System.out.println("Error: transacciones inválidas en bloque " + i);
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
        if (nodes.isEmpty()) return;

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
}
