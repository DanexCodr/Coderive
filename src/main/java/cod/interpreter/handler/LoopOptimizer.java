package cod.interpreter.handler;

import cod.ast.ASTFactory;
import cod.ast.node.*;
import cod.debug.DebugSystem;
import cod.error.InternalError;
import cod.error.ProgramError;
import cod.interpreter.InterpreterVisitor;
import cod.interpreter.TailCallSignal;
import cod.interpreter.context.ExecutionContext;
import cod.interpreter.exception.BreakLoopException;
import cod.math.AutoStackingNumber;
import cod.range.NaturalArray;
import cod.range.Range;
import cod.range.pattern.ArrayTracker;
import cod.range.pattern.ConditionalPattern;
import cod.range.pattern.OutputAwarePattern;
import cod.range.pattern.SequencePattern;
import cod.range.formula.ConditionalFormula;
import cod.range.formula.SequenceFormula;

import java.util.*;

public class LoopOptimizer {
    private static final int LAZY_THRESHOLD = 10;
    private static final int MAX_SUPPORTED_LAG = 64;

    private final InterpreterVisitor dispatcher;
    private final TypeHandler typeSystem;
    private final ExpressionHandler expressionHandler;
    private final ArrayOperationHandler arrayOperationHandler;
    private final PatternApplier patternApplier;

    public LoopOptimizer(
        InterpreterVisitor dispatcher,
        TypeHandler typeSystem,
        ExpressionHandler expressionHandler,
        ArrayOperationHandler arrayOperationHandler,
        PatternApplier patternApplier) {
        if (dispatcher == null) throw new InternalError("LoopOptimizer dispatcher is null");
        if (typeSystem == null) throw new InternalError("LoopOptimizer typeSystem is null");
        if (expressionHandler == null) throw new InternalError("LoopOptimizer expressionHandler is null");
        if (arrayOperationHandler == null) throw new InternalError("LoopOptimizer arrayOperationHandler is null");
        if (patternApplier == null) throw new InternalError("LoopOptimizer patternApplier is null");
        this.dispatcher = dispatcher;
        this.typeSystem = typeSystem;
        this.expressionHandler = expressionHandler;
        this.arrayOperationHandler = arrayOperationHandler;
        this.patternApplier = patternApplier;
    }

    public Object executeForLoop(For node) {
        if (node == null) {
            throw new InternalError("visit(For) called with null node");
        }

        ExecutionContext ctx = dispatcher.getCurrentContext();
        int originalDepth = ctx.getScopeDepth();

        long loopSize = estimateLoopSize(node, ctx);
        boolean hasSideEffects = hasSideEffects(node.body);

        boolean useLazyExecution = shouldUseLazyExecution(loopSize, hasSideEffects);

        int loopId = ArrayTracker.beginLoop(node);

        ArrayTracker.setLoopSize(loopId, loopSize);
        ArrayTracker.setSideEffects(loopId, hasSideEffects);

        try {
            ctx.pushScope();

            if (useLazyExecution) {
                Object result = tryOptimizedExecution(node, loopId);
                if (result != null) {
                    return result;
                }
            } else {
                DebugSystem.debug("LOOP",
                    String.format("Skipping optimization: size=%d, sideEffects=%s",
                        loopSize, hasSideEffects));
            }

            ArrayTracker.incrementIteration();

            if (node.range != null) {
                return arrayOperationHandler.executeRangeLoop(ctx, node, node.iterator);
            } else if (node.arraySource != null) {
                Object arrayObj = dispatcher.dispatch(node.arraySource);
                arrayObj = typeSystem.unwrap(arrayObj);
                return arrayOperationHandler.executeArrayLoop(ctx, node, node.iterator, arrayObj);
            }
            throw new ProgramError("Invalid for loop: neither range nor array source specified");

        } catch (ProgramError e) {
            throw e;
        } catch (TailCallSignal e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("For loop execution failed", e);
        } finally {
            ArrayTracker.LoopStats stats = ArrayTracker.endLoop();
            if (stats != null) {
                DebugSystem.debug("LOOP", stats.toString());
            }

            while (ctx.getScopeDepth() > originalDepth) {
                ctx.popScope();
            }
        }
    }

    public boolean shouldUseLazyExecution(long loopSize, boolean hasSideEffects) {
        if (loopSize < 0) {
            return false;
        }

        if (loopSize < LAZY_THRESHOLD) {
            return !hasSideEffects;
        }

        return true;
    }

