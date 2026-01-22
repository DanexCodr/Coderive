package cod.error;

@SuppressWarnings("serial")
public class LexError extends RuntimeException {

    public LexError(String message) {
        super(message);
    }
}
