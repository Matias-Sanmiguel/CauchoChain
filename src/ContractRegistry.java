import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


//Registro que contiene los contratos desplegados y permite ejecutarlos por id.
public class ContractRegistry {

    private final List<SmartContract> contracts = new ArrayList<>();
    //Composición: este registry posee una instancia de Blockchain
    private final Blockchain blockchain;

    public ContractRegistry() {
        this.blockchain = null;
    }

    public ContractRegistry(Blockchain blockchain) {
        this.blockchain = blockchain != null ? blockchain : new Blockchain();
    }

    
    //Despliega un smart contract en el registro.
    //El registro se establecerá en el contrato para mantener la relación de composición.
    public String deploy(SmartContract sc) {
        if (sc == null) throw new IllegalArgumentException("SmartContract cannot be null");
        sc.setRegistry(this);
        contracts.add(sc);
        return sc.getId() == null ? null : sc.getId().toString();
    }

    
    //Busca un contrato desplegado por su id. Devuelve null si no lo encuentra.
    public SmartContract getContractRegistry(UUID id) {
        if (id == null) return null;
        for (SmartContract sc : contracts) {
            if (id.equals(sc.getId())) return sc;
        }
        return null;
    }

    
    //Ejecuta el contrato con el id y parámetros indicados. Lanza excepción si no se encuentra o es inválido.
    public void execute(UUID id, Map<String, Object> params) {
        SmartContract sc = getContractRegistry(id);
        if (sc == null) {
            throw new IllegalArgumentException("Contract not found: " + id);
        }
        if (!sc.validate()) {
            throw new IllegalStateException("Contract validation failed: " + id);
        }
        sc.execute(params);
    }

    public List<SmartContract> getContracts() {
        return new ArrayList<>(contracts);
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }
}
