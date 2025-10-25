import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

public abstract class WalletBase {

	// Par de claves (privada/publica)
	private KeyPair privateKey;

	// Representación en String (Base64) de la clave pública — usada como address
	private String publicKey;

	public WalletBase() {
		// Generar par de claves al crear la wallet
		CryptoUtils cu = new CryptoUtils();
		this.privateKey = cu.generateKeyPair();
		PublicKey pk = this.privateKey.getPublic();
		this.publicKey = Base64.getEncoder().encodeToString(pk.getEncoded());
	}


	//Devuelve la dirección (representación de la clave pública).
	public String getAddress() {
		return publicKey;
	}

	
	 //Firma los datos con la clave privada de esta wallet y devuelve la firma (Base64).
	public String signData(String data) {
		CryptoUtils cu = new CryptoUtils();
		return cu.sign(data, privateKey);
	}

	//Verifica que la firma `sig` corresponde a `data` usando la clave pública de esta wallet.
	public boolean verify(String sig, String data) {
		CryptoUtils cu = new CryptoUtils();
		// CryptoUtils.verify toma (data, signature, KeyPair)
		return cu.verify(data, sig, privateKey);
	}

	// Exponer el KeyPair protegido en caso de que subclases lo necesiten
	protected KeyPair getKeyPair() {
		return privateKey;
	}
}

