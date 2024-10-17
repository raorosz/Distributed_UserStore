public class ReadResponse extends Message {
    private static final long serialVersionUID = 1L;
    private String username;
    private String ssn;
    private boolean found;
    
    public ReadResponse(String username, String ssn, boolean found){
        this.username = username;
        this.ssn = ssn;
        this.found = found;
    }

    public String getUsername(){
        return username;
    }

    public String getSsn(){
        return ssn;
    }

    public boolean isFound(){
        return found;
    }
}
