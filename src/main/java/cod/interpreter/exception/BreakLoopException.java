package cod.interpreter.exception;

public class BreakLoopException extends RuntimeException {
    public BreakLoopException() {
        super("Break loop");
    }
  }