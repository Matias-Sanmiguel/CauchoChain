import miner.Miner;
import model.Blockchain;
import model.Transaction;
import wallet.Wallet;


public class DemoTest {
    public static void main(String[] args) {
        try {
            System.out.println("Iniciando DemoTest...");

            Blockchain bc = new Blockchain();
            Miner miner = new Miner(1.0f, "miner1");
            Wallet juan = new Wallet("juan");
            Wallet pancho = new Wallet("pancho");

            Transaction genesisReward = new Transaction(null, juan.getAddress(), 100.0f);
            bc.pendingTransactions.add(genesisReward);
            bc.txPool.addTransaction(genesisReward);

            System.out.println("Minando bloque genesis con recompensa a juan...");
            miner.mine(bc);

            float juanBal = bc.getBalance(juan.getAddress());
            System.out.println("Balance juan después del minado: " + juanBal);
            if (Math.abs(juanBal - 100.0f) > 0.001f) throw new RuntimeException("juan balance esperado 100, obtenido=" + juanBal);

            Transaction tx = juan.createTransaction(pancho.getAddress(), 40.0f, bc);
            bc.createTransaction(tx);

            System.out.println("Minando bloque con la transacción juan -> pancho...");
            miner.mine(bc);

            float panchoBal = bc.getBalance(pancho.getAddress());
            float juanBalAfter = bc.getBalance(juan.getAddress());

            System.out.println("Balance pancho: " + panchoBal);
            System.out.println("Balance juan ahora: " + juanBalAfter);

            if (Math.abs(panchoBal - 40.0f) > 0.001f) throw new RuntimeException("pancho balance esperado 40, obtenido=" + panchoBal);
            if (Math.abs(juanBalAfter - 60.0f) > 0.001f) throw new RuntimeException("juan balance esperado 60, obtenido=" + juanBalAfter);

            if (!bc.isChainValid()) throw new RuntimeException("La cadena no es válida");

            System.out.println("TEST PASSED");
        } catch (Exception e) {
            System.err.println("TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
