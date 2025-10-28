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
    private static final CryptoUtils crypto = new CryptoUtils();


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
        String datoParaHash = index + (prevHash == null ? "" : prevHash) + timestamp.getTime() + nonce + getMerkleRoot();
        return crypto.hash(datoParaHash);
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
                    nextLayer.add(crypto.hash(treeLayer.get(i) + treeLayer.get(i + 1)));
                } else {
                    nextLayer.add(crypto.hash(treeLayer.get(i) + treeLayer.get(i)));
                }
            }
            treeLayer = nextLayer;
        }
        return treeLayer.get(0);
    }

    // valida las transacciones
    public boolean hasValidTransactions() {
        if (transactions == null) return true;
        for (Transaction tx : transactions) {
            if (tx == null) continue;
            if (!tx.isValid()) return false;
        }
        return true;
    }

    // Getters y setters para cumplir con encapsulamiento
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public int getNonce() {
        return nonce;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public void incrementNonce() {
        this.nonce++;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public int getIndex() {
        return index;
    }

}
