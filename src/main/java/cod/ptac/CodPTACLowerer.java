package cod.ptac;

import cod.ast.node.*;
import cod.range.pattern.ConditionalPattern;
import cod.range.pattern.SequencePattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CodPTACLowerer {
    private int tempCounter = 0;
    private int patternCounter = 0;
    private int lambdaCounter = 0;

    public CodPTACUnit lower(String unitName, Type type) {
        CodPTACUnit unit = new CodPTACUnit();
        unit.unitName = unitName;
        unit.className = type != null ? type.name : null;

        if (type == null || type.methods == null) {
            return unit;
        }

        for (Method method : type.methods) {
            unit.functions.add(lowerMethod(method, unit));
        }
        if (findFunction(unit, "main") != null) {
            unit.entryFunction = "main";
        } else if (!unit.functions.isEmpty()) {
            unit.entryFunction = unit.functions.get(0).name;
        }

        return unit;
    }

    private CodPTACFunction lowerMethod(Method method, CodPTACUnit unit) {
        CodPTACFunction fn = new CodPTACFunction();
        fn.name = method != null ? method.methodName : "anonymous";
        if (method != null && method.parameters != null) {
            for (Param param : method.parameters) {
                fn.parameters.add(param.name);
            }
        }
        if (method == null || method.body == null) {
            return fn;
        }

        for (Stmt stmt : method.body) {
            lowerStmt(stmt, fn, unit);
        }
        fn.instructions.add(new CodPTACInstruction(
            CodPTACOpcode.RETURN,
            null,
            Arrays.asList(CodPTACOperand.immediate(null))
        ));
        return fn;
    }

    private void lowerStmt(Stmt stmt, CodPTACFunction fn, CodPTACUnit unit) {
        if (stmt == null) return;

        if (stmt instanceof Var) {
            Var var = (Var) stmt;
            CodPTACOperand value = lowerExpr(var.value, fn, unit);
            fn.instructions.add(new CodPTACInstruction(
                CodPTACOpcode.ASSIGN,
                var.name,
                Arrays.asList(value)
            ));
            return;
        }

        if (stmt instanceof Assignment) {
            Assignment assign = (Assignment) stmt;
            CodPTACOperand rhs = lowerExpr(assign.right, fn, unit);
            if (assign.left instanceof Identifier) {
                fn.instructions.add(new CodPTACInstruction(
                    CodPTACOpcode.ASSIGN,
                    ((Identifier) assign.left).name,
                    Arrays.asList(rhs)
                ));
            } else if (assign.left instanceof IndexAccess) {
                IndexAccess access = (IndexAccess) assign.left;
                CodPTACOperand arr = lowerExpr(access.array, fn, unit);
                CodPTACOperand idx = lowerExpr(access.index, fn, unit);
                fn.instructions.add(new CodPTACInstruction(
                    CodPTACOpcode.LAZY_SET,
                    null,
                    Arrays.asList(arr, idx, rhs)
                ));
            }
            return;
        }

        if (stmt instanceof SlotAssignment) {
            SlotAssignment slot = (SlotAssignment) stmt;
            fn.instructions.add(new CodPTACInstruction(
                CodPTACOpcode.SLOT_SET,
                null,
                Arrays.asList(
                    CodPTACOperand.slot(slot.slotName),
                    lowerExpr(slot.value, fn, unit)
                )
            ));
            return;
        }

        if (stmt instanceof MultipleSlotAssignment) {
            MultipleSlotAssignment multi = (MultipleSlotAssignment) stmt;
            if (multi.assignments != null) {
                for (SlotAssignment slot : multi.assignments) {
                    lowerStmt(slot, fn, unit);
                }
            }
            return;
        }

        if (stmt instanceof ReturnSlotAssignment) {
            ReturnSlotAssignment ret = (ReturnSlotAssignment) stmt;
            if (ret.methodCall != null) {
                CodPTACOperand result = lowerExpr(ret.methodCall, fn, unit);
                fn.instructions.add(new CodPTACInstruction(
                    CodPTACOpcode.SLOT_UNPACK,
                    null,
                    Arrays.asList(result)
                ));
            } else if (ret.lambda != null) {
                CodPTACOperand lambdaReg = lowerExpr(ret.lambda, fn, unit);
                fn.instructions.add(new CodPTACInstruction(
                    CodPTACOpcode.SLOT_UNPACK,
                    null,
                    Arrays.asList(lambdaReg)
                ));
            }
            return;
        }

        if (stmt instanceof For) {
            lowerFor((For) stmt, fn, unit);
            return;
        }

        if (stmt instanceof StmtIf) {
            StmtIf ifStmt = (StmtIf) stmt;
            String thenLabel = "L_then_" + nextTemp();
            String endLabel = "L_end_" + nextTemp();
            CodPTACOperand cond = lowerExpr(ifStmt.condition, fn, unit);
            fn.instructions.add(new CodPTACInstruction(
                CodPTACOpcode.BRANCH_IF,
                null,
                Arrays.asList(cond, CodPTACOperand.label(thenLabel))
            ));
            if (ifStmt.elseBlock != null && ifStmt.elseBlock.statements != null) {
                for (Stmt elseStmt : ifStmt.elseBlock.statements) {
                    lowerStmt(elseStmt, fn, unit);
                }
            }
            fn.instructions.add(new CodPTACInstruction(
                CodPTACOpcode.BRANCH,
                null,
                Arrays.asList(CodPTACOperand.label(endLabel))
            ));
            fn.instructions.add(new CodPTACInstruction(
                CodPTACOpcode.NOP,
                thenLabel,
                new ArrayList<CodPTACOperand>()
            ));
            if (ifStmt.thenBlock != null && ifStmt.thenBlock.statements != null) {
                for (Stmt thenStmt : ifStmt.thenBlock.statements) {
                    lowerStmt(thenStmt, fn, unit);
                }
            }
            fn.instructions.add(new CodPTACInstruction(
                CodPTACOpcode.NOP,
                endLabel,
                new ArrayList<CodPTACOperand>()
            ));
            return;
        }

        if (stmt instanceof Block) {
            Block block = (Block) stmt;
            if (block.statements != null) {
                for (Stmt nested : block.statements) {
                    lowerStmt(nested, fn, unit);
                }
            }
            return;
        }

        if (stmt instanceof Exit) {
            fn.instructions.add(new CodPTACInstruction(
                CodPTACOpcode.RETURN,
                null,
                Arrays.asList(CodPTACOperand.immediate(null))
            ));
            return;
        }

        if (stmt instanceof Expr) {
            lowerExpr((Expr) stmt, fn, unit);
            return;
        }

        fn.instructions.add(new CodPTACInstruction(CodPTACOpcode.NOP, null, new ArrayList<CodPTACOperand>()));
    }

    private void lowerFor(For node, CodPTACFunction fn, CodPTACUnit unit) {
        if (node == null || node.range == null) return;

        String rangeReg = nextPattern();
        CodPTACOpcode rangeOpcode = selectRangeOpcode(node.range);
        List<CodPTACOperand> rangeOps = new ArrayList<CodPTACOperand>();
        rangeOps.add(lowerExpr(node.range.start, fn, unit));
        rangeOps.add(lowerExpr(node.range.end, fn, unit));
        if (node.range.step != null) {
            rangeOps.add(lowerExpr(node.range.step, fn, unit));
        }
        fn.instructions.add(new CodPTACInstruction(rangeOpcode, rangeReg, rangeOps));

        List<Stmt> body = node.body != null ? node.body.statements : null;
        if (body == null || body.isEmpty()) return;

        SequencePattern.Pattern seq = SequencePattern.extract(body, node.iterator);
        ConditionalPattern cond = null;
        if (body.size() == 1 && body.get(0) instanceof StmtIf) {
            cond = ConditionalPattern.extract((StmtIf) body.get(0), node.iterator);
        }

        if (seq != null && seq.isOptimizable() && cond != null && cond.isOptimizable()) {
            String condLambda = lowerConditionLambda(cond, unit);
            String mapLambda = lowerSequenceLambda(seq, unit);
            fn.instructions.add(new CodPTACInstruction(
                CodPTACOpcode.FILTER_MAP,
                nextPattern(),
                Arrays.asList(
                    CodPTACOperand.register(rangeReg),
                    CodPTACOperand.function(condLambda),
                    CodPTACOperand.function(mapLambda)
                )
            ));
            return;
        }

        if (cond != null && cond.isOptimizable()) {
            String condLambda = lowerConditionLambda(cond, unit);
            fn.instructions.add(new CodPTACInstruction(
                CodPTACOpcode.FILTER,
                nextPattern(),
                Arrays.asList(CodPTACOperand.register(rangeReg), CodPTACOperand.function(condLambda))
            ));
            return;
        }

        if (seq != null && seq.isOptimizable()) {
            String lambdaName = lowerSequenceLambda(seq, unit);
            fn.instructions.add(new CodPTACInstruction(
                CodPTACOpcode.MAP,
                nextPattern(),
                Arrays.asList(CodPTACOperand.register(rangeReg), CodPTACOperand.function(lambdaName))
            ));
            return;
        }

        for (Stmt stmt : body) {
            lowerStmt(stmt, fn, unit);
        }
    }

    private String lowerConditionLambda(ConditionalPattern pattern, CodPTACUnit unit) {
        String lambdaName = nextLambdaName("cond");
        CodPTACFunction lambda = new CodPTACFunction();
        lambda.name = lambdaName;
        lambda.lambdaBlock = true;
        lambda.parameters.add(pattern.indexVar != null ? pattern.indexVar : "p0");

        if (pattern.branches != null && !pattern.branches.isEmpty()) {
            ConditionalPattern.Branch first = pattern.branches.get(0);
            if (first != null && first.condition != null) {
                CodPTACOperand condition = lowerExpr(first.condition, lambda, unit);
                lambda.instructions.add(new CodPTACInstruction(
                    CodPTACOpcode.RETURN,
                    null,
                    Arrays.asList(condition)
                ));
            }
        }
        if (lambda.instructions.isEmpty()) {
            lambda.instructions.add(new CodPTACInstruction(
                CodPTACOpcode.RETURN,
                null,
                Arrays.asList(CodPTACOperand.immediate(Boolean.TRUE))
            ));
        }
        unit.functions.add(lambda);
        return lambdaName;
    }

    private String lowerSequenceLambda(SequencePattern.Pattern pattern, CodPTACUnit unit) {
        String lambdaName = nextLambdaName("seq");
        CodPTACFunction lambda = new CodPTACFunction();
        lambda.name = lambdaName;
        lambda.lambdaBlock = true;
        lambda.parameters.add(pattern.indexVar != null ? pattern.indexVar : "p0");

        if (pattern.steps != null) {
            for (SequencePattern.Step step : pattern.steps) {
                if (step == null) {
                    continue;
                }
                CodPTACOperand value = lowerExpr(step.expression, lambda, unit);
                if (step.tempVar != null) {
                    lambda.instructions.add(new CodPTACInstruction(
                        CodPTACOpcode.ASSIGN,
                        step.tempVar,
                        Arrays.asList(value)
                    ));
                } else {
                    lambda.instructions.add(new CodPTACInstruction(
                        CodPTACOpcode.RETURN,
                        null,
                        Arrays.asList(value)
                    ));
                }
            }
        }
        if (lambda.instructions.isEmpty()) {
            lambda.instructions.add(new CodPTACInstruction(
                CodPTACOpcode.RETURN,
                null,
                Arrays.asList(CodPTACOperand.immediate(null))
            ));
        }
        unit.functions.add(lambda);
        return lambdaName;
    }

    private CodPTACOperand lowerExpr(Expr expr, CodPTACFunction fn, CodPTACUnit unit) {
        if (expr == null) return CodPTACOperand.immediate(null);

        if (expr instanceof IntLiteral) return CodPTACOperand.immediate(((IntLiteral) expr).value);
        if (expr instanceof FloatLiteral) return CodPTACOperand.immediate(((FloatLiteral) expr).value);
        if (expr instanceof BoolLiteral) return CodPTACOperand.immediate(((BoolLiteral) expr).value);
        if (expr instanceof TextLiteral) return CodPTACOperand.immediate(((TextLiteral) expr).value);
        if (expr instanceof NoneLiteral) return CodPTACOperand.immediate(null);
        if (expr instanceof Identifier) return CodPTACOperand.register(((Identifier) expr).name);

        if (expr instanceof Range) {
            Range range = (Range) expr;
            String dest = nextPattern();
            CodPTACOpcode op = selectRangeOpcode(range);
            List<CodPTACOperand> ops = new ArrayList<CodPTACOperand>();
            ops.add(lowerExpr(range.start, fn, unit));
            ops.add(lowerExpr(range.end, fn, unit));
            if (range.step != null) ops.add(lowerExpr(range.step, fn, unit));
            fn.instructions.add(new CodPTACInstruction(op, dest, ops));
            return CodPTACOperand.register(dest);
        }

        if (expr instanceof IndexAccess) {
            IndexAccess access = (IndexAccess) expr;
            String dest = nextTemp();
            fn.instructions.add(new CodPTACInstruction(
                CodPTACOpcode.LAZY_GET,
                dest,
                Arrays.asList(
                    lowerExpr(access.array, fn, unit),
                    lowerExpr(access.index, fn, unit)
                )
            ));
            return CodPTACOperand.register(dest);
        }

        if (expr instanceof BinaryOp) {
            BinaryOp binary = (BinaryOp) expr;
            CodPTACOperand left = lowerExpr(binary.left, fn, unit);
            CodPTACOperand right = lowerExpr(binary.right, fn, unit);
            String dest = nextTemp();
            fn.instructions.add(new CodPTACInstruction(
                mapBinary(binary.op),
                dest,
                Arrays.asList(left, right)
            ));
            return CodPTACOperand.register(dest);
        }

        if (expr instanceof MethodCall) {
            MethodCall call = (MethodCall) expr;
            List<CodPTACOperand> ops = new ArrayList<CodPTACOperand>();
            if (call.isSelfCall) {
                for (Expr arg : call.arguments) {
                    ops.add(lowerExpr(arg, fn, unit));
                }
                String dest = nextTemp();
                fn.instructions.add(new CodPTACInstruction(CodPTACOpcode.SELF, dest, ops));
                return CodPTACOperand.register(dest);
            }

            ops.add(CodPTACOperand.function(call.name));
            if (call.arguments != null) {
                for (Expr arg : call.arguments) {
                    ops.add(lowerExpr(arg, fn, unit));
                }
            }
            String dest = nextTemp();
            fn.instructions.add(new CodPTACInstruction(CodPTACOpcode.CALL, dest, ops));
            return CodPTACOperand.register(dest);
        }

        if (expr instanceof Lambda) {
            Lambda lambdaNode = (Lambda) expr;
            String lambdaName = nextLambdaName("inline");
            CodPTACFunction lambda = new CodPTACFunction();
            lambda.name = lambdaName;
            lambda.lambdaBlock = true;
            if (lambdaNode.parameters != null) {
                for (Param param : lambdaNode.parameters) {
                    lambda.parameters.add(param.name);
                }
            }
            if (lambdaNode.expressionBody != null) {
                CodPTACOperand val = lowerExpr(lambdaNode.expressionBody, lambda, unit);
                lambda.instructions.add(new CodPTACInstruction(CodPTACOpcode.RETURN, null, Arrays.asList(val)));
            } else if (lambdaNode.body != null) {
                lowerStmt(lambdaNode.body, lambda, unit);
            }
            if (lambda.instructions.isEmpty()) {
                lambda.instructions.add(new CodPTACInstruction(
                    CodPTACOpcode.RETURN,
                    null,
                    Arrays.asList(CodPTACOperand.immediate(null))
                ));
            }
            unit.functions.add(lambda);
            return CodPTACOperand.function(lambdaName);
        }

        return CodPTACOperand.identifier(String.valueOf(expr));
    }

    private CodPTACOpcode selectRangeOpcode(Range range) {
        if (range != null && (range.start instanceof TextLiteral || range.end instanceof TextLiteral)) {
            return range.step == null ? CodPTACOpcode.RANGE_L : CodPTACOpcode.RANGE_LS;
        }
        return range != null && range.step != null ? CodPTACOpcode.RANGE_S : CodPTACOpcode.RANGE;
    }

    private CodPTACOpcode mapBinary(String op) {
        if ("+".equals(op)) return CodPTACOpcode.ADD;
        if ("-".equals(op)) return CodPTACOpcode.SUB;
        if ("*".equals(op)) return CodPTACOpcode.MUL;
        if ("/".equals(op)) return CodPTACOpcode.DIV;
        if ("%".equals(op)) return CodPTACOpcode.MOD;
        if ("==".equals(op)) return CodPTACOpcode.EQ;
        if ("!=".equals(op)) return CodPTACOpcode.NE;
        if (">".equals(op)) return CodPTACOpcode.GT;
        if ("<".equals(op)) return CodPTACOpcode.LT;
        if (">=".equals(op)) return CodPTACOpcode.GTE;
        if ("<=".equals(op)) return CodPTACOpcode.LTE;
        return CodPTACOpcode.NOP;
    }

    private String nextTemp() {
        return "t" + (tempCounter++);
    }

    private String nextPattern() {
        return "p" + (patternCounter++);
    }

    private String nextLambdaName(String prefix) {
        return "lambda$" + prefix + "$" + (lambdaCounter++);
    }

    private CodPTACFunction findFunction(CodPTACUnit unit, String name) {
        if (unit == null || unit.functions == null) return null;
        for (CodPTACFunction fn : unit.functions) {
            if (fn != null && name.equals(fn.name)) return fn;
        }
        return null;
    }
}
