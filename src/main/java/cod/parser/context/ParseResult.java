package cod.parser.context;

import cod.error.ParseError;

/**
 * Represents the result of a parsing operation, including the new parser state.
 */
public class ParseResult<T> {
    private final T value;
    private final ParserState state;
    private final ParseError error;
    
    private ParseResult(T value, ParserState state, ParseError error) {
        this.value = value;
        this.state = state;
        this.error = error;
    }
    
    public static <T> ParseResult<T> success(T value, ParserState state) {
        return new ParseResult<T>(value, state, null);
    }
    
    public static <T> ParseResult<T> failure(ParseError error, ParserState state) {
        return new ParseResult<T>(null, state, error);
    }
    
    public boolean isSuccess() {
        return error == null;
    }
    
    public boolean isFailure() {
        return error != null;
    }
    
    public T getValue() {
        if (error != null) {
            throw new IllegalStateException("Cannot get value from failed result: " + error);
        }
        return value;
    }
    
    public ParserState getState() {
        return state;
    }
    
    public ParseError getError() {
        return error;
    }
    
}