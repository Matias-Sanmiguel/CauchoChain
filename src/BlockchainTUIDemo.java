import java.io.IOException;

public class BlockchainTUIDemo {
    public static void main(String[] args) {
        try {
            // Crear blockchain y TUI
            Blockchain bc = new Blockchain();
            BlockchainTUI tui = new BlockchainTUI(bc);

            // Crear wallets
            Wallet juan = new Wallet("juan");
            Wallet pancho = new Wallet("pancho");
            Wallet maria = new Wallet("maria");

            // Registrar wallets en la TUI
            tui.addWallet("juan", juan);
            tui.addWallet("pancho", pancho);
            tui.addWallet("maria", maria);

            tui.addLog("Sistema iniciado. Preparando blockchain...");

            // Dar recompensa inicial a juan
            Transaction genesisReward = new Transaction(null, juan.getAddress(), 100.0f);
            bc.pendingTransactions.add(genesisReward);
            bc.txPool.addTransaction(genesisReward);

            tui.addLog("Presiona [M] para minar el bloque génesis y começar");

            // Iniciar TUI (bloqueante)
            tui.start();

        } catch (IOException e) {
            System.err.println("Error en demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