    public long estimateLoopSize(For node, ExecutionContext ctx) {
        try {
            if (node.range != null) {
                Object startObj = dispatcher.dispatch(node.range.start);
                Object endObj = dispatcher.dispatch(node.range.end);

                startObj = typeSystem.unwrap(startObj);
                endObj = typeSystem.unwrap(endObj);

                AutoStackingNumber start = typeSystem.toAutoStackingNumber(startObj);
                AutoStackingNumber end = typeSystem.toAutoStackingNumber(endObj);

                AutoStackingNumber step;
                if (node.range.step != null) {
                    Object stepObj = dispatcher.dispatch(node.range.step);
                    step = typeSystem.toAutoStackingNumber(typeSystem.unwrap(stepObj));
                } else {
                    step = (start.compareTo(end) > 0) ?
                        AutoStackingNumber.minusOne(1) : AutoStackingNumber.one(1);
                }

                if (step.isZero()) return 0;

                AutoStackingNumber diff = end.subtract(start);
                AutoStackingNumber steps = diff.divide(step);
                AutoStackingNumber size = steps.add(AutoStackingNumber.one(1));

                return size.longValue();

            } else if (node.arraySource != null) {
                Object arrayObj = dispatcher.dispatch(node.arraySource);
                arrayObj = typeSystem.unwrap(arrayObj);

                if (arrayObj instanceof NaturalArray) {
                    NaturalArray arr = (NaturalArray) arrayObj;
                    if (arr.hasPendingUpdates()) {
                        arr.commitUpdates();
                    }
                    return arr.size();
                } else if (arrayObj instanceof List) {
                    return ((List<?>) arrayObj).size();
                }
            }
        } catch (Exception e) {
            DebugSystem.debug("LOOP", "Failed to estimate size: " + e.getMessage());
        }

        return -1;
    }

    public boolean hasSideEffects(Block body) {
        if (body == null || body.statements == null) return false;

        for (Stmt stmt : body.statements) {
            if (stmt instanceof MethodCall) {
                MethodCall call = (MethodCall) stmt;
                if ("out".equals(call.name) || "outs".equals(call.name) || "in".equals(call.name)) {
                    return true;
                }
                return true;
            }

            if (stmt instanceof StmtIf) {
                StmtIf ifStmt = (StmtIf) stmt;
                if (hasSideEffects(ifStmt.thenBlock) || hasSideEffects(ifStmt.elseBlock)) {
                    return true;
                }
            }

            if (stmt instanceof For) {
                return true;
            }

            if (stmt instanceof Assignment) {
                Assignment assign = (Assignment) stmt;
                if (assign.left instanceof PropertyAccess) {
                    return true;
                }
            }
        }

        return false;
    }

