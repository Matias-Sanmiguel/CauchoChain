public interface INetworkNode {
    String getId();
    void connect();
    void disconnect();
    void broadcastTransaction(Transaction tx);
    void broadcastBlock(Block b);
    void onReceiveMessage(NetworkMessage msg, String from);
}
