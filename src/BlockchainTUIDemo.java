import java.io.IOException;
import model.*;
import wallet.*;
import miner.*;
import utils.Logger;

public class BlockchainTUIDemo {
    public static void main(String[] args) {
        try {
            Logger logger = Logger.getInstance();

            Blockchain bc = new Blockchain();
            BlockchainTUI tui = new BlockchainTUI(bc);

            Wallet juan = new Wallet("juan");
            Wallet pancho = new Wallet("pancho");
            Wallet maria = new Wallet("maria");

            tui.addWallet("juan", juan);
            tui.addWallet("pancho", pancho);
            tui.addWallet("maria", maria);

            logger.info("Sistema iniciado. Preparando blockchain...");

            Transaction genesisReward = new Transaction(null, juan.getAddress(), 100.0f);
            bc.pendingTransactions.add(genesisReward);
            bc.txPool.addTransaction(genesisReward);

            logger.info("Presiona [M] para minar el bloque génesis");

            tui.start();

        } catch (IOException e) {
            System.err.println("Error en demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
