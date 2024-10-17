public class WriteRequest extends Message {
    private String username;
    private String ssn;

    public WriteRequest(String username, String ssn) {
        this.username = username;
        this.ssn = ssn;
    }

    public String getUsername() {
        return username;
    }

    public String getSsn() {
        return ssn;
    }
}
