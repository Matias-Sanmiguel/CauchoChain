package model;
import java.util.Map;
import java.util.UUID;

import wallet.WalletBase;

import java.util.HashMap;


public class SmartContract extends WalletBase {

    private UUID id;
    private String code;
    private String owner;
    private Map<String, Object> state;
    // Composición: referencia al ContractRegistry que contiene este contrato
    private ContractRegistry registry;

    public SmartContract(String code, String owner) {
        this.id = UUID.randomUUID();
        this.code = code;
        this.owner = owner;
        this.state = new HashMap<>();
    }

    //Establece el registro que contiene este contrato (composición).
    public void setRegistry(ContractRegistry registry) {
        this.registry = registry;
    }

    public ContractRegistry getRegistry() {
        return registry;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getOwner() {
        return owner;
    }

    
    //Ejecuta el contrato con los parámetros dados.
    public void execute(Map<String, Object> params) {
        if (params == null) return;
        // Fusiona los parámetros en el estado (sobrescribe claves existentes)
        state.putAll(params);
    }

    
    //Validación básica: asegura que `id` y `code` existan
    public boolean validate() {
        return id != null && code != null && !code.isEmpty();
    }

    
    //Devuelve una copia del mapa de estado para evitar exponer el mapa interno directamente.
    public Map<String, Object> getState() {
        return new HashMap<>(state);
    }

    @Override
    public String toString() {
        return "SmartContract[id=" + id + ", owner=" + owner + "]";
    }
}
