package model;

import java.util.ArrayList;
import java.util.List;
import miner.Miner;

public class Blockchain extends BlockchainCore {
    public List<Transaction> pendingTransactions;
    public ContractRegistry contractRegistry;
    public TransactionPool txPool;

    private int difficulty = 3; // cuantos ceros iniciales en el hash para el PoW
    private float miningReward = 50.0f;

    public Blockchain() {
        // BlockchainCore() ya inicializa `chain` y crea el genesis
        super();
        this.pendingTransactions = new ArrayList<>();
        this.contractRegistry = new ContractRegistry();
        this.txPool = new TransactionPool();
        // no volver a crear genesis ni reasignar `chain`
    }

    public Block getLatestBlock() {
        return getLastBlock();
    }

    // Agrega transacción SOLO al pool (sin confirmar aún)
    public void addTransactionToPool(Transaction tx) {
        if (tx == null || !tx.isValid()) {
            throw new RuntimeException("Transacción inválida, no se puede añadir.");
        }
        this.txPool.addTransaction(tx);
    }

    // Agrega transacción al pool y a pendingTransactions (para uso interno/minería)
    public void createTransaction(Transaction tx) {
        if (tx == null || !tx.isValid()) {
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

        Block latest = getLatestBlock();
        String prevHash = (latest == null) ? "0" : latest.getHash();

        Block newBlock = new Block(getChain().size(), transactionsToMine, prevHash, miner instanceof Miner ? ((Miner) miner).getAddress() : "UNKNOWN");

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

        // Agregar bloque minado a la cadena
        getChain().add(newBlock);

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

        // Solo recorrer transacciones confirmadas (en bloques minados)
        for (Block block : getChain()) {
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
        for (int i = 1; i < getChain().size(); i++) {
            Block current = getChain().get(i);
            Block previous = getChain().get(i - 1);

            if (!current.getHash().equals(current.calculateHash())) return false;
            if (!current.getPrevHash().equals(previous.getHash())) return false;
            if (!current.hasValidTransactions()) return false;
        }
        return true;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int newDifficulty) {
        if (newDifficulty > 0) {
            this.difficulty = newDifficulty;
        }
    }

    public float getMiningReward() {
        return miningReward;
    }
}