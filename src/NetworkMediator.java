public interface NetworkMediator {
    void registerNode(INetworkNode node);
    void unregisterNode(INetworkNode node);
    void broadcast(NetworkMessage msg, String from);
    void sendTo(NetworkMessage msg, String to, String from);
}
