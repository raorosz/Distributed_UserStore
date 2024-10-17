public class TokenHolderUpdate extends Message {
    private static final long serialVersionUID = 1L;
    private int newTokenHolderId;

    public TokenHolderUpdate(int newTokenHolderId) {
        this.newTokenHolderId = newTokenHolderId;
    }

    public int getNewTokenHolderId() {
        return newTokenHolderId;
    }
}


