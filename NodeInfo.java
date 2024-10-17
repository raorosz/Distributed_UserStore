public class NodeInfo {
    private int id;
    private String host;
    private int port;
    private boolean isPrimary;

    public NodeInfo(int id, String host, int port, boolean isPrimary) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.isPrimary = isPrimary;
    }

    public int getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isPrimary() {
        return isPrimary;
    }
}

