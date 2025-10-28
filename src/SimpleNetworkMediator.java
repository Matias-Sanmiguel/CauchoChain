import java.util.HashMap;
import java.util.Map;

public class SimpleNetworkMediator implements NetworkMediator {

    private Map<String, INetworkNode> nodes = new HashMap<>();

    @Override
    public void registerNode(INetworkNode node) {
        nodes.put(node.getId(), node);
        System.out.println("Nodo " + node.getId() + " conectado a la red.");
    }

    @Override
    public void unregisterNode(INetworkNode node) {
        nodes.remove(node.getId());
        System.out.println("Nodo " + node.getId() + " desconectado de la red.");
    }

    @Override
    public void broadcast(NetworkMessage msg, String from) {
        for (Map.Entry<String, INetworkNode> entry : nodes.entrySet()) {
            if (!entry.getKey().equals(from)) {
                entry.getValue().onReceiveMessage(msg, from);
            }
        }
    }

    @Override
    public void sendTo(NetworkMessage msg, String to, String from) {
        INetworkNode target = nodes.get(to);
        if (target != null) {
            target.onReceiveMessage(msg, from);
        }
    }
}
