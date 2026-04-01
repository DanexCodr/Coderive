package cod.lexer;

import cod.syntax.Keyword;
import cod.error.LexError;
import java.util.*;

public class IdentifierLexer {

    private final MainLexer lexer;
    private final List<String> extractedIdentifiers;
    private final Set<String> keywords;
    
    // Perfect hash for O(1) keyword detection
    private static final int[] KEYWORD_HASH = new int[512];
    private static final Keyword[] KEYWORD_BY_HASH = new Keyword[512];
    
    static {
        for (Keyword kw : Keyword.values()) {
            String name = kw.toString();
            int hash = perfectHash(name);
            
            // Check for collision during construction
            if (KEYWORD_HASH[hash] == 1) {
                Keyword existing = KEYWORD_BY_HASH[hash];
                throw new LexError(
                    "FATAL: Hash collision in keyword lexer!\n" +
                    "  Keyword '" + name + "' hashes to " + hash + "\n" +
                    "  Keyword '" + existing.toString() + "' already uses this hash.\n"
                );
            }
            
            KEYWORD_HASH[hash] = 1;
            KEYWORD_BY_HASH[hash] = kw;
        }
    }
    
    private static int perfectHash(String s) {
        int len = s.length();
        if (len == 0) return 0;
        // Use combination of length, first char, last char, and middle char
        int hash = len * 31;
        hash = hash * 31 + s.charAt(0) * 17;
        hash = hash * 31 + s.charAt(len - 1) * 13;
        if (len > 2) {
            hash = hash * 31 + s.charAt(len / 2) * 7;
        }
        // Add rolling hash for all characters to ensure uniqueness
        int rolling = 0;
        for (int i = 0; i < len; i++) {
            rolling = (rolling << 5) - rolling + s.charAt(i);
        }
        hash = hash ^ (rolling & 0x1FF);
        return hash & 511;
    }
    
    private static int perfectHash(char[] source, int start, int length) {
        if (length == 0) return 0;
        int hash = length * 31;
        hash = hash * 31 + source[start] * 17;
        hash = hash * 31 + source[start + length - 1] * 13;
        if (length > 2) {
            hash = hash * 31 + source[start + length / 2] * 7;
        }
        int rolling = 0;
        for (int i = 0; i < length; i++) {
            rolling = (rolling << 5) - rolling + source[start + i];
        }
        hash = hash ^ (rolling & 0x1FF);
        return hash & 511;
    }

    public IdentifierLexer(MainLexer lexer) {
        this.lexer = lexer;
        this.extractedIdentifiers = new ArrayList<String>();
        this.keywords = new HashSet<String>();
        
        for (Keyword keyword : Keyword.values()) {
            keywords.add(keyword.toString());
        }
    }

    public Token scan() {
        char c = lexer.peek();
        if (Character.isLetter(c) || c == '_') {
            return readIdentifierOrKeyword();
        }
        return null;
    }

    private Token readIdentifierOrKeyword() {
        int startLine = lexer.line;
        int startCol = lexer.column;
        int startPos = lexer.getPosition();
        int length = 0;
        
        // Count characters without allocating
        while (lexer.getPosition() < lexer.getInput().length) {
            char c = lexer.peek();
            if (Character.isLetterOrDigit(c) || c == '_') {
                lexer.consume();
                length++;
            } else {
                break;
            }
        }
        
        char[] source = lexer.getInputArray();
        
        // O(1) keyword detection using perfect hash
        int hash = perfectHash(source, startPos, length);
        if (KEYWORD_HASH[hash] == 1) {
            Keyword keyword = KEYWORD_BY_HASH[hash];
            // Verify exact match (no false positives)
            String kwName = keyword.toString();
            if (matchesExactly(source, startPos, length, kwName)) {
                return Token.createKeyword(source, startPos, length, 
                                          startLine, startCol, keyword);
            }
        }
        
        // Not a keyword - return identifier
        String identifierText = new String(source, startPos, length);
        extractedIdentifiers.add(identifierText);
        return Token.createIdentifier(source, startPos, length, startLine, startCol);
    }
    
    private boolean matchesExactly(char[] source, int start, int length, String keyword) {
        if (length != keyword.length()) return false;
        for (int i = 0; i < length; i++) {
            if (source[start + i] != keyword.charAt(i)) return false;
        }
        return true;
    }

    public List<String> extractAllIdentifiers() {
        extractedIdentifiers.clear();
        int savedPos = lexer.getPosition();
        int savedLine = lexer.line;
        int savedCol = lexer.column;

        lexer.setPosition(0);
        lexer.line = 1;
        lexer.column = 1;

        while (lexer.getPosition() < lexer.getInput().length) {
            Token token = scan();
            if (token == null) {
                lexer.consume();
            }
        }

        lexer.setPosition(savedPos);
        lexer.line = savedLine;
        lexer.column = savedCol;

        return new ArrayList<String>(extractedIdentifiers);
    }

    public boolean isKeyword(String text) {
        return keywords.contains(text);
    }
}