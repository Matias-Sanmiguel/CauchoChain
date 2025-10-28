import java.security.KeyPair;

public class Transaction {
    public String fromAddress;
    public String toAddress;
    public float amount;
    public float fee;
    public String signature;

    public Transaction(String from, String to, float amount) {
        this.fromAddress = from;
        this.toAddress = to;
        this.amount = amount;
        this.fee = 0.0f;
        this.signature = null;
    }

    public Transaction(String from, String to, float amount, float fee) {
        this.fromAddress = from;
        this.toAddress = to;
        this.amount = amount;
        this.fee = fee;
        this.signature = null;
    }

    // CALCULA EL HASh
    public String calculateHash() {
        String data = (fromAddress == null ? "" : fromAddress) + (toAddress == null ? "" : toAddress) + amount + fee + (signature == null ? "" : signature);
        return new CryptoUtils().hash(data);
    }

    // firma
    public void signTransaction(KeyPair keyPair) {
        if (keyPair == null) throw new IllegalArgumentException("KeyPair requerido para firmar");
        CryptoUtils crypto = new CryptoUtils();
        this.signature = crypto.sign(calculateHash(), keyPair);
    }

    // se fija si la transaccion es valida por las firmas
    public boolean isValid() {
        if (this.fromAddress == null) return true; // recompensa
        if (this.signature == null) return false; // si no esta firmada se invalida
        return true;
    }
}