    public Object tryOptimizedExecution(For node, int loopId) {
        OutputAwarePattern.OutputPattern outputPattern =
            OutputAwarePattern.extract(node, node.iterator);

        if (outputPattern.isOptimizable) {
            try {
                Object result = executeOutputAwareLoop(node, outputPattern);
                ArrayTracker.markLoopOptimized(loopId);
                return result;
            } catch (Exception e) {
                DebugSystem.debug("OPTIMIZER", "Output pattern failed: " + e.getMessage());
            }
        }

        List<PatternApplier.PatternResult> multiArrayPatterns = extractMultiArraySequencePatterns(node);
        if (!multiArrayPatterns.isEmpty()) {
            try {
                Object result = patternApplier.applyPatterns(node, multiArrayPatterns);
                ArrayTracker.markLoopOptimized(loopId);
                return result;
            } catch (Exception e) {
                DebugSystem.debug("OPTIMIZER", "Multi-array pattern failed: " + e.getMessage());
            }
        }

        PatternApplier.LinearRecurrencePattern recurrencePattern = extractLinearRecurrencePattern(node);
        if (recurrencePattern != null) {
            try {
                List<PatternApplier.PatternResult> patterns = new ArrayList<PatternApplier.PatternResult>();
                patterns.add(new PatternApplier.PatternResult(PatternApplier.PatternType.LINEAR_RECURRENCE, recurrencePattern, recurrencePattern.targetArray));
                Object result = patternApplier.applyPatterns(node, patterns);
                ArrayTracker.markLoopOptimized(loopId);
                return result;
            } catch (Exception e) {
                DebugSystem.debug("OPTIMIZER", "Linear recurrence pattern failed: " + e.getMessage());
            }
        }

        SequencePattern.Pattern seqPattern =
            SequencePattern.extract(node.body.statements, node.iterator);
        if (seqPattern != null && seqPattern.isOptimizable()) {
            try {
                List<PatternApplier.PatternResult> patterns = new ArrayList<PatternApplier.PatternResult>();
                patterns.add(new PatternApplier.PatternResult(PatternApplier.PatternType.SEQUENCE, seqPattern, seqPattern.targetArray));
                Object result = patternApplier.applyPatterns(node, patterns);
                ArrayTracker.markLoopOptimized(loopId);
                return result;
            } catch (Exception e) {
                DebugSystem.debug("OPTIMIZER", "Sequence pattern failed: " + e.getMessage());
            }
        }

        List<PatternApplier.PatternResult> allPatterns = new ArrayList<PatternApplier.PatternResult>();
        for (Stmt stmt : node.body.statements) {
            if (stmt instanceof StmtIf) {
                StmtIf ifStmt = (StmtIf) stmt;
                List<ConditionalPattern> patterns = extractConditionalPatterns(ifStmt, node.iterator);
                for (ConditionalPattern pattern : patterns) {
                    if (pattern != null && pattern.isOptimizable()) {
                        allPatterns.add(new PatternApplier.PatternResult(PatternApplier.PatternType.CONDITIONAL, pattern, pattern.array));
                    }
                }
            }
        }

        if (!allPatterns.isEmpty()) {
            try {
                Object result = patternApplier.applyPatterns(node, allPatterns);
                ArrayTracker.markLoopOptimized(loopId);
                return result;
            } catch (Exception e) {
                DebugSystem.debug("OPTIMIZER", "Conditional pattern failed: " + e.getMessage());
            }
        }

        return null;
    }

    public PatternApplier.LinearRecurrencePattern extractLinearRecurrencePattern(For node) {
        if (node == null || node.body == null || node.body.statements == null) {
            return null;
        }
        if (node.body.statements.size() != 1) {
            return null;
        }
        if (!(node.body.statements.get(0) instanceof Assignment)) {
            return null;
        }
        Assignment assign = (Assignment) node.body.statements.get(0);
        if (!(assign.left instanceof IndexAccess)) {
            return null;
        }
        IndexAccess leftAccess = (IndexAccess) assign.left;
        if (!(leftAccess.array instanceof Identifier) || !(leftAccess.index instanceof Identifier)) {
            return null;
        }
        String iter = node.iterator;
        Identifier idx = (Identifier) leftAccess.index;
        if (!iter.equals(idx.name)) {
            return null;
        }

        Object resolved = dispatcher.dispatch(leftAccess.array);
        resolved = typeSystem.unwrap(resolved);
        if (!(resolved instanceof NaturalArray)) {
            return null;
        }
        NaturalArray targetArray = (NaturalArray) resolved;

        Set<String> deps = new HashSet<String>();
        collectIndexedArrayRefs(assign.right, iter, deps);
        String targetName = ((Identifier) leftAccess.array).name;
        if (!deps.contains(targetName)) {
            return null;
        }
        for (String dep : deps) {
            if (!targetName.equals(dep)) {
                return null;
            }
        }

        AutoStackingNumber[] coeff = new AutoStackingNumber[MAX_SUPPORTED_LAG + 1];
        for (int i = 0; i < coeff.length; i++) coeff[i] = AutoStackingNumber.fromLong(0L);
        AutoStackingNumber[] constant = new AutoStackingNumber[]{AutoStackingNumber.fromLong(0L)};
        if (!collectLinearTerms(assign.right, targetName, iter, coeff, constant, AutoStackingNumber.fromLong(1L))) {
            return null;
        }

        int maxLag = 0;
        boolean hasAnyLag = false;
        for (int lag = 1; lag < coeff.length; lag++) {
            if (!coeff[lag].isZero()) {
                hasAnyLag = true;
                if (lag > maxLag) maxLag = lag;
            }
        }
        if (!hasAnyLag || maxLag <= 0) {
            return null;
        }

        int order = maxLag;
        AutoStackingNumber[] coeffByLag = new AutoStackingNumber[order];
        for (int lag = 1; lag <= order; lag++) {
            coeffByLag[lag - 1] = coeff[lag];
        }

        long[] bounds = resolveLoopBounds(node);
        if (bounds == null) {
            return null;
        }
        long min = bounds[0];
        long max = bounds[1];
        long recurrenceStart = min;
        if (recurrenceStart < order) {
            recurrenceStart = order;
        }
        if (recurrenceStart > max) {
            return null;
        }

        AutoStackingNumber[] seed = new AutoStackingNumber[order];
        long seedStart = recurrenceStart - order;
        for (int i = 0; i < order; i++) {
            long idxSeed = seedStart + i;
            Object vObj = targetArray.get(idxSeed);
            AutoStackingNumber v = typeSystem.toAutoStackingNumber(vObj);
            if (v == null) {
                return null;
            }
            seed[i] = v;
        }

        return new PatternApplier.LinearRecurrencePattern(
            leftAccess.array,
            order,
            coeffByLag,
            constant[0],
            recurrenceStart,
            seedStart,
            seed
        );
    }

