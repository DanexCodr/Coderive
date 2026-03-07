package cod.parser;

import cod.ast.ASTFactory;
import cod.ast.FlatAST;
import cod.lexer.Token;
import static cod.lexer.TokenType.*;
import static cod.syntax.Symbol.*;
import java.util.ArrayList;
import java.util.List;

public class SlotParser {

    private final BaseParser parser;
    private final ExpressionParser exprParser;
    private final ASTFactory factory;

    public SlotParser(ExpressionParser parser) {
        this.parser = parser;
        this.exprParser = parser;
        this.factory = parser.getFactory();
    }

    public SlotParser(StatementParser parser) {
        this.parser = parser;
        this.exprParser = parser.expressionParser;
        this.factory = parser.getFactory();
    }

    public SlotParser(DeclarationParser parser) {
        this.parser = parser;
        this.exprParser = parser.getStatementParser().expressionParser;
        this.factory = parser.getFactory();
    }

    public List<Integer> parseSlotContract() {
        parser.expect(DOUBLE_COLON);

        List<Integer> slots = new ArrayList<Integer>();

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
                    name = parser.expect(ID).text;
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
                    name = parser.expect(ID).text;
                    parser.expect(COLON);
                    type = parser.parseTypeReference();
                } else {
                    if (parser.is(parser.now(), ID)) {
                        throw parser.error("Mixed slot declaration styles not allowed. Found name '" +
                            parser.now().text + "' in unnamed slot list.");
                    }
                    name = String.valueOf(index);
                    type = parser.parseTypeReference();
                }
            }

            slots.add(factory.createSlot(type, name, nameToken));
            index++;

        } while (parser.consume(COMMA));

        return slots;
    }

    public List<Integer> parseSlotAssignments() {
        List<Integer> assignments = new ArrayList<Integer>();
        assignments.add(parseSingleSlotAssignment());
        while (parser.consume(COMMA)) {
            assignments.add(parseSingleSlotAssignment());
        }
        return assignments;
    }

    public int parseSingleSlotAssignment() {
        String slotName = null;
        int valueId;
        Token colonToken = null;

        if (parser.is(parser.now(), ID)) {
            Token afterId = parser.next();
            if (parser.is(afterId, COLON)) {
                slotName = parser.expect(ID).text;
                colonToken = parser.now();
                parser.expect(COLON);
                valueId = exprParser.parseExpr();
            } else {
                valueId = exprParser.parseExpr();
            }
        } else {
            valueId = exprParser.parseExpr();
        }

        return factory.createSlotAsmt(slotName, valueId, colonToken);
    }

    public int parseSlotAssignmentsAsStmt(Token tildeArrowToken) {
        List<Integer> assignments = parseSlotAssignments();

        if (assignments.size() == 1) {
            return assignments.get(0);
        } else {
            return factory.createMultipleSlotAsmt(assignments, tildeArrowToken);
        }
    }

    public void validateSlotCount(List<Integer> contract, List<Integer> assignments, Token errorToken) {
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