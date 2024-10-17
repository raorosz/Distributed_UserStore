import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Try to read node ID from environment variable, fallback to default if not present
        String nodeIdEnv = System.getenv("NODE_ID");
        int resolvedNodeId = nodeIdEnv != null ? Integer.parseInt(nodeIdEnv) : 1;  // Default to 1 if NODE_ID is not set

        // Define node configurations
        List<NodeInfo> nodes = new ArrayList<>();
        nodes.add(new NodeInfo(1, "node1", 5001, true)); // Primary
        nodes.add(new NodeInfo(2, "node2", 5002, false));
        nodes.add(new NodeInfo(3, "node3", 5003, false));
        nodes.add(new NodeInfo(4, "node4", 5004, false));

        System.out.println("Node configuration complete. Starting node with ID: " + resolvedNodeId);

        NodeInfo myNodeInfo = null;
        List<NodeInfo> otherNodes = new ArrayList<>();

        // Determine which node to start
        for (NodeInfo nodeInfo : nodes) {
            if (nodeInfo.getId() == resolvedNodeId) {
                myNodeInfo = nodeInfo;
            } else {
                otherNodes.add(new NodeInfo(nodeInfo.getId(), nodeInfo.getHost(), nodeInfo.getPort(), nodeInfo.isPrimary()));
            }
        }

        if (myNodeInfo != null) {
            Node node = new Node(myNodeInfo.getId(), myNodeInfo.isPrimary(), myNodeInfo.getHost(), myNodeInfo.getPort(), otherNodes);
            node.start();
            System.out.println("Node " + myNodeInfo.getId() + " started successfully.");
        } else {
            System.err.println("Node ID not found in the configuration.");
        }
    }
}
