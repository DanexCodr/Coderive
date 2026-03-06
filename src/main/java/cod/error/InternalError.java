package cod.error;

@SuppressWarnings("serial")
public class InternalError extends RuntimeException {
    public InternalError(String message) {
        super(message);
    }
    
    public InternalError(String message, Throwable cause) {
        super(message, cause);
    }
}