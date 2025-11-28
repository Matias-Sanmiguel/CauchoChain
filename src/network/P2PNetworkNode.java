package network;

import model.Block;
import model.Blockchain;
import model.Transaction;
import utils.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class P2PNetworkNode implements INetworkNode {
    private String id;
    private int port;
    private Blockchain blockchain;
    private ServerSocket serverSocket;
    private List<ObjectOutputStream> peers;
    private boolean running;
    private Logger logger;

    public P2PNetworkNode(String id, int port, Blockchain blockchain) {
        this.id = id;
        this.port = port;
        this.blockchain = blockchain;
        this.peers = Collections.synchronizedList(new ArrayList<>());
        this.logger = Logger.getInstance();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void connect() {
        // Start the server thread to listen for incoming connections
        new Thread(this::startServer).start();
    }

    public void connectToPeer(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            registerPeer(socket);
            logger.info("Conectado a peer: " + host + ":" + port);
        } catch (IOException e) {
            logger.error("Error conectando a peer " + host + ":" + port + " - " + e.getMessage());
        }
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            logger.info("Nodo P2P iniciado en puerto " + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Nueva conexión entrante de: " + clientSocket.getInetAddress());
                registerPeer(clientSocket);
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Error en el servidor P2P: " + e.getMessage());
            }
        }
    }

    private void registerPeer(Socket socket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            peers.add(out);

            // Start a thread to listen to this peer
            new Thread(() -> listenToPeer(in, out, socket)).start();

            // Solicitar la cadena al conectarse
            out.writeObject(new NetworkMessage(NetworkMessage.Type.CHAIN_REQUEST, null));
            out.flush();

            // Send a PING or initial handshake if needed
            // out.writeObject(new NetworkMessage(NetworkMessage.Type.PING, "Hello from " +
            // id));

        } catch (IOException e) {
            logger.error("Error registrando peer: " + e.getMessage());
        }
    }

    private void listenToPeer(ObjectInputStream in, ObjectOutputStream out, Socket socket) {
        try {
            while (socket.isConnected() && !socket.isClosed()) {
                Object obj = in.readObject();
                if (obj instanceof NetworkMessage) {
                    onReceiveMessage((NetworkMessage) obj, out, socket.getInetAddress().toString());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.warning("Peer desconectado: " + socket.getInetAddress());
            // Remove peer from list (would need more robust management to remove the
            // specific output stream)
        }
    }

    @Override
    public void disconnect() {
        running = false;
        try {
            if (serverSocket != null)
                serverSocket.close();
            for (ObjectOutputStream out : peers) {
                out.close();
            }
            peers.clear();
        } catch (IOException e) {
            logger.error("Error desconectando nodo: " + e.getMessage());
        }
    }

    @Override
    public void broadcastTransaction(Transaction tx) {
        NetworkMessage msg = new NetworkMessage(NetworkMessage.Type.TRANSACTION, tx);
        broadcast(msg);
    }

    @Override
    public void broadcastBlock(Block b) {
        NetworkMessage msg = new NetworkMessage(NetworkMessage.Type.BLOCK, b);
        broadcast(msg);
    }

    private void broadcast(NetworkMessage msg) {
        List<ObjectOutputStream> failedPeers = new ArrayList<>();

        synchronized (peers) {
            for (ObjectOutputStream out : peers) {
                try {
                    out.writeObject(msg);
                    out.flush();
                } catch (IOException e) {
                    logger.error("Error enviando mensaje a peer: " + e.getMessage());
                    failedPeers.add(out);
                }
            }
            peers.removeAll(failedPeers);
        }
    }

    @Override
    public void onReceiveMessage(NetworkMessage msg, String from) {
        // Sobrecarga para compatibilidad si se llama internamente sin out
        onReceiveMessage(msg, null, from);
    }

    public void onReceiveMessage(NetworkMessage msg, ObjectOutputStream responder, String from) {
        logger.debug("Mensaje recibido de " + from + ": " + msg.getType());

        switch (msg.getType()) {
            case TRANSACTION:
                Transaction tx = (Transaction) msg.getPayload();
                // Avoid infinite loops or reprocessing known txs
                // In a real system, we check if we already have it in the pool
                try {
                    blockchain.addTransactionToPool(tx);
                    logger.info("Transacción recibida y añadida al pool.");
                } catch (Exception e) {
                    logger.warning("Transacción rechazada: " + e.getMessage());
                }
                break;

            case BLOCK:
                Block block = (Block) msg.getPayload();
                // Validate and add block
                // Simple check: is it the next block?
                if (block.getPrevHash().equals(blockchain.getLatestBlock().getHash())) {
                    // Agregar el bloque directamente a la cadena sin hacer broadcast
                    // para evitar loops infinitos (el nodo que minó ya hizo el broadcast)
                    List<Block> chain = blockchain.getChain();

                    // Validar estructura del bloque antes de agregarlo
                    if (model.BlockValidator.validateBlockStructure(block)) {
                        chain.add(block);
                        logger.info("Bloque recibido y añadido a la cadena: #" + block.getIndex());

                        // Remover transacciones del pool que ya están en el bloque
                        blockchain.txPool.removeTransactions(block.getTransactions());
                        blockchain.pendingTransactions.removeAll(block.getTransactions());
                    } else {
                        logger.warning("Bloque recibido tiene estructura inválida.");
                    }
                } else {
                    logger.warning("Bloque recibido no encaja en la cadena local.");
                    // Solicitar sincronización de cadena completa
                    if (responder != null) {
                        try {
                            responder.writeObject(new NetworkMessage(NetworkMessage.Type.CHAIN_REQUEST, null));
                            responder.flush();
                        } catch (IOException e) {
                            logger.error("Error solicitando cadena: " + e.getMessage());
                        }
                    }
                }
                break;

            case PING:
                logger.info("PING recibido de " + from);
                break;

            case CHAIN_REQUEST:
                logger.info("Solicitud de cadena recibida de " + from);
                if (responder != null) {
                    try {
                        List<Block> currentChain = new ArrayList<>(blockchain.getChain());
                        responder.writeObject(new NetworkMessage(NetworkMessage.Type.CHAIN_RESPONSE, currentChain));
                        responder.flush();
                    } catch (IOException e) {
                        logger.error("Error enviando cadena: " + e.getMessage());
                    }
                }
                break;

            case CHAIN_RESPONSE:
                logger.info("Cadena recibida de " + from + ". Verificando...");
                try {
                    List<Block> receivedChain = (List<Block>) msg.getPayload();
                    blockchain.replaceChain(receivedChain);
                } catch (Exception e) {
                    logger.error("Error procesando cadena recibida: " + e.getMessage());
                }
                break;

            default:
                logger.warning("Tipo de mensaje desconocido: " + msg.getType());
        }
    }
}
