import java.util.ArrayList;
import java.util.List;

public class Blockchain {
    public List<Transaction> pendingTransactions;
    public ContractRegistry contractRegistry;
    public TransactionPool txPool;
    public List<Block> chain;

    private int difficulty = 3; // cuantos ceros iniciales en el hash para el PoW
    private float miningReward = 50.0f;

    public Blockchain() {
        this.pendingTransactions = new ArrayList<>();
        this.contractRegistry = new ContractRegistry();
        this.txPool = new TransactionPool();
        this.chain = new ArrayList<>();
        // Genesis block
        Block genesis = new Block(0, new ArrayList<>(), "0");
        genesis.setHash(genesis.calculateHash());
        chain.add(genesis);
    }

    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    // Agrega transacción al pool y a pendingTransactions
    public void createTransaction(Transaction tx) {
        if (!tx.isValid()) {
            throw new RuntimeException("Transacción inválida, no se puede añadir.");
        }
        this.pendingTransactions.add(tx);
        this.txPool.addTransaction(tx);
    }

    // Minado de las transacciones pendientes. Recompensa al miner.
    public void minePendingTransactions(Miner miner) {
        if (pendingTransactions.isEmpty()) {
            System.out.println("No hay transacciones pendientes para minar.");
            return;
        }
        List<Transaction> transactionsToMine = new ArrayList<>(pendingTransactions);

        Block newBlock = new Block(chain.size(), transactionsToMine, getLatestBlock().getHash());
        // Proof of Work
        String target = new String(new char[difficulty]).replace('\0', '0');
        while (!newBlock.getHash().substring(0, difficulty).equals(target)) {
            newBlock.incrementNonce();
            newBlock.setHash(newBlock.calculateHash());
        }

        // Validar transacciones del bloque
        if (!newBlock.hasValidTransactions()) {
            throw new RuntimeException("El bloque contiene transacciones inválidas.");
        }

        chain.add(newBlock);
        // Limpiar pool: quitar transacciones minadas
        this.txPool.removeTransactions(transactionsToMine);
        this.pendingTransactions.removeAll(transactionsToMine);

        // Crear transacción de recompensa para el minero (se añade a pendientes para el siguiente bloque)
        Transaction rewardTx = new Transaction(null, miner.getAddress(), miningReward);
        this.pendingTransactions.add(rewardTx);
        this.txPool.addTransaction(rewardTx);

        System.out.println("Bloque minado y añadido a la cadena: " + newBlock.getHash());
    }

    public float getBalance(String address) {
        float balance = 0.0f;
        // Revisar todos los bloques
        for (Block block : chain) {
            for (Transaction tx : block.getTransactions()) {
                if (address.equals(tx.fromAddress)) {
                    balance -= tx.amount;
                }
                if (address.equals(tx.toAddress)) {
                    balance += tx.amount;
                }
            }
        }
        // Revisar transacciones pendientes también
        for (Transaction tx : pendingTransactions) {
            if (address.equals(tx.fromAddress)) {
                balance -= tx.amount;
            }
            if (address.equals(tx.toAddress)) {
                balance += tx.amount;
            }
        }
        return balance;
    }

    public String deployContract(SmartContract sc) {
        return contractRegistry.deploy(sc);
    }

    public boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            if (!current.getHash().equals(current.calculateHash())) return false;
            if (!current.getPrevHash().equals(previous.getHash())) return false;
            if (!current.hasValidTransactions()) return false;
        }
        return true;
    }
}