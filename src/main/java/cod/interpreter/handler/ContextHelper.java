package cod.interpreter.handler;

import cod.ast.node.NoneLiteral;
import cod.ast.node.Type;
import cod.error.InternalError;
import cod.error.ProgramError;
import cod.interpreter.Interpreter;
import cod.interpreter.context.ExecutionContext;
import cod.range.NaturalArray;
import cod.ast.node.Range;

import java.util.List;
import java.util.Map;

public class ContextHelper {
    private final Interpreter interpreter;
    private Type internalRangeSpecType;
    private Type internalMultiRangeSpecType;

    public ContextHelper(Interpreter interpreter) {
        if (interpreter == null) {
            throw new InternalError("ContextHelper constructed with null interpreter");
        }
        this.interpreter = interpreter;
    }

    public Object createNoneValue() {
        return new NoneLiteral();
    }

    public Range getRangeFromArray(NaturalArray arr) {
        try {
            java.lang.reflect.Field rangeField = NaturalArray.class.getDeclaredField("baseRange");
            rangeField.setAccessible(true);
            return (Range) rangeField.get(arr);
        } catch (Exception e) {
            return null;
        }
    }

    public Type resolveInternalRangeSpecType() {
        if (internalRangeSpecType != null) {
            return internalRangeSpecType;
        }
        try {
            Type type = interpreter.getImportResolver().resolveImport("internal.range.RangeSpec");
            if (type == null) {
                throw new ProgramError("Unable to load internal.range.RangeSpec");
            }
            internalRangeSpecType = type;
            return type;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Failed loading internal.range.RangeSpec", e);
        }
    }

    public Type resolveInternalMultiRangeSpecType() {
        if (internalMultiRangeSpecType != null) {
            return internalMultiRangeSpecType;
        }
        try {
            Type type = interpreter.getImportResolver().resolveImport("internal.range.MultiRangeSpec");
            if (type == null) {
                throw new ProgramError("Unable to load internal.range.MultiRangeSpec");
            }
            internalMultiRangeSpecType = type;
            return type;
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Failed loading internal.range.MultiRangeSpec", e);
        }
    }

    public boolean isVariableDeclaredInAnyScope(ExecutionContext ctx, String name) {
        if (ctx == null || name == null) return false;
        List<Map<String, Object>> localsStack = ctx.getLocalsStack();
        if (localsStack == null) return false;
        for (int i = localsStack.size() - 1; i >= 0; i--) {
            Map<String, Object> scope = localsStack.get(i);
            if (scope != null && scope.containsKey(name)) {
                return true;
            }
        }
        return false;
    }
}
