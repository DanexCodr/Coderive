package cod.interpreter.handler;

import cod.ast.node.*;
import cod.debug.DebugSystem;
import cod.error.InternalError;
import cod.error.ProgramError;
import cod.interpreter.InterpreterVisitor;
import cod.math.AutoStackingNumber;
import cod.range.NaturalArray;
import cod.range.formula.ConditionalFormula;
import cod.range.formula.LinearRecurrenceFormula;
import cod.range.formula.SequenceFormula;
import cod.range.pattern.ConditionalPattern;
import cod.range.pattern.SequencePattern;

import java.util.*;

public class PatternApplier {
    public enum PatternType {
        CONDITIONAL,
        SEQUENCE,
        LINEAR_RECURRENCE
    }

    public static class PatternResult {
        public final PatternType type;
        public final Object pattern;
        public final Expr targetArray;

        public PatternResult(PatternType type, Object pattern, Expr targetArray) {
            if (type == null) {
                throw new InternalError("PatternResult constructed with null type");
            }
            this.type = type;
            this.pattern = pattern;
            this.targetArray = targetArray;
        }
    }

    public static class LinearRecurrencePattern {
        public final Expr targetArray;
        public final int order;
        public final AutoStackingNumber[] coefficientsByLag;
        public final AutoStackingNumber constantTerm;
        public final long recurrenceStart;
        public final long seedStart;
        public final AutoStackingNumber[] seedValues;

        public LinearRecurrencePattern(
            Expr targetArray,
            int order,
            AutoStackingNumber[] coefficientsByLag,
            AutoStackingNumber constantTerm,
            long recurrenceStart,
            long seedStart,
            AutoStackingNumber[] seedValues
        ) {
            this.targetArray = targetArray;
            this.order = order;
            this.coefficientsByLag = coefficientsByLag;
            this.constantTerm = constantTerm;
            this.recurrenceStart = recurrenceStart;
            this.seedStart = seedStart;
            this.seedValues = seedValues;
        }
    }

    private final InterpreterVisitor dispatcher;
    private final TypeHandler typeSystem;
    private final ExpressionHandler expressionHandler;
    private final ArrayOperationHandler arrayOperationHandler;

    public PatternApplier(
        InterpreterVisitor dispatcher,
        TypeHandler typeSystem,
        ExpressionHandler expressionHandler,
        ArrayOperationHandler arrayOperationHandler
    ) {
        if (dispatcher == null) throw new InternalError("PatternApplier dispatcher is null");
        if (typeSystem == null) throw new InternalError("PatternApplier typeSystem is null");
        if (expressionHandler == null) throw new InternalError("PatternApplier expressionHandler is null");
        if (arrayOperationHandler == null) throw new InternalError("PatternApplier arrayOperationHandler is null");
        this.dispatcher = dispatcher;
        this.typeSystem = typeSystem;
        this.expressionHandler = expressionHandler;
        this.arrayOperationHandler = arrayOperationHandler;
    }

