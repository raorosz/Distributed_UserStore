public class ErrorMessage extends Message {
    private static final long serialVersionUID = 1L;
    private String error;

    public ErrorMessage(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
