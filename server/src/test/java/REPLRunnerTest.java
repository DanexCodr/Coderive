import cod.runner.REPLRunner;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for REPLRunner.eval() — the core evaluation logic used by the
 * Coderive playground (both local CheerpJ execution and the remote API).
 */
public class REPLRunnerTest {

    @Before
    public void resetState() {
        REPLRunner.reset();
    }

    @Test
    public void testArithmetic() {
        assertEquals("8", REPLRunner.eval("5 + 3").trim());
    }

    @Test
    public void testMultiplication() {
        assertEquals("21", REPLRunner.eval("3 * 7").trim());
    }

    @Test
    public void testVariableDeclarationAndAccess() {
        REPLRunner.eval("x := 42");
        assertEquals("42", REPLRunner.eval("x").trim());
    }

    @Test
    public void testStringLiteral() {
        assertEquals("hello", REPLRunner.eval("\"hello\"").trim());
    }

    @Test
    public void testStringConcatenation() {
        assertEquals("hello world", REPLRunner.eval("\"hello\" + \" world\"").trim());
    }

    @Test
    public void testBooleanGreaterThan() {
        assertEquals("true", REPLRunner.eval("5 > 3").trim());
    }

    @Test
    public void testBooleanLessThan() {
        assertEquals("false", REPLRunner.eval("5 < 3").trim());
    }

    @Test
    public void testResetClearsVariables() {
        REPLRunner.eval("myVar := 99");
        assertEquals("99", REPLRunner.eval("myVar").trim());
        REPLRunner.reset();
        String result = REPLRunner.eval("myVar");
        assertTrue("Expected error after reset, got: " + result, result.contains("Error"));
    }

    @Test
    public void testResetCommand() {
        REPLRunner.eval("y := 10");
        assertEquals("State reset.", REPLRunner.eval(";reset").trim());
        String afterReset = REPLRunner.eval("y");
        assertTrue("Expected error after ;reset, got: " + afterReset, afterReset.contains("Error"));
    }

    @Test
    public void testEmptyInput() {
        assertEquals("", REPLRunner.eval(""));
    }

    @Test
    public void testNullInput() {
        assertEquals("", REPLRunner.eval(null));
    }

    @Test
    public void testVariablePersistsAcrossCalls() {
        REPLRunner.eval("counter := 10");
        REPLRunner.eval("counter = counter + 5");
        assertEquals("15", REPLRunner.eval("counter").trim());
    }
}