    private boolean collectLinearTerms(
        Expr expr,
        String targetArrayName,
        String iterator,
        AutoStackingNumber[] coeffByLag,
        AutoStackingNumber[] constant,
        AutoStackingNumber sign
    ) {
        if (expr == null) return false;

        if (expr instanceof BinaryOp) {
            BinaryOp bin = (BinaryOp) expr;
            if ("+".equals(bin.op)) {
                return collectLinearTerms(bin.left, targetArrayName, iterator, coeffByLag, constant, sign) &&
                       collectLinearTerms(bin.right, targetArrayName, iterator, coeffByLag, constant, sign);
            }
            if ("-".equals(bin.op)) {
                return collectLinearTerms(bin.left, targetArrayName, iterator, coeffByLag, constant, sign) &&
                       collectLinearTerms(bin.right, targetArrayName, iterator, coeffByLag, constant, sign.multiply(AutoStackingNumber.fromLong(-1L)));
            }
            if ("*".equals(bin.op)) {
                TermRef ref = extractIndexedTargetTerm(bin.left, targetArrayName, iterator);
                AutoStackingNumber scalar = toNumericLiteral(bin.right);
                if (ref == null || scalar == null) {
                    ref = extractIndexedTargetTerm(bin.right, targetArrayName, iterator);
                    scalar = toNumericLiteral(bin.left);
                }
                if (ref != null && scalar != null) {
                    AutoStackingNumber c = sign.multiply(scalar);
                    coeffByLag[ref.lag] = coeffByLag[ref.lag].add(c);
                    return true;
                }
                return false;
            }
            return false;
        }

        TermRef ref = extractIndexedTargetTerm(expr, targetArrayName, iterator);
        if (ref != null) {
            coeffByLag[ref.lag] = coeffByLag[ref.lag].add(sign);
            return true;
        }

        AutoStackingNumber literal = toNumericLiteral(expr);
        if (literal != null) {
            constant[0] = constant[0].add(sign.multiply(literal));
            return true;
        }

        return false;
    }

    private static class TermRef {
        final int lag;
        TermRef(int lag) { this.lag = lag; }
    }

    private TermRef extractIndexedTargetTerm(Expr expr, String targetArrayName, String iterator) {
        if (!(expr instanceof IndexAccess)) {
            return null;
        }
        IndexAccess access = (IndexAccess) expr;
        if (!(access.array instanceof Identifier)) {
            return null;
        }
        String arrayName = ((Identifier) access.array).name;
        if (!targetArrayName.equals(arrayName)) {
            return null;
        }
        int lag = extractLag(access.index, iterator);
        if (lag <= 0 || lag > MAX_SUPPORTED_LAG) {
            return null;
        }
        return new TermRef(lag);
    }

    private int extractLag(Expr indexExpr, String iterator) {
        if (indexExpr instanceof BinaryOp) {
            BinaryOp bin = (BinaryOp) indexExpr;
            if ("-".equals(bin.op) && bin.left instanceof Identifier &&
                iterator.equals(((Identifier) bin.left).name)) {
                AutoStackingNumber n = toNumericLiteral(bin.right);
                if (n == null) return -1;
                long lag = n.longValue();
                if (lag <= 0 || lag > Integer.MAX_VALUE) return -1;
                return (int) lag;
            }
        }
        return -1;
    }

