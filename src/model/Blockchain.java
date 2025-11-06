package model;

import java.util.ArrayList;
import java.util.List;
import miner.Miner;
import utils.Logger;

public class Blockchain extends BlockchainCore {
    public List<Transaction> pendingTransactions;
    public ContractRegistry contractRegistry;
    public TransactionPool txPool;
    private Logger logger;

    private int difficulty = 3; // cuantos ceros iniciales en el hash para el PoW
    private float miningReward = 50.0f;

    public Blockchain() {
        // BlockchainCore() ya inicializa `chain` y crea el genesis
        super();
        this.pendingTransactions = new ArrayList<>();
        this.contractRegistry = new ContractRegistry();
        this.txPool = new TransactionPool();
        this.logger = Logger.getInstance();
        logger.info("Blockchain inicializada");
        // no volver a crear genesis ni reasignar `chain`
    }

    public Block getLatestBlock() {
        return getLastBlock();
    }

    // Agrega transacción SOLO al pool (sin confirmar aún)
    public void addTransactionToPool(Transaction tx) {
        if (tx == null || !tx.isValid()) {
            logger.error("Transacción inválida: " + (tx != null ? tx.toString() : "null"));
            throw new RuntimeException("Transacción inválida, no se puede añadir.");
        }
        this.txPool.addTransaction(tx);
        logger.debug("Transacción agregada al pool: " + tx.toAddress + " -> " + tx.amount);
    }

    // Agrega transacción al pool y a pendingTransactions (para uso interno/minería)
    public void createTransaction(Transaction tx) {
        if (tx == null || !tx.isValid()) {
            logger.error("Transacción inválida para crear: " + (tx != null ? tx.toString() : "null"));
            throw new RuntimeException("Transacción inválida, no se puede añadir.");
        }
        this.pendingTransactions.add(tx);
        this.txPool.addTransaction(tx);
        logger.info("Transacción creada: " + tx.fromAddress + " -> " + tx.toAddress + " (" + tx.amount + ")");
    }

    // Minado de las transacciones pendientes. Recompensa al miner.
    public void minePendingTransactions(Miner miner) {
        if (pendingTransactions.isEmpty()) {
            logger.warning("No hay transacciones pendientes para minar.");
            return;
        }

        logger.info("Iniciando minado de bloque con " + pendingTransactions.size() + " transacciones");
        List<Transaction> transactionsToMine = new ArrayList<>(pendingTransactions);

        Block latest = getLatestBlock();
        String prevHash = (latest == null) ? "0" : latest.getHash();

        Block newBlock = new Block(getChain().size(), transactionsToMine, prevHash, miner instanceof Miner ? ((Miner) miner).getAddress() : "UNKNOWN");

        // Proof of Work
        String target = new String(new char[difficulty]).replace('\0', '0');
        long startTime = System.currentTimeMillis();

        while (!newBlock.getHash().substring(0, difficulty).equals(target)) {
            newBlock.incrementNonce();
            newBlock.setHash(newBlock.calculateHash());
        }

        long miningTime = System.currentTimeMillis() - startTime;

        // Validar transacciones del bloque
        if (!newBlock.hasValidTransactions()) {
            logger.error("El bloque contiene transacciones inválidas");
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

        logger.success("Bloque #" + newBlock.getIndex() + " minado en " + miningTime + "ms | Hash: " + newBlock.getHash() + " | Nonce: " + newBlock.getNonce());
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
        logger.info("Desplegando contrato inteligente: " + sc.getId());
        return contractRegistry.deploy(sc);
    }

    public boolean isChainValid() {
        logger.debug("Validando cadena de bloques");
        for (int i = 1; i < getChain().size(); i++) {
            Block current = getChain().get(i);
            Block previous = getChain().get(i - 1);

            if (!current.getHash().equals(current.calculateHash())) {
                logger.error("Hash inválido en bloque #" + i);
                return false;
            }
            if (!current.getPrevHash().equals(previous.getHash())) {
                logger.error("Link roto entre bloque #" + (i-1) + " y #" + i);
                return false;
            }
            if (!current.hasValidTransactions()) {
                logger.error("Transacciones inválidas en bloque #" + i);
                return false;
            }
        }
        logger.success("Cadena de bloques validada correctamente");
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

    public Logger getLogger() {
        return logger;
    }
}