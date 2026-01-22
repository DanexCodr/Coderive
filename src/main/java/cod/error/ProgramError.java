package cod.error;

@SuppressWarnings("serial")
public class ProgramError extends RuntimeException {
    
    public ProgramError(String message) {
        super(message);
    }
    
}