    private AutoStackingNumber toNumericLiteral(Expr expr) {
        if (expr instanceof IntLiteral) {
            return ((IntLiteral) expr).value;
        }
        if (expr instanceof FloatLiteral) {
            return ((FloatLiteral) expr).value;
        }
        if (expr instanceof Unary) {
            Unary unary = (Unary) expr;
            if ("-".equals(unary.op)) {
                AutoStackingNumber inner = toNumericLiteral(unary.operand);
                if (inner == null) return null;
                return AutoStackingNumber.fromLong(0L).subtract(inner);
            }
            if ("+".equals(unary.op)) {
                return toNumericLiteral(unary.operand);
            }
        }
        return null;
    }

    private long[] resolveLoopBounds(For node) {
        if (node == null) return null;
        if (node.range != null) {
            Object startObj = dispatcher.dispatch(node.range.start);
            Object endObj = dispatcher.dispatch(node.range.end);
            long start = expressionHandler.toLong(startObj);
            long end = expressionHandler.toLong(endObj);
            return new long[]{Math.min(start, end), Math.max(start, end)};
        }
        if (node.arraySource != null) {
            Object sourceObj = dispatcher.dispatch(node.arraySource);
            sourceObj = typeSystem.unwrap(sourceObj);
            if (sourceObj instanceof NaturalArray) {
                NaturalArray sourceArr = (NaturalArray) sourceObj;
                if (sourceArr.size() > 0) {
                    return new long[]{0L, sourceArr.size() - 1L};
                }
            } else if (sourceObj instanceof List) {
                List<?> list = (List<?>) sourceObj;
                if (!list.isEmpty()) {
                    return new long[]{0L, list.size() - 1L};
                }
            }
        }
        return null;
    }

    public List<PatternApplier.PatternResult> extractMultiArraySequencePatterns(For node) {
        List<PatternApplier.PatternResult> results = new ArrayList<PatternApplier.PatternResult>();
        if (node == null || node.body == null || node.body.statements == null) {
            return results;
        }

        List<Stmt> statements = node.body.statements;
        if (statements.size() < 2) {
            return results;
        }

        List<String> orderedTargets = new ArrayList<String>();
        List<Assignment> orderedAssignments = new ArrayList<Assignment>();

        for (Stmt stmt : statements) {
            if (!(stmt instanceof Assignment)) {
                return new ArrayList<PatternApplier.PatternResult>();
            }

            Assignment assign = (Assignment) stmt;
            if (assign.isDeclaration || !(assign.left instanceof IndexAccess)) {
                return new ArrayList<PatternApplier.PatternResult>();
            }

            IndexAccess indexAccess = (IndexAccess) assign.left;
            if (!(indexAccess.array instanceof Identifier) || !(indexAccess.index instanceof Identifier)) {
                return new ArrayList<PatternApplier.PatternResult>();
            }

            Identifier index = (Identifier) indexAccess.index;
            if (!node.iterator.equals(index.name)) {
                return new ArrayList<PatternApplier.PatternResult>();
            }

            String targetName = ((Identifier) indexAccess.array).name;
            if (orderedTargets.contains(targetName)) {
                return new ArrayList<PatternApplier.PatternResult>();
            }

            orderedTargets.add(targetName);
            orderedAssignments.add(assign);
        }

        for (int i = 0; i < orderedAssignments.size(); i++) {
            Assignment assign = orderedAssignments.get(i);
            IndexAccess indexAccess = (IndexAccess) assign.left;
            Identifier targetArray = (Identifier) indexAccess.array;

            Set<String> refs = new HashSet<String>();
            collectIndexedArrayRefs(assign.right, node.iterator, refs);

            for (String ref : refs) {
                int refIndex = orderedTargets.indexOf(ref);
                if (refIndex == -1 || refIndex > i) {
                    return new ArrayList<PatternApplier.PatternResult>();
                }
            }

            List<SequencePattern.Step> steps = new ArrayList<SequencePattern.Step>();
            steps.add(new SequencePattern.Step(null, assign.right));
            SequencePattern.Pattern pattern = new SequencePattern.Pattern(steps, targetArray, node.iterator);
            results.add(new PatternApplier.PatternResult(PatternApplier.PatternType.SEQUENCE, pattern, targetArray));
        }

        return results;
    }

