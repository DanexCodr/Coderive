package cod.interpreter.exception;

@SuppressWarnings("serial")
public class SkipIterationException extends RuntimeException {
    public SkipIterationException() {
      super("Skip iteration");
    }
  }