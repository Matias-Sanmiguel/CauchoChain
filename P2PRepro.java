import model.*;
import miner.*;
import wallet.*;
import network.*;

public class P2PRepro {
    public static void main(String[] args) throws Exception {
        Blockchain bc1 = new Blockchain();
        Blockchain bc2 = new Blockchain();

        P2PNetworkNode node1 = new P2PNetworkNode("node1", 7001, bc1);
        P2PNetworkNode node2 = new P2PNetworkNode("node2", 7002, bc2);
        bc1.addNode(node1);
        bc2.addNode(node2);

        node1.connect();
        node2.connect();
        Thread.sleep(500);
        node1.connectToPeer("127.0.0.1", 7002);
        node2.connectToPeer("127.0.0.1", 7001);
        Thread.sleep(1500);

        Transaction reward = new Transaction(null, "addr", 1f);
        bc1.createTransaction(reward);
        Miner miner = new Miner(2.0f, "minerOne");
        bc1.minePendingTransactions(miner);
        Block latest = bc1.getLatestBlock();
        node1.broadcastBlock(latest);

        Thread.sleep(2000);
        System.out.println("BC2 size: " + bc2.getChain().size());
    }
}
