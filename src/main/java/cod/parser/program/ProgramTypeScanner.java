package cod.parser.program;

import cod.error.ParseError;
import cod.lexer.Token;
import cod.lexer.TokenType;
import static cod.lexer.TokenType.*;
import cod.parser.context.ParserContext;
import cod.syntax.Keyword;
import static cod.syntax.Keyword.*;
import cod.syntax.Symbol;
import static cod.syntax.Symbol.*;

import java.util.List;

/**
 * Dedicated scanner for detecting program type.
 * Self-contained, no dependencies on MainParser.
 */
public class ProgramTypeScanner {
    private final ParserContext ctx;
    
    public ProgramTypeScanner(List<Token> tokens) {
        this.ctx = new ParserContext(tokens);
    }
    
    public ProgramType scan() {
        boolean hasUnit = false;
        boolean hasDirectCode = false;
        boolean hasMethods = false;
        boolean hasClasses = false;
        
        // First check for unit at the beginning
        ctx.save();
        try {
            skipWhitespaceAndComments();
            if (isKeyword(UNIT)) {
                hasUnit = true;
            }
        } finally {
            ctx.restore();
        }
        
        // If we have a unit, check what follows
        if (hasUnit) {
            // Skip unit declaration using expect()
            skipWhitespaceAndComments();
            if (isKeyword(UNIT)) {
                ctx.expect(UNIT);
                if (match(ID)) {
                    parseQualifiedName();
                }
                if (match(LPAREN)) {
                    skipOptionalMainClass();
                }
            }
            
            // Skip imports
            while (isKeyword(USE)) {
                skipUseStatement();
            }
            
            // Check for classes and policies
            while (!match(EOF)) {
                skipWhitespaceAndComments();
                if (isClassStart()) {
                    hasClasses = true;
                    skipTypeDeclaration();
                } else if (isPolicyDeclaration()) {
                    // Handle policy declaration
                    skipPolicyDeclaration();
                } else if (!match(EOF)) {
                    // Anything else is direct code (illegal in module)
                    hasDirectCode = true;
                    ctx.consume();
                }
            }
        } else {
            // No unit - check for scripts or method scripts
            // Skip imports first
            while (isKeyword(USE)) {
                skipUseStatement();
            }
            
            // Check the rest of the file
            while (!match(EOF)) {
                skipWhitespaceAndComments();
                
                if (isMethodDeclarationStart()) {
                    hasMethods = true;
                    skipMethodDeclaration();
                } else if (isClassStart()) {
                    hasClasses = true;
                    skipTypeDeclaration();
                } else if (isPolicyDeclaration()) {
                    // Skip policy declaration in non-module context
                    skipPolicyDeclaration();
                } else if (!match(EOF)) {
                    hasDirectCode = true;
                    skipStatement();
                }
            }
        }
        
        return determineProgramType(hasUnit, hasDirectCode, hasMethods, hasClasses);
    }
    
    // =========================================================================
    // PRIVATE HELPER METHODS (optimized with expect())
    // =========================================================================
    
    private void skipWhitespaceAndComments() {
        while (ctx.hasMore()) {
            Token t = ctx.current();
            if (t.type == WS || t.type == LINE_COMMENT || t.type == BLOCK_COMMENT) {
                ctx.consume();
            } else {
                break;
            }
        }
    }
    
    private boolean isKeyword(Keyword keyword) {
        Token current = ctx.current();
        return current != null && current.type == KEYWORD && is(current.text, keyword);
    }
    
