package cod.interpreter.exception;

public class EarlyExitException extends RuntimeException {
    public EarlyExitException() {
      super("Early exit");
    }
  }
