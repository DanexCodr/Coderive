package cod.interpreter.exception;

@SuppressWarnings("serial")
public class EarlyExitException extends RuntimeException {
    public EarlyExitException() {
      super("Early exit");
    }
  }