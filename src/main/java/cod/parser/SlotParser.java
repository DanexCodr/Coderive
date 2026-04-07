package cod.parser;

import cod.ast.ASTFactory;
import cod.ast.node.*;
import cod.lexer.Token;
import static cod.lexer.TokenType.*;
import static cod.syntax.Symbol.*;
import java.util.ArrayList;
import java.util.List;

public class SlotParser {
    
    private final BaseParser parser;
    private final ExpressionParser exprParser;
    
    // Constructor for when called from ExpressionParser
    public SlotParser(ExpressionParser parser) {
        this.parser = parser;
        this.exprParser = parser;
    }
    
    // Constructor for when called from StatementParser
    public SlotParser(StatementParser parser) {
        this.parser = parser;
        this.exprParser = parser.expressionParser;
    }
    
    // Constructor for when called from DeclarationParser
    public SlotParser(DeclarationParser parser) {
        this.parser = parser;
        this.exprParser = parser.getStatementParser().expressionParser;
    }
    
    /**
     * Parse slot contract: :: name: type, name: type
     */
    public List<Slot> parseSlotContract() {
        parser.expect(DOUBLE_COLON);
        
        List<Slot> slots = new ArrayList<Slot>();
        
        boolean firstSlot = true;
        boolean isNamedMode = false;
        int index = 0;
        
        do {
            String name;
            String type;
            Token nameToken = null;
            
            if (firstSlot) {
                if (parser.is(parser.now(), ID)) {
                    isNamedMode = true;
                    nameToken = parser.now();
                    name = parser.expect(ID).getText();
                    parser.expect(COLON);
                    type = parser.parseTypeReference();
                } else {
                    isNamedMode = false;
                    name = String.valueOf(index);
                    type = parser.parseTypeReference();
                }
                firstSlot = false;
            } else {
                if (isNamedMode) {
                    if (!parser.is(parser.now(), ID)) {
                        throw parser.error("Mixed slot declaration styles not allowed. Expected name for slot.");
                    }
                    nameToken = parser.now();
                    name = parser.expect(ID).getText();
                    parser.expect(COLON);
                    type = parser.parseTypeReference();
                } else {
                    if (parser.is(parser.now(), ID)) {
                        throw parser.error("Mixed slot declaration styles not allowed. Found name '" +
                            parser.now().getText() + "' in unnamed slot list.");
                    }
                    name = String.valueOf(index);
                    type = parser.parseTypeReference();
                }
            }
            
            slots.add(ASTFactory.createSlot(type, name, nameToken));
            index++;
            
        } while (parser.consume(COMMA));
        
        return slots;
    }
    
    /**
     * Parse slot assignments: ~> name: expr, expr
     */
    public List<SlotAssignment> parseSlotAssignments() {
        List<SlotAssignment> assignments = new ArrayList<SlotAssignment>();
        
        // Parse first assignment
        assignments.add(parseSingleSlotAssignment());
        
        // Parse additional assignments after commas
        while (parser.consume(COMMA)) {
            assignments.add(parseSingleSlotAssignment());
        }
        
        return assignments;
    }
    
    /**
     * Parse a single slot assignment
     */
    public SlotAssignment parseSingleSlotAssignment() {
        String slotName = null;
        Expr value;
        Token colonToken = null;
        
        if (parser.is(parser.now(), ID)) {
            Token afterId = parser.next();
            if (parser.is(afterId, COLON)) {
                slotName = parser.expect(ID).getText();
                colonToken = parser.now();
                parser.expect(COLON);
                value = exprParser.parseExpr();
            } else {
                slotName = null;
                value = exprParser.parseExpr();
            }
        } else {
            slotName = null;
            value = exprParser.parseExpr();
        }
        
        return ASTFactory.createSlotAsmt(slotName, value, colonToken);
    }
    
    /**
     * Parse slot assignments and wrap appropriately
     */
    public Stmt parseSlotAssignmentsAsStmt(Token tildeArrowToken) {
        List<SlotAssignment> assignments = parseSlotAssignments();
        
        if (assignments.size() == 1) {
            return assignments.get(0);
        }
        return ASTFactory.createMultipleSlotAsmt(assignments, tildeArrowToken);
    }
    
    /**
     * Validate that assignments match contract
     */
    public void validateSlotCount(List<Slot> contract, List<SlotAssignment> assignments, Token errorToken) {
        if (contract == null || contract.isEmpty()) {
            return;
        }
        
        int contractSize = contract.size();
        int assignmentSize = assignments.size();
        
        if (contractSize != assignmentSize) {
            throw parser.error(
                "Slot contract expects " + contractSize + " return value(s), but " +
                assignmentSize + " provided in ~> assignment",
                errorToken
            );
        }
    }
}
