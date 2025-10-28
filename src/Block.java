import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Block {

    private String hash;
    private String prevHash;
    private String data;
    private Date timestamp;
    private int index;
    private List<Transaction> transactions;
    private int nonce;


    public Block(int index, List<Transaction> transactions, String prevHash) {
        this.index = index;
        this.transactions = new ArrayList<>(transactions);
        this.prevHash = prevHash;
        this.nonce = 0;
        this.timestamp = new Date();
        this.hash = calculateHash();
    }

    // Concatena todoo para generar el hash del bloque
    public String calculateHash(){
        String datoParaHash = Integer.toString(index) + (prevHash == null ? "" : prevHash) + Long.toString(timestamp.getTime()) + Integer.toString(nonce) + getMerkleRoot();
        return HashUtil.sha256(dataToHash);
    }

    // La merkleRoot es básicamente una forma de reducir las transacciones de un bloque que le des, es una formula matematica que se usa para hashear
    public String getMerkleRoot() {
        List<String> treeLayer = new ArrayList<>();
        for (Transaction tx : transactions) {
            treeLayer.add(tx.calculateHash());
        }
        if (treeLayer.isEmpty()) return "";
        while (treeLayer.size() > 1) {
            List<String> nextLayer = new ArrayList<>();
            for (int i = 0; i < treeLayer.size(); i += 2) {
                if (i + 1 < treeLayer.size()) {
                    nextLayer.add(HashUtil.sha256(treeLayer.get(i) + treeLayer.get(i + 1)));
                } else {
                    nextLayer.add(HashUtil.sha256(treeLayer.get(i) + treeLayer.get(i)));
                }
            }
            treeLayer = nextLayer;
        }
        return treeLayer.get(0);
    }

}

