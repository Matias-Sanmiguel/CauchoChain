public class BlockchainController {
    private Blockchain blockchain;
    private NetworkMediator mediator;
    private Logger logger;

    public BlockchainController(Blockchain blockchain) {
        this.blockchain = blockchain;
        this.mediator = null;
        this.logger = new Logger();
    }

    public BlockchainController(Blockchain blockchain, NetworkMediator mediator, Logger logger) {
        this.blockchain = blockchain;
        this.mediator = mediator;
        this.logger = logger != null ? logger : new Logger();
    }

    /**
     * Inicializa la red si no existe un mediator. Crea un SimpleNetworkMediator por defecto.
     */
    public void initNetwork(){
        if (this.mediator == null) {
            this.mediator = new SimpleNetworkMediator();
            logger.log("Network initialized with SimpleNetworkMediator");
        } else {
            logger.log("Network already initialized");
        }
    }

    /**
     * Añade un nodo a la red y lo registra en el mediator.
     */
    public void addNode(INetworkNode node) {
        if (node == null) return;
        if (this.mediator == null) initNetwork();
        mediator.registerNode(node);
        logger.log("Node added: " + node.getId());
    }

    /**
     * Maneja una nueva transacción: la añade al blockchain (que validará) y la broadcastea en la red.
     */
    public void handleNewTransaction(Transaction tx) {
        if (tx == null) return;
        try {
            blockchain.createTransaction(tx);
            logger.log("New transaction handled: " + tx.calculateHash());
            if (mediator != null) {
                NetworkMessage msg = new NetworkMessage(NetworkMessage.Type.TRANSACTION, tx);
                mediator.broadcast(msg, "controller");
            }
        } catch (Exception e) {
            logger.log("Failed to handle transaction: " + e.getMessage());
        }
    }

    /**
     * Maneja una solicitud de minado: delega en el miner para que mine la blockchain y
     * broadcastea el bloque minado a la red.
     */
    public void handleMiningRequest(IMiner miner) {
        if (miner == null) return;
        try {
            // Dejar que el miner haga su trabajo
            miner.mine(blockchain);

            // Después del minado, obtener el último bloque y anunciarlo
            Block latest = blockchain.getLatestBlock();
            if (latest != null && mediator != null) {
                NetworkMessage msg = new NetworkMessage(NetworkMessage.Type.BLOCK, latest);
                String from = (miner instanceof Miner) ? ((Miner) miner).getAddress() : "miner";
                mediator.broadcast(msg, from);
                logger.log("Broadcasted new block: " + latest.getHash() + " from " + from);
            }
        } catch (Exception e) {
            logger.log("Mining request failed: " + e.getMessage());
        }
    }

    /**
     * Maneja el deploy de un smart contract a la blockchain y notifica.
     */
    public void handleContractDeploy(SmartContract sc) {
        if (sc == null) return;
        try {
            String id = blockchain.deployContract(sc);
            logger.log("Contract deployed: " + id);
            if (mediator != null) {
                NetworkMessage msg = new NetworkMessage(NetworkMessage.Type.CHAIN_RESPONSE, sc);
                mediator.broadcast(msg, "controller");
            }
        } catch (Exception e) {
            logger.log("Contract deployment failed: " + e.getMessage());
        }
    }

    // Getters / setters
    public NetworkMediator getMediator() {
        return mediator;
    }
    public void setMediator(NetworkMediator mediator) {
        this.mediator = mediator;
    }
    public Logger getLogger() {
        return logger;
    }
    public void setLogger(Logger logger) { this.logger = logger; }
    public Blockchain getBlockchain() { return blockchain; }
    public void setBlockchain(Blockchain blockchain) { this.blockchain = blockchain; }
}