    private boolean is(String a, String b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
    
    private boolean is(String text, Keyword... keywords) {
        for (Keyword kw : keywords) {
            if (is(text, kw.toString())) return true;
        }
        return false;
    }
    
    private boolean match(TokenType type) {
        Token current = ctx.current();
        return current != null && current.type == type;
    }
    
    private boolean match(Symbol symbol) {
        Token current = ctx.current();
        return current != null && current.type == SYMBOL && current.symbol == symbol;
    }
    
    private void parseQualifiedName() {
        if (ctx.current() != null && ctx.current().type == ID) {
            ctx.consume();
            while (ctx.current() != null && ctx.current().symbol == DOT) {
                ctx.consume();
                if (ctx.current() != null && ctx.current().type == ID) {
                    ctx.consume();
                } else {
                    break;
                }
            }
        }
    }
    
    private void skipOptionalMainClass() {
        if (match(LPAREN)) {
            ctx.consume();
            while (!match(RPAREN) && !match(EOF)) {
                ctx.consume();
            }
            if (match(RPAREN)) {
                ctx.consume();
            }
        }
    }
    
    private void skipUseStatement() {
        ctx.expect(USE); // Use expect() instead of custom check
        if (match(LBRACE)) {
            ctx.consume();
            skipUntil(RBRACE);
        } else {
            parseQualifiedName();
        }
    }
    
    private void skipUntil(Symbol symbol) {
        while (!match(EOF) && !match(symbol)) {
            Token t = ctx.current();
            if (t.symbol == LBRACE || t.symbol == LPAREN || t.symbol == LBRACKET) {
                ctx.consume();
                skipUntilMatching(t.symbol);
            } else {
                ctx.consume();
            }
        }
        if (match(symbol)) ctx.consume();
    }
    
    private void skipUntilMatching(Symbol opening) {
        Symbol closing;
        if (opening == LBRACE) closing = RBRACE;
        else if (opening == LPAREN) closing = RPAREN;
        else if (opening == LBRACKET) closing = RBRACKET;
        else return;
        
        skipUntil(closing);
    }
    
    private boolean isClassStart() {
        Token current = ctx.current();
        if (current == null) return false;

        // Check for visibility modifier
        if (current.type == KEYWORD && is(current.text, SHARE, LOCAL)) {
            return true;
        }

        // Check for class name without modifier (must be PascalCase)
        if (current.type != ID) return false;
        
        String name = current.text;
        if (name.length() == 0 || !Character.isUpperCase(name.charAt(0))) {
            return false;
        }
        
        // Look ahead to see if it's a valid class declaration
        ctx.save();
        try {
            ctx.consume(); // Skip class name
            skipWhitespaceAndComments();
            Token next = ctx.current();
            if (next == null) return false;
            
            // Check for inheritance ('is')
            if (next.type == KEYWORD && is(next.text, IS)) {
                ctx.consume(); // skip "is"
                skipWhitespaceAndComments();
                if (!match(ID)) return false;
                ctx.consume(); // skip parent name
                skipWhitespaceAndComments();
                next = ctx.current();
                if (next == null) return false;
            }
            
            // Check for policies ('with')
            while (next.type == KEYWORD && is(next.text, WITH)) {
                ctx.consume(); // skip "with"
                skipWhitespaceAndComments();
                if (!match(ID)) return false;
                ctx.consume(); // skip first policy name
                skipWhitespaceAndComments();
                
                // Skip additional policies separated by commas
                while (ctx.current() != null && ctx.current().symbol == COMMA) {
                    ctx.consume(); // skip comma
                    skipWhitespaceAndComments();
                    if (!match(ID)) return false;
                    ctx.consume(); // skip next policy name
                    skipWhitespaceAndComments();
                }
                
                next = ctx.current();
                if (next == null) return false;
            }
            
            // Must end with opening brace
            return next.symbol == LBRACE;
        } finally {
            ctx.restore();
        }
    }
    
    private boolean isPolicyDeclaration() {
        ctx.save();
        try {
            skipWhitespaceAndComments();

            Token current = ctx.current();
            if (current != null && current.type == KEYWORD && 
                is(current.text, SHARE, LOCAL)) {
                ctx.consume();
                skipWhitespaceAndComments();
            }

            current = ctx.current();
            if (current == null || current.type != KEYWORD || 
                !is(current.text, POLICY)) {
                return false;
            }

            ctx.consume(); // consume POLICY
            skipWhitespaceAndComments();

            return match(ID);
        } finally {
            ctx.restore();
        }
    }
    
    private void skipPolicyDeclaration() {
        // Skip optional visibility modifier
        if (ctx.current() != null && ctx.current().type == KEYWORD && 
            is(ctx.current().text, SHARE, LOCAL)) {
            ctx.consume();
            skipWhitespaceAndComments();
        }
        
        // Skip 'policy' keyword using expect() pattern
        if (ctx.current() != null && ctx.current().type == KEYWORD && 
            is(ctx.current().text, POLICY)) {
            ctx.consume();
            skipWhitespaceAndComments();
        }
        
        // Skip policy name
        if (match(ID)) {
            ctx.consume();
            skipWhitespaceAndComments();
        }
        
        // Skip 'with' and composed policies if present
        while (ctx.current() != null && ctx.current().type == KEYWORD && 
               is(ctx.current().text, WITH)) {
            ctx.consume(); // skip "with"
            skipWhitespaceAndComments();
            parseQualifiedName(); // skip policy name
            skipWhitespaceAndComments();
            
            // Skip additional policies separated by commas
            while (ctx.current() != null && ctx.current().symbol == COMMA) {
                ctx.consume(); // skip comma
                skipWhitespaceAndComments();
                parseQualifiedName(); // skip next policy name
                skipWhitespaceAndComments();
            }
        }
        
        // Skip opening brace
        if (match(LBRACE)) {
            ctx.consume();
        } else {
            // Not a valid policy declaration
            return;
        }
        
        // Skip everything inside the policy (method declarations)
        int braceDepth = 1;
        while (!match(EOF) && braceDepth > 0) {
            Token t = ctx.current();
            if (t.symbol == LBRACE) braceDepth++;
            else if (t.symbol == RBRACE) braceDepth--;
            else if (braceDepth == 1) {
                // At top level of policy, skip policy method declarations
                if (isPolicyMethodDeclarationStart()) {
                    skipPolicyMethodDeclaration();
                    continue;
                }
            }
            ctx.consume();
        }
    }
    
    private boolean isPolicyMethodDeclarationStart() {
        Token current = ctx.current();
        if (current == null) return false;
        
        // Check if it's a valid method name (ID or allowed keyword)
        boolean isValidName = (current.type == ID) || 
                             (current.type == KEYWORD && canKeywordBeMethodName(current.text));
        
        if (!isValidName) return false;
        
        // Look ahead to check for '('
        ctx.save();
        try {
            ctx.consume(); // Skip name
            skipWhitespaceAndComments();
            return ctx.current() != null && ctx.current().symbol == LPAREN;
        } finally {
            ctx.restore();
        }
    }
    
    private boolean canKeywordBeMethodName(String keywordText) {
        return is(keywordText, IN, ALL, ANY);
    }
    
    private void skipPolicyMethodDeclaration() {
        // Skip method name (could be ID or allowed keyword)
        if (ctx.current() != null && 
            (ctx.current().type == ID || 
             (ctx.current().type == KEYWORD && canKeywordBeMethodName(ctx.current().text)))) {
            ctx.consume();
            skipWhitespaceAndComments();
        }
        
        // Skip parameters
        if (match(LPAREN)) {
            ctx.consume();
            skipUntil(RPAREN);
        }
        
        // Skip return slots if present
        if (ctx.current() != null && ctx.current().symbol == DOUBLE_COLON) {
            ctx.consume();
            
            // Skip type reference
            skipTypeReference();
            
            // Skip additional return slots separated by commas
            while (ctx.current() != null && ctx.current().symbol == COMMA) {
                ctx.consume();
                skipTypeReference();
            }
        }
    }
    
    private boolean isMethodDeclarationStart() {
        Token first = ctx.current();
        if (first == null) return false;
        
        if (first.type == KEYWORD) {
            String text = first.text;
            if (is(text, LOCAL, SHARE)) {
                ctx.save();
                try {
                    ctx.consume(); // skip local/share
                    skipWhitespaceAndComments();
                    Token second = ctx.current();
                    if (second == null || second.type != ID) return false;
                    ctx.consume(); // skip method name
                    skipWhitespaceAndComments();
                    Token third = ctx.current();
                    return third != null && third.symbol == LPAREN;
                } finally {
                    ctx.restore();
                }
            }
        }
        return false;
    }
    
    private void skipTypeDeclaration() {
        // Skip optional visibility modifier
        if (ctx.current() != null && ctx.current().type == KEYWORD && 
            is(ctx.current().text, SHARE, LOCAL)) {
            ctx.consume();
            skipWhitespaceAndComments();
        }
        
        // Skip type name
        if (match(ID)) {
            ctx.consume();
            skipWhitespaceAndComments();
        }
        
        // Skip 'is' and base type if present
        if (ctx.current() != null && ctx.current().type == KEYWORD && 
            is(ctx.current().text, IS)) {
            ctx.consume(); // skip "is"
            skipWhitespaceAndComments();
            parseQualifiedName();
            skipWhitespaceAndComments();
        }
        
        // Skip 'with' and policies if present
        while (ctx.current() != null && ctx.current().type == KEYWORD && 
               is(ctx.current().text, WITH)) {
            ctx.consume(); // skip "with"
            skipWhitespaceAndComments();
            parseQualifiedName(); // skip policy name
            skipWhitespaceAndComments();
            
            // Skip additional policies separated by commas
            while (ctx.current() != null && ctx.current().symbol == COMMA) {
                ctx.consume(); // skip comma
                skipWhitespaceAndComments();
                parseQualifiedName(); // skip next policy name
                skipWhitespaceAndComments();
            }
        }
        
        // Skip opening brace
        if (match(LBRACE)) {
            ctx.consume();
        } else {
            // Not a valid class declaration
            return;
        }
        
        // Skip everything until matching closing brace
        int braceDepth = 1;
        while (!match(EOF) && braceDepth > 0) {
            Token t = ctx.current();
            if (t.symbol == LBRACE) braceDepth++;
            else if (t.symbol == RBRACE) braceDepth--;
            ctx.consume();
        }
    }
    
    private void skipMethodDeclaration() {
        // Check for builtin modifier
        boolean isBuiltin = false;
        if (ctx.current() != null && ctx.current().type == KEYWORD && 
            is(ctx.current().text, BUILTIN)) {
            isBuiltin = true;
            ctx.consume(); // skip "builtin"
        }
        
        // Skip modifier (share/local)
        if (ctx.current() != null && ctx.current().type == KEYWORD && 
            is(ctx.current().text, SHARE, LOCAL)) {
            ctx.consume();
        }
        
        // Skip method name
        if (match(ID)) ctx.consume();
        
        // Skip parameters
        if (match(LPAREN)) {
            ctx.consume();
            skipUntil(RPAREN);
        }
        
        // Skip return slots if present
        if (match(DOUBLE_COLON)) {
            ctx.consume();
            skipSlotContract();
        }
        
        // Builtin methods have no body
        if (isBuiltin) {
            return;
        }
        
        // Handle both ~> and { syntax
        if (match(TILDE_ARROW)) {
            ctx.consume();
            skipSlotAssignment();
            while (ctx.current() != null && ctx.current().symbol == COMMA) {
                ctx.consume();
                skipSlotAssignment();
            }
        } else if (match(LBRACE)) {
            ctx.consume();
            skipUntil(RBRACE);
        }
    }
    
    private void skipSlotAssignment() {
        // Skip optional slot name and colon
        if (match(ID)) {
            Token next = ctx.peek(1);
            if (next != null && next.symbol == COLON) {
                ctx.consume(); // skip name
                ctx.consume(); // skip colon
            }
        }
        // Skip the expression
        skipExpression();
    }
    
    private void skipSlotContract() {
        do {
            // Skip name: type or just type
            if (match(ID) && ctx.peek(1) != null && ctx.peek(1).symbol == COLON) {
                ctx.consume(); // name
                ctx.consume(); // colon
            }
            // Skip type
            skipTypeReference();
        } while (ctx.current() != null && ctx.current().symbol == COMMA);
    }
    
    private void skipStatement() {
        Token current = ctx.current();
        if (current == null) return;
        
        if (current.type == KEYWORD) {
            String text = current.text;
            if (is(text, IF)) {
                skipIfStatement();
            } else if (is(text, FOR)) {
                skipForStatement();
            } else if (is(text, EXIT)) {
                ctx.consume();
            } else if (is(text, SHARE, LOCAL)) {
                // Check if this is a method declaration
                ctx.save();
                try {
                    ctx.consume(); // skip share/local
                    skipWhitespaceAndComments();
                    if (match(ID)) {
                        ctx.consume(); // skip name
                        skipWhitespaceAndComments();
                        if (match(LPAREN)) {
                            // It's a method declaration
                            ctx.restore(); // go back
                            skipMethodDeclaration();
                            return;
                        }
                    }
                } finally {
                    // Already handled consumption
                }
            } else {
                ctx.consume();
            }
        } else if (current.type == ID) {
            // Skip until statement end
            skipUntilStatementEnd();
        } else {
            ctx.consume();
        }
    }
    
    private void skipForStatement() {
        // Use ctx.tryConsume() pattern for optional parts
        ctx.expect(FOR); // FOR is required
        
        if (match(ID)) ctx.consume();
        
        if (ctx.current() != null && ctx.current().type == KEYWORD && 
            is(ctx.current().text, BY)) {
            ctx.consume(); // skip "by"
            skipExpression();
        }
        
        if (ctx.current() != null && ctx.current().type == KEYWORD && 
            is(ctx.current().text, IN)) {
            ctx.consume(); // skip "in"
        }
        
        skipExpression();
        
        if (ctx.current() != null && ctx.current().type == KEYWORD && 
            is(ctx.current().text, TO)) {
            ctx.consume(); // skip "to"
        } else {
            return;
        }
        
        skipExpression();
        
        if (match(LBRACE)) {
            ctx.consume();
            skipUntil(RBRACE);
        } else {
            skipStatement();
        }
    }
    
    private void skipIfStatement() {
        ctx.expect(IF); // IF is required
        skipExpression();
        
        // Skip then block
        if (match(LBRACE)) {
            ctx.consume();
            skipUntil(RBRACE);
        } else {
            skipStatement();
        }
        
        // Skip else/elif if present
        while (ctx.current() != null && ctx.current().type == KEYWORD && 
               is(ctx.current().text, ELIF, ELSE)) {
            ctx.consume(); // skip elif/else
            
            if (ctx.current() != null && ctx.current().type == KEYWORD && 
                is(ctx.current().text, IF)) {
                ctx.consume(); // skip "if" in "else if"
                skipExpression();
            }
            
            if (match(LBRACE)) {
                ctx.consume();
                skipUntil(RBRACE);
            } else {
                skipStatement();
            }
        }
    }
    
    private void skipExpression() {
        int braceDepth = 0;
        int parenDepth = 0;
        int bracketDepth = 0;

        while (!match(EOF)) {
            Token t = ctx.current();
            
            if (t.symbol == LBRACE) braceDepth++;
            else if (t.symbol == RBRACE) braceDepth--;
            else if (t.symbol == LPAREN) parenDepth++;
            else if (t.symbol == RPAREN) parenDepth--;
            else if (t.symbol == LBRACKET) bracketDepth++;
            else if (t.symbol == RBRACKET) bracketDepth--;

            if (braceDepth < 0 || parenDepth < 0 || bracketDepth < 0) {
                return;
            }

            if (braceDepth == 0 && parenDepth == 0 && bracketDepth == 0) {
                if (t.symbol == COMMA) {
                    return;
                }
                
                if (t.type == KEYWORD) {
                    String text = t.text;
                    if (is(text, IF, FOR, EXIT, ELSE, ELIF, SHARE, LOCAL, UNIT)) {
                        return;
                    }
                }
            }

            ctx.consume();
        }
    }
    
    private void skipUntilStatementEnd() {
        while (!match(EOF)) {
            Token t = ctx.current();
            
            if (t.symbol == RBRACE ||
                (t.type == KEYWORD && 
                 is(t.text, ELSE, ELIF, IF, FOR, EXIT))) {
                break;
            }
            
            if (t.symbol == LBRACE || t.symbol == LPAREN || t.symbol == LBRACKET) {
                ctx.consume();
                skipUntilMatching(t.symbol);
            } else {
                ctx.consume();
            }
        }
    }
    
    private void skipTypeReference() {
        if (match(LBRACKET)) {
            ctx.consume(); // consume '['
            if (!match(RBRACKET)) {
                skipTypeReference();
            }
            // FIX: Check and consume ']'
            if (match(RBRACKET)) {
                ctx.consume();
            }
        } else if (match(LPAREN)) {
            ctx.consume(); // consume '('
            skipTypeReference();
            while (ctx.current() != null && ctx.current().symbol == COMMA) {
                ctx.consume(); // consume ','
                skipTypeReference();
            }
            // FIX: Check and consume ')'
            if (match(RPAREN)) {
                ctx.consume();
            }
        } else if (ctx.current() != null && 
                   (isTypeKeyword(ctx.current().text) || ctx.current().type == ID)) {
            ctx.consume();
        }
        
        // Skip union
        while (ctx.current() != null && ctx.current().symbol == PIPE) {
            ctx.consume(); // consume '|'
            skipTypeReference();
        }
    }
    
    private boolean isTypeKeyword(String text) {
        return is(text, INT, TEXT, FLOAT, BOOL, TYPE);
    }
    
    private ProgramType determineProgramType(boolean hasUnit, boolean hasDirectCode, 
                                           boolean hasMethods, boolean hasClasses) {
        // Copy the exact same logic from MainParser
        if (hasUnit) {
            if (hasDirectCode) {
                throw new ParseError("[MODULE] Modules cannot have direct code outside classes.", 
                    ctx.getLine(), ctx.getColumn());
            }
            if (hasMethods && !hasClasses) {
                throw new ParseError("[MODULE] Modules cannot have methods outside classes.", 
                    ctx.getLine(), ctx.getColumn());
            }
            return ProgramType.MODULE;
        }
        
        if (hasDirectCode) {
            if (hasMethods) {
                throw new ParseError("[SCRIPT] Cannot mix direct code and method declarations.\n" +
                                   "Either:\n" +
                                   "1. Remove methods and keep as script, OR\n" +
                                   "2. Remove direct code and make it a method script, OR\n" +
                                   "3. Add 'unit' and classes to make it a module.", 
                    ctx.getLine(), ctx.getColumn());
            }
            if (hasClasses) {
                throw new ParseError("[SCRIPT] Scripts cannot contain class declarations.", 
                    ctx.getLine(), ctx.getColumn());
            }
            return ProgramType.SCRIPT;
        }
        
        if (hasMethods) {
            if (hasClasses) {
                throw new ParseError("[METHOD_SCRIPT] Method scripts cannot contain class declarations.", 
                    ctx.getLine(), ctx.getColumn());
            }
            return ProgramType.METHOD_SCRIPT;
        }
        
        if (hasClasses) {
            throw new ParseError("[UNKNOWN] Classes require 'unit' declaration.\n" +
                               "Add: unit namespace.name\n" +
                               "Before your class definitions.", 
                ctx.getLine(), ctx.getColumn());
        }
        
        throw new ParseError("[UNKNOWN] Empty file or unrecognized structure", 1, 1);
    }
}