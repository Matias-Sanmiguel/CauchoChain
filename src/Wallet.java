public class Wallet extends WalletBase implements IWallet {

	// Alias público/identificador de la wallet
	private String alias;

	// Balance en caché para evitar recalcular constantemente
	private float cachedBalance = 0.0f;

	public Wallet(String alias) {
		this.alias = alias;
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
	
		return tx;
	}
    // 1. Verifica que el monto sea positivo.
    // 2. Verifica que haya fondos suficientes en la wallet.
    // 3. Crea y devuelve una nueva transacción.

	public float getCachedBalance() {
		return cachedBalance;
	}
}