    private void collectIndexedArrayRefs(Expr expr, String iterator, Set<String> refs) {
        if (expr == null || refs == null) {
            return;
        }

        if (expr instanceof IndexAccess) {
            IndexAccess access = (IndexAccess) expr;
            if (access.array instanceof Identifier && access.index instanceof Identifier) {
                Identifier idx = (Identifier) access.index;
                if (iterator.equals(idx.name)) {
                    refs.add(((Identifier) access.array).name);
                }
            }
            collectIndexedArrayRefs(access.array, iterator, refs);
            collectIndexedArrayRefs(access.index, iterator, refs);
            return;
        }

        if (expr instanceof BinaryOp) {
            BinaryOp bin = (BinaryOp) expr;
            collectIndexedArrayRefs(bin.left, iterator, refs);
            collectIndexedArrayRefs(bin.right, iterator, refs);
            return;
        }

        if (expr instanceof Unary) {
            collectIndexedArrayRefs(((Unary) expr).operand, iterator, refs);
            return;
        }

        if (expr instanceof MethodCall) {
            MethodCall call = (MethodCall) expr;
            if (call.arguments != null) {
                for (Expr arg : call.arguments) {
                    collectIndexedArrayRefs(arg, iterator, refs);
                }
            }
            return;
        }

        if (expr instanceof TypeCast) {
            collectIndexedArrayRefs(((TypeCast) expr).expression, iterator, refs);
            return;
        }

        if (expr instanceof PropertyAccess) {
            PropertyAccess prop = (PropertyAccess) expr;
            collectIndexedArrayRefs(prop.left, iterator, refs);
            collectIndexedArrayRefs(prop.right, iterator, refs);
            return;
        }

        if (expr instanceof Tuple) {
            Tuple tuple = (Tuple) expr;
            if (tuple.elements != null) {
                for (Expr elem : tuple.elements) {
                    collectIndexedArrayRefs(elem, iterator, refs);
                }
            }
            return;
        }

        if (expr instanceof Array) {
            Array array = (Array) expr;
            if (array.elements != null) {
                for (Expr elem : array.elements) {
                    collectIndexedArrayRefs(elem, iterator, refs);
                }
            }
        }
    }

    public Object executeOutputAwareLoop(For node, OutputAwarePattern.OutputPattern pattern) {
        ExecutionContext ctx = dispatcher.getCurrentContext();

        try {
            NaturalArray arr = createArrayFromOutputPattern(node, pattern.computation, ctx);

            ctx.enterOptimizedLoop();

            if (node.range != null) {
                executeOutputRangeLoop(ctx, node, arr, pattern.outputCalls);
            } else if (node.arraySource != null) {
                executeOutputArrayLoop(ctx, node, arr, pattern.outputCalls);
            }
            return arr;
        } finally {
            ctx.exitOptimizedLoop();
        }
    }

    public NaturalArray createArrayFromOutputPattern(For node, Object computation, ExecutionContext ctx) {
        if (computation instanceof SequencePattern.Pattern) {
            SequencePattern.Pattern seqPattern = (SequencePattern.Pattern) computation;

            Range range = node.range;
            if (range == null && node.arraySource != null) {
                Object sourceObj = dispatcher.dispatch(node.arraySource);
                sourceObj = typeSystem.unwrap(sourceObj);

                if (sourceObj instanceof NaturalArray) {
                    NaturalArray sourceArr = (NaturalArray) sourceObj;
                    long size = sourceArr.size();

                    Expr start = ASTFactory.createIntLiteral(0, null);
                    Expr end = ASTFactory.createIntLiteral((int)(size - 1), null);
                    range = ASTFactory.createRange(null, start, end, null, null);
                }
            }

            if (range == null) {
                throw new ProgramError("Cannot create array from pattern: no range specified");
            }

            NaturalArray arr = new NaturalArray(range, dispatcher, ctx);

            if (seqPattern.isSimple()) {
                SequenceFormula formula = SequenceFormula.createSimple(
                    0, arr.size() - 1,
                    seqPattern.getFinalExpression(),
                    node.iterator
                );
                arr.addSequenceFormula(formula);
            } else {
                SequenceFormula formula = SequenceFormula.createFromSequence(
                    0, arr.size() - 1, node.iterator,
                    seqPattern.getTempVarNames(),
                    seqPattern.getTempExpressions(),
                    seqPattern.getFinalExpression()
                );
                arr.addSequenceFormula(formula);
            }

            return arr;

        } else if (computation instanceof ConditionalPattern) {
            ConditionalPattern condPattern = (ConditionalPattern) computation;

            Range range = node.range;
            if (range == null && node.arraySource != null) {
                Object sourceObj = dispatcher.dispatch(node.arraySource);
                sourceObj = typeSystem.unwrap(sourceObj);

                if (sourceObj instanceof NaturalArray) {
                    NaturalArray sourceArr = (NaturalArray) sourceObj;
                    long size = sourceArr.size();

                    Expr start = ASTFactory.createIntLiteral(0, null);
                    Expr end = ASTFactory.createIntLiteral((int)(size - 1), null);
                    range = ASTFactory.createRange(null, start, end, null, null);
                }
            }

            if (range == null) {
                throw new ProgramError("Cannot create array from pattern: no range specified");
            }

            NaturalArray arr = new NaturalArray(range, dispatcher, ctx);

            List<Expr> conditions = new ArrayList<Expr>();
            List<List<Stmt>> branchStatements = new ArrayList<List<Stmt>>();

            for (ConditionalPattern.Branch branch : condPattern.branches) {
                conditions.add(branch.condition);
                branchStatements.add(branch.statements);
            }

            ConditionalFormula formula = new ConditionalFormula(
                0, arr.size() - 1, node.iterator,
                conditions,
                branchStatements,
                condPattern.elseStatements
            );
            arr.addConditionalFormula(formula);

            return arr;
        }

        throw new ProgramError("Unknown computation pattern type");
    }

