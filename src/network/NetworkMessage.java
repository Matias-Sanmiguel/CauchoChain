package network;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        TRANSACTION, BLOCK, CHAIN_REQUEST, CHAIN_RESPONSE, PING
    }

    public Type type;
    private Object payload;
    private long timestamp;

    public NetworkMessage(Type type, Object payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public Type getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
