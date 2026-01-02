package miner;

import java.util.ArrayList;
import java.util.List;
import model.Block;
import model.Blockchain;
import model.Transaction;
import model.User;
import wallet.Wallet;
import utils.Logger;
import model.BlockValidator;

public class Miner implements IMiner {
    private float hashMined;
    private float hashRate;
    private Wallet wallet;
    private Logger logger;

    public Miner(float hashRate) {
        this.hashMined = 0.0f;
        this.hashRate = hashRate;
        this.wallet = new Wallet("Miner_" + System.currentTimeMillis());
        this.logger = Logger.getInstance();
    }

    public Miner(float hashRate, String minerAlias) {
        this.hashRate = hashRate;
        this.hashMined = 0.0f;
        this.wallet = new Wallet(minerAlias);
        this.logger = Logger.getInstance();
    }

    public Miner(float hashRate, User minerUser) {
        this.hashRate = hashRate;
        this.hashMined = 0.0f;
        this.wallet = new Wallet("Miner_" + minerUser.getName(), minerUser);
        this.logger = Logger.getInstance();
    }

    public void mine(Blockchain bc) {
        if (bc.pendingTransactions.isEmpty()) {
            logger.warning("No hay transacciones pendientes para minar.");
            return;
        }

        logger.info("Minero " + wallet.getAlias() + " iniciando minado");

        List<Transaction> transactionsToMine = new ArrayList<>(bc.pendingTransactions);

        Block latest = bc.getLatestBlock();
        String prevHash = (latest == null) ? "0" : latest.getHash();

        Block newBlock = new Block(bc.getChain().size(), transactionsToMine, prevHash, this.getAddress());

        int difficulty = bc.getDifficulty();
        String target = new String(new char[difficulty]).replace('\0', '0');

        long startTime = System.currentTimeMillis();

        while (!newBlock.getHash().substring(0, Math.min(difficulty, newBlock.getHash().length())).equals(target)) {
            newBlock.setNonce(newBlock.getNonce() + 1);
            newBlock.setHash(newBlock.calculateHash());
        }

        long timeTaken = System.currentTimeMillis() - startTime;

        if (!validateBlock(newBlock)) {
            logger.error("El bloque minado no es válido.");
            throw new RuntimeException("El bloque minado no es válido.");
        }

        // Usar addBlock() para que se haga el broadcast automáticamente
        bc.addBlock(newBlock);
        bc.txPool.removeTransactions(transactionsToMine);
        bc.pendingTransactions.removeAll(transactionsToMine);

        float miningReward = bc.getMiningReward();
        Transaction rewardTx = new Transaction(null, wallet.getAddress(), miningReward);
        bc.pendingTransactions.add(rewardTx);
        bc.txPool.addTransaction(rewardTx);

        this.hashMined += miningReward;
        this.wallet.getBalance(bc);

        logger.success("Bloque #" + newBlock.getIndex() + " minado por " + wallet.getAlias() + " en " + timeTaken
                + "ms | Nonce: " + newBlock.getNonce() + " | Recompensa: " + miningReward);
    }

    @Override
    public boolean validateBlock(Block block) {
        // Ideally, validateBlock should receive the blockchain instance or difficulty
        // For now, we can assume a default or try to get it if we change the signature.
        // But to keep signature, we will use BlockValidator with a hardcoded difficulty
        // or similar logic.
        // Wait, the original code had hardcoded difficulty 3 here.
        return BlockValidator.validateBlock(block, 3);
    }

    public float getTotalMined() {
        return hashMined;
    }

    public float getHashRate() {
        return hashRate;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public String getAddress() {
        return wallet.getAddress();
    }

    public float getBalance(Blockchain bc) {
        return wallet.getBalance(bc);
    }

    @Override
    public String toString() {
        return "Miner[alias=" + wallet.getAlias() + ", hashRate=" + hashRate +
                ", totalMined=" + hashMined + "]";
    }

}
