package miner;

import java.util.ArrayList;
import java.util.List;
import model.Block;
import model.Blockchain;
import model.Transaction;
import model.User;
import wallet.Wallet;
import utils.Logger;

public class Miner implements IMiner {
    private float hashMined;
    private float hashRate;
    private Wallet wallet;
    private Logger logger;

    public Miner(float hashRate){
        this.hashMined=0.0f;
        this.hashRate=hashRate;
        this.wallet=new Wallet("Miner_" + System.currentTimeMillis());
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

    public void mine(Blockchain bc){
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
            newBlock.setNonce(newBlock.getNonce()+1);
            newBlock.setHash(newBlock.calculateHash());
        }

        long timeTaken = System.currentTimeMillis() - startTime;

        if (!validateBlock(newBlock)) {
            logger.error("El bloque minado no es válido.");
            throw new RuntimeException("El bloque minado no es válido.");
        }

        bc.getChain().add(newBlock);
        bc.txPool.removeTransactions(transactionsToMine);
        bc.pendingTransactions.removeAll(transactionsToMine);

        float miningReward = bc.getMiningReward();

        Transaction rewardTx = new Transaction(null, wallet.getAddress(), miningReward);
        bc.pendingTransactions.add(rewardTx);
        bc.txPool.addTransaction(rewardTx);

        this.hashMined += miningReward;

        logger.success("Bloque #" + newBlock.getIndex() + " minado por " + wallet.getAlias() + " en " + timeTaken + "ms | Nonce: " + newBlock.getNonce() + " | Recompensa: " + miningReward);
    }

    @Override
    public boolean validateBlock(Block block){
        if (!block.getHash().equals(block.calculateHash())) {
            logger.error("Hash inválido en bloque #" + block.getIndex());
            return false;
        }

        if (!block.hasValidTransactions()) {
            logger.error("Transacciones inválidas en bloque #" + block.getIndex());
            return false;
        }

        int difficulty = 3;
        String target = new String(new char[difficulty]).replace('\0', '0');
        if (!block.getHash().substring(0, Math.min(difficulty, block.getHash().length())).equals(target)) {
            logger.error("Dificultad no cumplida en bloque #" + block.getIndex());
            return false;
        }

        logger.debug("Bloque #" + block.getIndex() + " validado correctamente");
        return true;
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
