package cod.range.formula;

import cod.ast.nodes.ExprNode;
import cod.interpreter.Evaluator;
import cod.interpreter.context.ExecutionContext;
import java.util.*;

public class SequenceFormula {
    public final long start;
    public final long end;
    public final String indexVar;
    public final List<Step> steps;
    
    public static class Step {
        public final String tempVar;
        public final ExprNode expression;
        
        public Step(String tempVar, ExprNode expression) {
            this.tempVar = tempVar;
            this.expression = expression;
        }
        
        public boolean isFinal() {
            return tempVar == null;
        }
    }
    
    public SequenceFormula(long start, long end, ExprNode formula, String indexVar) {
        this.start = start;
        this.end = end;
        this.indexVar = indexVar;
        this.steps = new ArrayList<Step>();
        this.steps.add(new Step(null, formula));
    }
    
    public SequenceFormula(long start, long end, String indexVar, List<Step> steps) {
        this.start = start;
        this.end = end;
        this.indexVar = indexVar;
        this.steps = steps != null ? steps : new ArrayList<Step>();
        
        if (this.steps.isEmpty()) {
            throw new IllegalArgumentException("SequenceFormula must have at least one step");
        }
        
        Step lastStep = this.steps.get(this.steps.size() - 1);
        if (lastStep.tempVar != null) {
            throw new IllegalArgumentException("Last step must have null tempVar (final result)");
        }
    }
    
    public boolean contains(long index) {
        return index >= start && index <= end;
    }
    
    public Object evaluate(long index, Evaluator evaluator, ExecutionContext context) {
        // Save old values to restore later
        Object oldIndexValue = context.getVariable(indexVar);
        Map<String, Object> oldTempValues = new HashMap<String, Object>();
        
        try {
            // Set the current index
            context.setVariable(indexVar, index);
            
            Object lastResult = null;
            
            // Execute each step in sequence
            for (int i = 0; i < steps.size(); i++) {
                Step step = steps.get(i);
                
                // Evaluate the expression in current context
                Object stepResult = evaluator.evaluate(step.expression, context);
                
                if (step.tempVar != null) {
                    // Save old value if exists
                    Object oldTemp = context.getVariable(step.tempVar);
                    if (oldTemp != null) {
                        oldTempValues.put(step.tempVar, oldTemp);
                    }
                    
                    // Store new value
                    context.setVariable(step.tempVar, stepResult);
                }
                
                lastResult = stepResult;
            }
            
            return lastResult;
            
        } finally {
            // Restore index variable
            if (oldIndexValue != null) {
                context.setVariable(indexVar, oldIndexValue);
            } else {
                context.removeVariable(indexVar);
            }
            
            // Restore or remove temp variables
            for (Step step : steps) {
                if (step.tempVar != null) {
                    if (oldTempValues.containsKey(step.tempVar)) {
                        context.setVariable(step.tempVar, oldTempValues.get(step.tempVar));
                    } else {
                        context.removeVariable(step.tempVar);
                    }
                }
            }
        }
    }
    
    public static SequenceFormula createSimple(long start, long end, ExprNode formula, String indexVar) {
        return new SequenceFormula(start, end, formula, indexVar);
    }
    
    public static SequenceFormula createFromSequence(long start, long end, String indexVar,
                                                    List<String> tempVarNames,
                                                    List<ExprNode> tempExpressions,
                                                    ExprNode finalExpr) {
        if (tempVarNames.size() != tempExpressions.size()) {
            throw new IllegalArgumentException("tempVarNames and tempExpressions must have same size");
        }
        
        List<Step> steps = new ArrayList<Step>();
        
        for (int i = 0; i < tempVarNames.size(); i++) {
            steps.add(new Step(tempVarNames.get(i), tempExpressions.get(i)));
        }
        
        steps.add(new Step(null, finalExpr));
        
        return new SequenceFormula(start, end, indexVar, steps);
    }
    
    public int getStepCount() {
        return steps.size();
    }
    
    public boolean isSimple() {
        return steps.size() == 1 && steps.get(0).isFinal();
    }
    
    public List<String> getTempVarNames() {
        List<String> names = new ArrayList<String>();
        for (Step step : steps) {
            if (step.tempVar != null) {
                names.add(step.tempVar);
            }
        }
        return names;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SequenceFormula[%d to %d]: ", start, end));
        
        if (isSimple()) {
            sb.append("simple: ").append(steps.get(0).expression);
        } else {
            sb.append("steps=").append(steps.size());
            sb.append(", temps=").append(getTempVarNames());
        }
        
        return sb.toString();
    }
}