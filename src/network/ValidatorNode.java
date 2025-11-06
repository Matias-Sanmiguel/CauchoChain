package network;
import model.Block;
import model.CryptoUtils;

public class ValidatorNode {
    private float stake;
    private String publicKey;
    private String alias;
    private String address;
    private boolean active;

    private static final CryptoUtils crypto = new CryptoUtils();

    public ValidatorNode(float stake, String publicKey) {
        this.stake = stake;
        this.publicKey = publicKey;
        this.alias = "Nodo-" + System.currentTimeMillis();
        this.address = generateAddress();
        this.active = true;
    }

    public ValidatorNode(String alias, String address) {
        this.stake = 0.0f;
        this.publicKey = null;
        this.alias = alias;
        this.address = address;
        this.active = true;
    }

    public ValidatorNode(float stake) {
        this(stake, null);
    }

    public ValidatorNode() {
        this(0.0f, null);
    }

    private String generateAddress() {
        return "0x" + Integer.toHexString(hashCode());
    }

    public float getStake() {
        return stake;
    }

    public void setStake(float stake) {
        this.stake = stake;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    // TODO POW

    public boolean validate(Block b){
        if (b == null) return false;

        // Comprueba transacciones
        if(!b.hasValidTransactions()){
            return false;
        }

        // calcula de vuelta el hash y lo compara, si no coincide, el bloque fue modificado
        String recalculatedHash = b.calculateHash();
        if(!recalculatedHash.equals(b.getHash())){
            return false;
        }


        return true;
    }

    // firma el bloque
    public String sign(Block b, java.security.KeyPair keyPair){
        if (b == null || keyPair == null) return null;
        // primero por las dudas vemos si el hash existe
        String hash = b.getHash();
        if (hash == null || hash.isEmpty()) {
            hash = b.calculateHash();
            b.setHash(hash);
        }
        // firma
        String signature = crypto.sign(hash, keyPair);
        b.setSignature(signature);


        //guardamos la firma publicca
        try {
            java.security.PublicKey pk = keyPair.getPublic();
            byte[] pkBytes = pk.getEncoded();
            String pkBase64 = java.util.Base64.getEncoder().encodeToString(pkBytes);
            this.publicKey = pkBase64;
        } catch (Exception ignored) {}
        return signature;
    }

    //chequea si la firma del bloque es verdadera con una libreria
    public boolean verifySignature(Block b, String signature, java.security.KeyPair keyPair){
        if (b == null || signature == null || keyPair == null) return false;
        String hash = b.getHash();
        if (hash == null || hash.isEmpty()) hash = b.calculateHash();
        return crypto.verify(hash, signature, keyPair);
    }

}