    public Object applyPatterns(For node, List<PatternResult> patterns) {
        if (node == null) {
            throw new InternalError("applyPatterns called with null node");
        }
        if (patterns == null) {
            throw new InternalError("applyPatterns called with null patterns");
        }

        try {
            List<NaturalArray> targetArrays = new ArrayList<NaturalArray>();
            List<List<PatternResult>> groupedPatterns = new ArrayList<List<PatternResult>>();
            Map<Integer, Integer> arrayIdToGroupIndex = new HashMap<Integer, Integer>();

            for (PatternResult result : patterns) {
                if (result == null || result.targetArray == null) {
                    continue;
                }

                Object resolvedArray = dispatcher.dispatch(result.targetArray);
                resolvedArray = typeSystem.unwrap(resolvedArray);

                if (!(resolvedArray instanceof NaturalArray)) {
                    DebugSystem.debug("OPTIMIZER", "Array not optimizable, falling back to normal execution");
                    return arrayOperationHandler.executeForLoopNormally(node);
                }

                NaturalArray naturalArray = (NaturalArray) resolvedArray;
                int arrayId = naturalArray.getArrayId();
                Integer existingGroup = arrayIdToGroupIndex.get(arrayId);
                int groupIndex = existingGroup != null ? existingGroup.intValue() : -1;

                if (groupIndex == -1) {
                    targetArrays.add(naturalArray);
                    List<PatternResult> newGroup = new ArrayList<PatternResult>();
                    newGroup.add(result);
                    groupedPatterns.add(newGroup);
                    arrayIdToGroupIndex.put(arrayId, targetArrays.size() - 1);
                } else {
                    groupedPatterns.get(groupIndex).add(result);
                }
            }

            if (targetArrays.isEmpty()) {
                DebugSystem.debug("OPTIMIZER", "No target arrays found, falling back to normal execution");
                return arrayOperationHandler.executeForLoopNormally(node);
            }

            long start = 0, end = 0;
            boolean boundsFound = false;

            if (node.range != null) {
                Object startObj = dispatcher.dispatch(node.range.start);
                Object endObj = dispatcher.dispatch(node.range.end);
                start = expressionHandler.toLong(startObj);
                end = expressionHandler.toLong(endObj);
                boundsFound = true;
            } else if (node.arraySource != null) {
                Object sourceObj = dispatcher.dispatch(node.arraySource);
                if (sourceObj instanceof NaturalArray) {
                    NaturalArray sourceArr = (NaturalArray) sourceObj;
                    if (sourceArr.size() > 0) {
                        start = 0;
                        end = sourceArr.size() - 1;
                        boundsFound = true;
                    }
                }
            }

            if (!boundsFound) {
                DebugSystem.debug("OPTIMIZER", "Could not determine bounds, falling back to normal execution");
                return arrayOperationHandler.executeForLoopNormally(node);
            }

            long min = Math.min(start, end);
            long max = Math.max(start, end);

            for (int arrayIndex = 0; arrayIndex < targetArrays.size(); arrayIndex++) {
                NaturalArray arr = targetArrays.get(arrayIndex);
                List<PatternResult> arrayPatterns = groupedPatterns.get(arrayIndex);

                for (PatternResult result : arrayPatterns) {
                    if (result.type == PatternType.SEQUENCE) {
                        applySequencePattern(arr, (SequencePattern.Pattern) result.pattern, min, max, node.iterator);
                    } else if (result.type == PatternType.CONDITIONAL) {
                        applyConditionalPattern(arr, (ConditionalPattern) result.pattern, min, max, node.iterator);
                    } else if (result.type == PatternType.LINEAR_RECURRENCE) {
                        applyLinearRecurrencePattern(arr, (LinearRecurrencePattern) result.pattern, min, max, node.iterator);
                    }
                }
            }

            return targetArrays.get(targetArrays.size() - 1);
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Pattern application failed, falling back to normal execution", e);
        }
    }

    public void applyConditionalPattern(NaturalArray arr, ConditionalPattern pattern,
                                        long min, long max, String iterator) {
        if (pattern == null) {
            throw new InternalError("applyConditionalPattern called with null pattern");
        }
        if (arr == null) {
            throw new InternalError("applyConditionalPattern called with null array");
        }

        try {
            List<Expr> conditions = new ArrayList<Expr>();
            List<List<Stmt>> branchStatements = new ArrayList<List<Stmt>>();

            for (ConditionalPattern.Branch branch : pattern.branches) {
                conditions.add(branch.condition);
                branchStatements.add(branch.statements);
            }

            ConditionalFormula formula = new ConditionalFormula(
                min, max, iterator,
                conditions,
                branchStatements,
                pattern.elseStatements
            );
            arr.addConditionalFormula(formula);
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Failed to apply conditional pattern", e);
        }
    }

    public void applySequencePattern(NaturalArray arr,
                                     SequencePattern.Pattern pattern,
                                     long min, long max, String iterator) {
        if (pattern == null) {
            throw new InternalError("applySequencePattern called with null pattern");
        }
        if (arr == null) {
            throw new InternalError("applySequencePattern called with null array");
        }

        try {
            SequenceFormula formula;

            if (pattern.isSimple()) {
                formula = SequenceFormula.createSimple(min, max, pattern.getFinalExpression(), iterator);
            } else {
                formula = SequenceFormula.createFromSequence(
                    min, max, iterator,
                    pattern.getTempVarNames(),
                    pattern.getTempExpressions(),
                    pattern.getFinalExpression()
                );
            }

            arr.addSequenceFormula(formula);

        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Failed to apply sequence pattern", e);
        }
    }

    public void applyLinearRecurrencePattern(
        NaturalArray arr,
        LinearRecurrencePattern pattern,
        long min,
        long max,
        String iterator
    ) {
        if (arr == null) {
            throw new InternalError("applyLinearRecurrencePattern called with null array");
        }
        if (pattern == null) {
            throw new InternalError("applyLinearRecurrencePattern called with null pattern");
        }
        try {
            long start = Math.max(min, pattern.seedStart);
            long end = max;
            if (end < start) {
                return;
            }
            LinearRecurrenceFormula formula = new LinearRecurrenceFormula(
                start,
                end,
                pattern.recurrenceStart,
                pattern.coefficientsByLag,
                pattern.constantTerm,
                pattern.seedValues,
                pattern.seedStart
            );
            arr.addLinearRecurrenceFormula(formula);
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Failed to apply linear recurrence pattern", e);
        }
    }
}
