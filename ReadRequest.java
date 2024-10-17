public class ReadRequest extends Message {
    private static final long serialVersionUID = 1L;
    private String username;

    public ReadRequest(String username) {
        this.username = username;
    }
    
    public String getUsername() {
        return username;
    }
}
