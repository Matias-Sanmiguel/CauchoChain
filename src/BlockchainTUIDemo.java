import java.io.IOException;
import model.*;
import wallet.*;
import miner.*;
import utils.Logger;

public class BlockchainTUIDemo {
    public static void main(String[] args) {
        try {
            Logger logger = Logger.getInstance();

            int port = 5000;
            if (args.length > 0) {
                try {
                    port = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    System.err.println("Puerto invalido, usando default: 5000");
                }
            }

            Blockchain bc = new Blockchain();
            BlockchainTUI tui = new BlockchainTUI(bc, port);

            Wallet juan = new Wallet("juan");
            Wallet pancho = new Wallet("pancho");
            Wallet maria = new Wallet("maria");

            tui.addWallet("juan", juan);
            tui.addWallet("pancho", pancho);
            tui.addWallet("maria", maria);

            logger.info("Sistema iniciado. Preparando blockchain...");

            logger.info("Presiona [M] para minar el primer bloque");

            tui.start();

        } catch (IOException e) {
            System.err.println("Error en demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
