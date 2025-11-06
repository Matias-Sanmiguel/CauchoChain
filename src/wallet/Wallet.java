package wallet;
import java.util.UUID;
import model.Blockchain;
import model.Transaction;
import model.User;

public class Wallet extends WalletBase implements IWallet {

	// Alias público/identificador de la wallet
	private String alias;

	// Identificador del usuario propietario . Puede ser null si no está asignada.
	private UUID ownerId;

	// Balance en caché para evitar recalcular constantemente
	private float cachedBalance = 0.0f;

	public Wallet(String alias) {
		this.alias = alias;
	}

	// Constructor que enlaza la wallet con un User
	public Wallet(String alias, User owner) {
		this.alias = alias;
		if (owner != null) {
			this.ownerId = owner.getUserId();
			// Mantener sincronizado el alias en el User
			owner.setAlias(alias);
		}
	}

	public String getAlias() {
		return alias;
	}

	@Override
	public String getAddress() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public UUID getOwnerId() {
		return ownerId;
	}


	@Override
	public float getBalance(Blockchain bc) {
		if (bc == null) {
			return cachedBalance;
		}
		cachedBalance = bc.getBalance(alias);
		return cachedBalance;
	}
    //  Devuelve el balance consultando la cadena (Blockchain) y actualiza cachedBalance.
    //  Si la blockchain es null, devuelve el valor en caché.


	@Override
	public Transaction createTransaction(String to, float amt, Blockchain bc) {
		float balance = getBalance(bc);
		if (amt <= 0) {
			throw new IllegalArgumentException("El monto debe ser positivo");
		}
		if (amt > balance) {
			throw new IllegalArgumentException("Fondos insuficientes: " + balance + " solicitados: " + amt);
		}

		Transaction tx = new Transaction(this.alias, to, amt);


        // firma la transaccion con el keypar de la wallet
		try {
			java.security.KeyPair kp = getKeyPair();
			tx.signTransaction(kp);
		} catch (Exception e) {
			// Si falla la firma, lanzar para que el llamador sepa
			throw new RuntimeException("Error al firmar la transacción: " + e.getMessage(), e);
		}
	
		return tx;
	}

	public float getCachedBalance() {
		return cachedBalance;
	}

	public static Wallet createWallet(String alias) {
		return new Wallet(alias);
	}
}
