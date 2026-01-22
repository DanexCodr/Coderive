package cod.interpreter.exception;

@SuppressWarnings("serial")
public class BreakLoopException extends RuntimeException {
    public BreakLoopException() {
        super("Break loop");
    }
  }