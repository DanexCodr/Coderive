package cod.interpreter.exception;

public class SkipIterationException extends RuntimeException {
    public SkipIterationException() {
      super("Skip iteration");
    }
  }