package model;
import java.util.ArrayList;
import java.util.List;

public class TransactionPool {
    // la transaccion entra como pendiente para depsues ser valida
    private final List<Transaction> pending;

    public TransactionPool() {

        this.pending = new ArrayList<>();
    }

    // añade la transaccion pendiente como valida ya
    public synchronized void addTransaction(Transaction trans) {
        if (trans == null) return;
        if (!trans.isValid()) return;
        this.pending.add(trans);
    }

    // trae las transacciones validas del pool en una lista
    public synchronized List<Transaction> getValidTransactions() {
        List<Transaction> copy = new ArrayList<>();
        for (Transaction trans : this.pending) {
            if (trans != null && trans.isValid()) copy.add(trans);
        }
        return copy;
    }

    // borra las transacciones pendientes que ya validamos con lo de addtransaction
    public synchronized void removeTransactions(List<Transaction> transaction) {
        if (transaction == null || transaction.isEmpty()) return;
        this.pending.removeAll(transaction);
    }

    // deletea el pool completo de cosas
    public synchronized void clear() {
        this.pending.clear();
    }

    // trae todas las pendientes
    // no es del to do necesario pero sirve para ver si quedan colgadas
    public synchronized List<Transaction> getPending() {
        return new ArrayList<>(this.pending);
    }
}


/*
* tuvimos que usar este atributo de synchronized porque sino
se sobreescribirian los datos de manera incorrecta por la cantidad de entradas
* la que se acostumran las blockchain/
 */