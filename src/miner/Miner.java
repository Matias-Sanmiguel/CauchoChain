package miner;

import java.util.ArrayList;
import java.util.List;
import model.Block;
import model.Blockchain;
import model.Transaction;
import model.User;
import wallet.Wallet;

public class Miner implements IMiner {
    private float hashMined;
    private float hashRate;
    private Wallet wallet;
    public Miner(float hashRate){
        this.hashMined=0.0f;
        this.hashRate=hashRate;
        this.wallet=new Wallet("Miner_" + System.currentTimeMillis());
    }
    public Miner(float hashRate, String minerAlias) {
        this.hashRate = hashRate;
        this.hashMined = 0.0f;
        this.wallet = new Wallet(minerAlias);
    }

    public Miner(float hashRate, User minerUser) {
        this.hashRate = hashRate;
        this.hashMined = 0.0f;
        this.wallet = new Wallet("Miner_" + minerUser.getName(), minerUser);
    }



    public void mine(Blockchain bc){
        if (bc.pendingTransactions.isEmpty()) {
            System.out.println("No hay transacciones pendientes para minar.");
            return;
        }


        List<Transaction> transactionsToMine = new ArrayList<>(bc.pendingTransactions);


        // calcular prevHash de forma segura
        Block latest = bc.getLatestBlock();
        String prevHash = (latest == null) ? "0" : latest.getHash();

        // crear bloque con la dirección del minero
        Block newBlock = new Block(bc.getChain().size(), transactionsToMine, prevHash, this.getAddress());

        int difficulty = bc.getDifficulty(); // saca la diff desde blockchain

        String target = new String(new char[difficulty]).replace('\0', '0');

        long startTime = System.currentTimeMillis(); //toma el tiempo de inicio para ver cuanto tarda el minado

        while (!newBlock.getHash().substring(0, Math.min(difficulty, newBlock.getHash().length())).equals(target)) {
            // while para encontrar el hash correcto, corta el bucle cuando el hash tiene los ceros necesarios al inicio
            newBlock.setNonce(newBlock.getNonce()+1);
            newBlock.setHash(newBlock.calculateHash());
        }

        long endTime = System.currentTimeMillis();// pone el tiempo en el que finalizo la mineria
        long timeTaken = endTime - startTime;// calcula el tiempo que ha tardado en minar el bloque


        if (!validateBlock(newBlock)) { //si el bloque no es valido, lanza una excepcion
            throw new RuntimeException("El bloque minado no es válido.");
        }

        bc.getChain().add(newBlock); //añade el bloque a la cadena de bloques

        bc.txPool.removeTransactions(transactionsToMine); //elimina las transacciones minadas del pool de transacciones
        bc.pendingTransactions.removeAll(transactionsToMine); //elimina las transacciones minadas de las transacciones pendientes

        float miningReward = bc.getMiningReward(); // saca la reward de la blockchain con el getter


        Transaction rewardTx = new Transaction(null, wallet.getAddress(), miningReward);
        // crea una transaccion de recompensa hacia la direccion del minero
        bc.pendingTransactions.add(rewardTx); //añade la transaccion de recompensa a las transacciones pendientes
        bc.txPool.addTransaction(rewardTx); //añade la transaccion de recompensa al pool(registro) de transacciones

        this.hashMined += miningReward; //actualiza el total minado por el minero

        System.out.println("✓ Bloque #" + newBlock.getIndex() + " minado por " + wallet.getAlias());
        System.out.println("  Hash: " + newBlock.getHash());
        System.out.println("  Nonce: " + newBlock.getNonce());
        System.out.println("  Tiempo: " + timeTaken + "ms");
        System.out.println("  Recompensa: " + miningReward + " | Total minado: " + hashMined);
    }

    @Override
    public boolean validateBlock (Block block){
        if (!block.getHash().equals(block.calculateHash())) { //verifica que el hash del bloque sea correcto
            //el hash del bloque deberia ser igual al hash si lo recalculo desde cero
            // si no coinciden, el hash fue modificado, el bloque se considera invalido
            return false;
        }

        if (!block.hasValidTransactions()) { //verifica que las transacciones del bloque sean validas, chequea firmas, saldos, etc
            return false;
        }

        int difficulty = 3;
        String target = new String(new char[difficulty]).replace('\0', '0');
        if (!block.getHash().substring(0, Math.min(difficulty, block.getHash().length())).equals(target)) {
            return false;
        }

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
