package cod.ast.nodes;

import cod.ast.VisitorImpl;
import cod.ast.SourceSpan;
import cod.lexer.Token;
import java.util.List;

public abstract class ASTNode {
    // Source span for error reporting
    public SourceSpan sourceSpan;
    
    // Get the source span (with null-safe default)
    public SourceSpan getSourceSpan() {
        return sourceSpan != null ? sourceSpan : new SourceSpan(1, 1, 1, 1);
    }
    
    // Set the source span
    public void setSourceSpan(SourceSpan span) {
        this.sourceSpan = span;
    }
    
    // Set source span from tokens
    public void setSourceSpanFromTokens(List<Token> tokens) {
        if (tokens != null && !tokens.isEmpty()) {
            this.sourceSpan = SourceSpan.fromTokens(tokens);
        }
    }
    
    // Existing abstract method
    public abstract <T> T accept(VisitorImpl<T> visitor);
}