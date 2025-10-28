public class InMemoryNetworkNode implements INetworkNode {
    private String id;
    private String address;
    private NetworkMediator mediator;
    private Blockchain blockchain;

    public InMemoryNetworkNode(String id, String address, NetworkMediator mediator, Blockchain bc) {
        this.id = id;
        this.address = address;
        this.mediator = mediator;
        this.blockchain = bc;
    }

    @Override
    public String getId() { return id; }

    @Override
    public void connect() { mediator.registerNode(this); }

    @Override
    public void disconnect() { mediator.unregisterNode(this); }

    @Override
    public void broadcastTransaction(Transaction tx) {
        NetworkMessage msg = new NetworkMessage(NetworkMessage.Type.TRANSACTION, tx);
        mediator.broadcast(msg, id);
    }

    @Override
    public void broadcastBlock(Block b) {
        NetworkMessage msg = new NetworkMessage(NetworkMessage.Type.BLOCK, b);
        mediator.broadcast(msg, id);
    }

    @Override
public void onReceiveMessage(NetworkMessage msg, String from) {
    switch (msg.getType()) {
        case TRANSACTION:
            blockchain.createTransaction((Transaction) msg.getPayload());
            break;
        case BLOCK:
            blockchain.addBlock((Block) msg.getPayload());
            break;
        case PING:
            System.out.println(id + " recibió PING de " + from);
            break;
        default:
            System.out.println("Mensaje no reconocido.");
            break;
        }
    }
}
