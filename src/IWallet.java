
public interface IWallet {

	//Devuelve la dirección / alias de la wallet.
	String getAddress();

	//Devuelve el balance de esta wallet consultando la cadena provista.
	float getBalance(Blockchain bc);

	
    //Crea una transacción desde esta wallet hacia `to` por `amt` usando la información de la cadena.
	Transaction createTransaction(String to, float amt, Blockchain bc);

}
