package network;
public class NetworkMessage {
    public enum Type { TRANSACTION, BLOCK, CHAIN_REQUEST, CHAIN_RESPONSE, PING }

    public Type type;
    private Object payload;
    private long timestamp;

    public NetworkMessage(Type type, Object payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public Type getType() { return type; }
    public Object getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }
}
