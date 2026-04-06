package cod.interpreter;

import cod.interpreter.handler.TypeHandler;
import cod.interpreter.registry.LiteralRegistry;

public class InterpreterVisitor extends InterpreterCore {
    public InterpreterVisitor(Interpreter interpreter, TypeHandler typeSystem, LiteralRegistry literalRegistry) {
        super(interpreter, typeSystem, literalRegistry);
    }
}
