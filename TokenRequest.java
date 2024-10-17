public class TokenRequest extends Message {
    private int requesterId;

    public TokenRequest(int requesterId){
        this.requesterId = requesterId;
    }
    
    public int getRequesterId() {
        return requesterId;
    }
}
