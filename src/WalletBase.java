import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

public abstract class WalletBase {

	private KeyPair privateKey;
	// stringuear la public key
	private String publicKey;

	public WalletBase() {
		// creacion de la wallet en si mediante la creacion de claves
		CryptoUtils cu = new CryptoUtils();
		this.privateKey = cu.generateKeyPair();
		PublicKey pk = this.privateKey.getPublic();
		this.publicKey = Base64.getEncoder().encodeToString(pk.getEncoded());
	}


	//da la clave publica
	public String getAddress() {
		return publicKey;
	}

	
	 //firma y devuelve la firma
	public String signData(String data) {
		CryptoUtils cu = new CryptoUtils();
		return cu.sign(data, privateKey);
	}

	//ver si coinciden las firmas
	public boolean verify(String sig, String data) {
		CryptoUtils cu = new CryptoUtils();
		// CryptoUtils.verify toma (data, signature, KeyPair)
		return cu.verify(data, sig, privateKey);
	}

	// hicimos esto para poder mostrar el keypar mas tarde en las sublcases
	protected KeyPair getKeyPair() {
        return privateKey;
	}
}

