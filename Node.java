import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Node {
    private int nodeId;
    private boolean isPrimary;
    private String host;
    private int port;
    private Map<String, String> userStore;
    private List<NodeInfo> otherNodes;
    private ConcurrentLinkedQueue<Integer> requestQueue;
    private volatile int tokenHolderId;
    private volatile boolean isTokenRequested;
    private volatile boolean hasToken;
    private ExecutorService connectionPool;

    // Constructor
    public Node(int nodeId, boolean isPrimary, String host, int port, List<NodeInfo> otherNodes) {
        this.nodeId = nodeId;
        this.isPrimary = isPrimary;
        this.host = host;
        this.port = port;
        this.otherNodes = otherNodes;
        this.userStore = Collections.synchronizedMap(new HashMap<>());
        this.requestQueue = new ConcurrentLinkedQueue<>();
        this.isTokenRequested = false;
        this.hasToken = isPrimary; // Primary starts with the token
        this.tokenHolderId = isPrimary ? nodeId : -1;
        this.connectionPool = Executors.newFixedThreadPool(10); // Limit to 10 concurrent connections
        System.out.println("Node " + nodeId + " initialized. isPrimary: " + isPrimary + ", hasToken: " + hasToken);
    }

    // Start the node server
    public void start() {
        System.out.println("Node " + nodeId + " starting...");
        new Thread(() -> listen()).start();
    }

    private void listen() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Node " + nodeId + " listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Node " + nodeId + " accepted connection from " + clientSocket.getRemoteSocketAddress());
                connectionPool.submit(() -> handleConnection(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

  private void handleConnection(Socket socket) {
    try (
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
    ) {
        System.out.println("Node " + nodeId + " handling connection. Streams established.");
        Message msg = (Message) in.readObject();
        System.out.println("Node " + nodeId + " received message: " + msg.getClass().getSimpleName());
        
        if (msg instanceof ReadRequest) {
            handleReadRequest((ReadRequest) msg, out);
        } else if (msg instanceof WriteRequest) {
            handleWriteRequest((WriteRequest) msg, out);
        } else if (msg instanceof TokenRequest) {
            handleTokenRequest((TokenRequest) msg);
        } else if (msg instanceof TokenGrant) {
            handleTokenGrant();
        } else if (msg instanceof ReplicationMessage) {
            handleReplicationMessage((ReplicationMessage) msg);
        } else if (msg instanceof TokenHolderUpdate) {
            handleTokenHolderUpdate((TokenHolderUpdate) msg);
        } else {
            System.err.println("Unknown message type received: " + msg.getClass());
        }
    } catch (IOException | ClassNotFoundException e) {
        System.err.println("Exception in handleConnection for node " + nodeId);
        e.printStackTrace();
    } finally {
        try {
            socket.close();  // Ensure socket is closed after processing
            System.out.println("Node " + nodeId + " closed socket.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

    

    private void handleReadRequest(ReadRequest req, ObjectOutputStream out) throws IOException {
        String ssn = userStore.get(req.getUsername());
        boolean found = ssn != null;
        ReadResponse resp = new ReadResponse(req.getUsername(), ssn, found);
        out.writeObject(resp);
        System.out.println("Node " + nodeId + " handled ReadRequest for user: " + req.getUsername());
    }

    private void handleWriteRequest(WriteRequest req, ObjectOutputStream out) throws IOException {
        System.out.println("Node " + nodeId + " handling WriteRequest for user: " + req.getUsername());
        if (isPrimary) {
            // Request the token if not held
            if (!hasToken) {
                System.out.println("Node " + nodeId + " does not have the token. Requesting token...");
                requestToken();
                // Wait until the token is granted
                synchronized (this) {
                    while (!hasToken) {
                        try {
                            wait(); // Wait until notified that the token has been granted
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                System.out.println("Node " + nodeId + " has received the token.");
            }
    
            // Perform the write operation
            performWrite(req);
            // Replicate to backups
            replicateToBackups(req);
            // Release the token
            releaseToken();
            // Acknowledge to client
            out.writeObject(new Acknowledgment("Write operation successful."));
            System.out.println("Node " + nodeId + " acknowledged WriteRequest for user: " + req.getUsername());
        } else {
            System.out.println("Node " + nodeId + " is forwarding WriteRequest to the primary node.");
            // Backup node: forward the write request to the primary node
            NodeInfo primaryInfo = getPrimaryInfo();
            try (Socket primarySocket = new Socket(primaryInfo.getHost(), primaryInfo.getPort());
                 ObjectOutputStream primaryOut = new ObjectOutputStream(primarySocket.getOutputStream());
                 ObjectInputStream primaryIn = new ObjectInputStream(primarySocket.getInputStream())) {
                primaryOut.writeObject(req);
                Message resp = (Message) primaryIn.readObject();
                out.writeObject(resp);  // Send the primary's response back to the client
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                out.writeObject(new ErrorMessage("Failed to forward write request to primary."));
            }
        }
    }
    

    private void performWrite(WriteRequest req) {
        userStore.put(req.getUsername(), req.getSsn());
        System.out.println("Node " + nodeId + " updated user: " + req.getUsername());
    }

    private void replicateToBackups(WriteRequest req) {
        ReplicationMessage repMsg = new ReplicationMessage(req.getUsername(), req.getSsn());
        for (NodeInfo node : otherNodes) {
            if (node.getId() != nodeId) { // Skip self (primary)
                System.out.println("Node " + nodeId + " sending replication message to Node " + node.getId());
                sendMessage(node.getHost(), node.getPort(), repMsg);
            }
        }
    }
    

    private void handleReplicationMessage(ReplicationMessage msg) {
        try {
            System.out.println("Node " + nodeId + " is replicating user: " + msg.getUsername());
            userStore.put(msg.getUsername(), msg.getSsn());
            System.out.println("Node " + nodeId + " replicated user: " + msg.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    private NodeInfo getPrimaryInfo() {
        for (NodeInfo node : otherNodes) {
            if (node.isPrimary()) {
                return node;
            }
        }
        // If self is primary
        return new NodeInfo(nodeId, host, port, isPrimary);
    }

    private void handleTokenRequest(TokenRequest req) {
        System.out.println("Node " + nodeId + " received TokenRequest from Node " + req.getRequesterId());
        if (hasToken && !isTokenRequested) {
            System.out.println("Node " + nodeId + " is granting the token to Node " + req.getRequesterId());
            // Grant token
            sendMessage(getNodeInfoById(req.getRequesterId()).getHost(),
                        getNodeInfoById(req.getRequesterId()).getPort(),
                        new TokenGrant());
            hasToken = false;
            tokenHolderId = req.getRequesterId();
            broadcastTokenHolderUpdate(tokenHolderId);
        } else {
            // Add to queue
            if (!requestQueue.contains(req.getRequesterId())) {
                requestQueue.add(req.getRequesterId());
            }
        }
    }

    private void handleTokenGrant() {
        hasToken = true;
        isTokenRequested = false;
        System.out.println("Node " + nodeId + " received the token.");
        synchronized (this) {
            notifyAll(); // Notify any waiting threads that the token has been granted
        }
    }

    private void handleTokenRelease() {
        if (!requestQueue.isEmpty()) {
            int nextHolder = requestQueue.poll();
            sendMessage(getNodeInfoById(nextHolder).getHost(),
                        getNodeInfoById(nextHolder).getPort(),
                        new TokenGrant());
            hasToken = false;
            tokenHolderId = nextHolder;
            broadcastTokenHolderUpdate(tokenHolderId);
        } else {
            // No pending requests; keep the token
            hasToken = true;
            tokenHolderId = nodeId;
            // No need to broadcast if the token holder hasn't changed
        }
    }

    private void requestToken() {
        if (!isTokenRequested) {
            isTokenRequested = true;
            System.out.println("Node " + nodeId + " requesting token from Node " + tokenHolderId);
            if (tokenHolderId != nodeId && tokenHolderId != -1) {
                // Send token request to current token holder
                NodeInfo tokenHolderInfo = getNodeInfoById(tokenHolderId);
                sendMessage(tokenHolderInfo.getHost(), tokenHolderInfo.getPort(), new TokenRequest(nodeId));
            } else {
                // Token holder ID is unknown or self, attempt to recover token
                System.out.println("Token holder unknown or self; assuming token.");
                hasToken = true;
                tokenHolderId = nodeId;
                broadcastTokenHolderUpdate(tokenHolderId);
                synchronized (this) {
                    notifyAll(); // Notify any waiting threads that the token has been granted
                }
            }
        }
    }

    private void releaseToken() {
        // Send token release message
        System.out.println("Node " + nodeId + " releasing the token.");
        handleTokenRelease();
    }

    private void broadcastTokenHolderUpdate(int newTokenHolderId) {
        // Broadcast to all nodes
        TokenHolderUpdate updateMsg = new TokenHolderUpdate(newTokenHolderId);
        for (NodeInfo node : otherNodes) {
            if (node.getId() != nodeId) {
                sendMessage(node.getHost(), node.getPort(), updateMsg);
            }
        }
    }

    private void handleTokenHolderUpdate(TokenHolderUpdate msg) {
        tokenHolderId = msg.getNewTokenHolderId();
        System.out.println("Node " + nodeId + " updated token holder to Node " + tokenHolderId);
    }

    private NodeInfo getNodeInfoById(int id) {
        for (NodeInfo node : otherNodes) {
            if (node.getId() == id) return node;
        }
        // If not found, assume self
        return new NodeInfo(nodeId, host, port, isPrimary);
    }

    private void sendMessage(String host, int port, Message msg) {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            out.writeObject(msg);
            out.flush(); // Ensure all data is sent
            // If expecting a response, read it here
            // Message response = (Message) in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

}