    public void executeOutputRangeLoop(ExecutionContext ctx, For node,
                                       NaturalArray arr, List<MethodCall> outputCalls) {
        try {
            Object startObj = dispatcher.dispatch(node.range.start);
            Object endObj = dispatcher.dispatch(node.range.end);
            startObj = typeSystem.unwrap(startObj);
            endObj = typeSystem.unwrap(endObj);

            long start = expressionHandler.toLong(startObj);
            long end = expressionHandler.toLong(endObj);
            long step = arrayOperationHandler.calculateRangeStep(node.range);

            for (long i = start; i <= end; i += step) {
                Object value = arr.get(i);

                arr.recordOutput(i, value);

                ctx.setVariable(node.iterator, value);

                for (MethodCall outputCall : outputCalls) {
                    MethodCall evalCall = new MethodCall();
                    evalCall.name = outputCall.name;
                    evalCall.arguments = new ArrayList<Expr>();

                    for (Expr arg : outputCall.arguments) {
                        if (arg instanceof Identifier &&
                            "_".equals(((Identifier) arg).name)) {
                            evalCall.arguments.add(new ValueExpr(value));
                        } else {
                            evalCall.arguments.add(arg);
                        }
                    }

                    dispatcher.dispatch(evalCall);
                }
            }
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Output range loop execution failed", e);
        }
    }

    public void executeOutputArrayLoop(ExecutionContext ctx, For node,
                                       NaturalArray arr, List<MethodCall> outputCalls) {
        try {
            Object sourceObj = dispatcher.dispatch(node.arraySource);
            sourceObj = typeSystem.unwrap(sourceObj);

            long size = 0;
            if (sourceObj instanceof NaturalArray) {
                size = ((NaturalArray) sourceObj).size();
            } else if (sourceObj instanceof List) {
                size = ((List<?>) sourceObj).size();
            } else {
                throw new ProgramError("Cannot iterate over: " +
                    (sourceObj != null ? sourceObj.getClass().getSimpleName() : "null"));
            }

            for (long i = 0; i < size; i++) {
                Object value = arr.get(i);

                arr.recordOutput(i, value);

                ctx.setVariable(node.iterator, value);

                for (MethodCall outputCall : outputCalls) {
                    MethodCall evalCall = new MethodCall();
                    evalCall.name = outputCall.name;
                    evalCall.arguments = new ArrayList<Expr>();

                    for (Expr arg : outputCall.arguments) {
                        if (arg instanceof Identifier &&
                            "_".equals(((Identifier) arg).name)) {
                            evalCall.arguments.add(new ValueExpr(value));
                        } else {
                            evalCall.arguments.add(arg);
                        }
                    }

                    dispatcher.dispatch(evalCall);
                }
            }
        } catch (ProgramError e) {
            throw e;
        } catch (Exception e) {
            throw new InternalError("Output array loop execution failed", e);
        }
    }

    public List<ConditionalPattern> extractConditionalPatterns(StmtIf ifStmt, String iterator) {
        try {
            return ConditionalPattern.extractAll(ifStmt, iterator);
        } catch (Exception e) {
            DebugSystem.debug("OPTIMIZER", "Failed to extract conditional pattern: " + e.getMessage());
            return new ArrayList<ConditionalPattern>();
        }
    }
}
