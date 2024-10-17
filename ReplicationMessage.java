public class ReplicationMessage extends Message {
    private static final long serialVersionUID = 1L;
    private String username;
    private String ssn;

    public ReplicationMessage(String username, String ssn){
        this.username = username;
        this.ssn = ssn;
    }
    
    public String getUsername(){
        return username;
    }

    public String getSsn(){
        return ssn;
    }
}
