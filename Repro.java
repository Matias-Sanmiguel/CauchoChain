import model.*;
import miner.*;
import wallet.*;

public class Repro {
    public static void main(String[] args) throws Exception {
        Blockchain bc1 = new Blockchain();
        Blockchain bc2 = new Blockchain();
        Wallet w1 = new Wallet("alice");
        Transaction reward = new Transaction(null, w1.getAddress(), 25f);
        bc1.createTransaction(reward);
        Miner miner = new Miner(2.0f, "minerOne");
        bc1.minePendingTransactions(miner);
        Block block = bc1.getLatestBlock();
        bc2.addBlock(block);
        System.out.println("Chain2 size: " + bc2.getChain().size());
    }
}
