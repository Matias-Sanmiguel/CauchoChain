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

    // Minado de las transacciones pendientes. Recompensa al miner
    public void minePendingTransactions(Miner miner) {
        miner.mine(this);
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

        // NO contar transacciones pendientes - solo las confirmadas en bloques
        return balance;
    }

    public String deployContract(SmartContract sc) {
        logger.info("Desplegando contrato inteligente: " + sc.getId());
        return contractRegistry.deploy(sc);
    }

    public boolean isChainValid() {
        return validateChain();
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

    public void setMiningReward(float newReward) {
        if (newReward > 0) {
            this.miningReward = newReward;
        }
    }

    public Logger getLogger() {
        return logger;
    }
}