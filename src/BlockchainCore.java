import java.util.ArrayList;
import java.util.List;

public abstract class BlockchainCore {

    // ATRIBUTOS BÁSICOS
    protected List<Block> chain;
    protected int difficulty;
    protected float reward;
    protected List<INetworkNode> nodes;

    // CONSTRUCTOR

    public BlockchainCore() {
        this.chain = new ArrayList<>();
        this.nodes = new ArrayList<>();
        this.difficulty = 2; // nivel inicial de dificultad
        this.reward = 50f;   // recompensa por bloque minado

        // Crear bloque génesis
        Block genesis = new Block(0, new ArrayList<>(), "0", "GENESIS");
        chain.add(genesis);
    }

    // MÉTODOS PÚBLICOS

    /** Agrega un bloque a la cadena si es válido */
    public void addBlock(Block newBlock) {
        Block lastBlock = getLastBlock();

        // 1️⃣ Verificar hash previo
        if (!newBlock.getPrevHash().equals(lastBlock.getHash())) {
            System.out.println("Error: hash previo no coincide. Bloque rechazado.");
            return;
        }

        // 2️⃣ Validar transacciones
        if (!newBlock.hasValidTransactions()) {
            System.out.println("Error: transacciones inválidas en el bloque.");
            return;
        }

        // 3️⃣ Validar hash del bloque
        String recalculated = newBlock.calculateHash();
        if (!recalculated.equals(newBlock.getHash())) {
            System.out.println("Error: hash inválido para el bloque.");
            return;
        }

        // 4️⃣ Agregarlo
        chain.add(newBlock);
        System.out.println("Bloque agregado correctamente con hash: " + newBlock.getHash());

        // 5️⃣ Difundir a otros nodos
        broadcastBlock(newBlock);
    }

    /** Devuelve el último bloque de la cadena */
    public Block getLastBlock() {
        return chain.get(chain.size() - 1);
    }

    /** Valida toda la cadena (hashes y transacciones) */
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

    /** Ajusta la dificultad según el tamaño de la cadena (simplificado) */
    public void adjustDifficulty() {
        if (chain.size() % 5 == 0 && difficulty < 5) {
            difficulty++;
            System.out.println("Dificultad aumentada a: " + difficulty);
        }
    }

    /** Difunde un bloque nuevo a los nodos conectados */
    public void broadcastBlock(Block b) {
        if (nodes.isEmpty()) return;

        System.out.println("Difundiendo bloque " + b.getHash() + " a la red...");
        for (INetworkNode n : nodes) {
            n.broadcastBlock(b);
        }
    }

    /** Agrega un nodo a la lista local */
    public void addNode(INetworkNode node) {
        nodes.add(node);
    }

    /** Devuelve todos los nodos conectados */
    public List<INetworkNode> getNodes() {
        return nodes;
    }
}
