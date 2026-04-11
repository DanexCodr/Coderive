package cod.interpreter;

import cod.interpreter.context.LambdaClosure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TailCallSignal extends RuntimeException {
    public final String methodName;
    public final LambdaClosure lambdaClosure;
    public final List<Object> arguments;

    private TailCallSignal(String methodName, LambdaClosure lambdaClosure, List<Object> arguments) {
        this.methodName = methodName;
        this.lambdaClosure = lambdaClosure;
        this.arguments = arguments != null ? new ArrayList<Object>(arguments) : Collections.<Object>emptyList();
    }

    public static TailCallSignal forMethod(String methodName, List<Object> arguments) {
        return new TailCallSignal(methodName, null, arguments);
    }

    public static TailCallSignal forLambda(LambdaClosure lambdaClosure, List<Object> arguments) {
        return new TailCallSignal(null, lambdaClosure, arguments);
    }
}
