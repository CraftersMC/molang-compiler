import gg.moonflower.molangcompiler.api.MolangCompiler;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangRuntime;
import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for error handling and exception cases.
 */
public class MolangErrorTest {

    @Test
    void testEmptyExpression() {
        MolangCompiler compiler = MolangCompiler.create();

        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("");
        });
    }

    @Test
    void testUnknownToken() {
        MolangCompiler compiler = MolangCompiler.create();

        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("@#$%");
        });
    }

    @Test
    void testUnmatchedParentheses() {
        MolangCompiler compiler = MolangCompiler.create();

        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("(5 + 3");
        });

        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("5 + 3)");
        });
    }

    @Test
    void testUnmatchedBraces() {
        MolangCompiler compiler = MolangCompiler.create();

        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("{temp.a = 5");
        });

        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("temp.a = 5}");
        });
    }

    @Test
    void testInvalidVariableName() {
        MolangCompiler compiler = MolangCompiler.create();

        // Missing object prefix
        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("temp.");
        });
    }

    @Test
    void testTrailingOperator() {
        MolangCompiler compiler = MolangCompiler.create();

        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("5 +");
        });

        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("5 + 3 *");
        });
    }

    @Test
    void testDoubleOperator() {
        MolangCompiler compiler = MolangCompiler.create();

        // Note: Some cases like ++ and -- are valid, but most double operators are not
        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("5 ++ 3");
        });
    }

    @Test
    void testInvalidFunctionCall() {
        MolangCompiler compiler = MolangCompiler.create();

        // Missing closing parenthesis
        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("math.sin(5");
        });

        // Missing parameter
        Assertions.assertThrows(MolangException.class, () -> {
            compiler.compile("math.sin()");
        });
    }

    @Test
    void testUnknownMathFunction() {
        MolangCompiler compiler = MolangCompiler.create();

        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("math.unknown_function(5)");
        });
    }

    @Test
    void testWrongNumberOfParameters() {
        MolangCompiler compiler = MolangCompiler.create();

        // math.sin expects 1 parameter
        Assertions.assertThrows(MolangException.class, () -> {
            compiler.compile("math.sin(5, 10)");
        });

        // math.clamp expects 3 parameters
        Assertions.assertThrows(MolangException.class, () -> {
            compiler.compile("math.clamp(5, 10)");
        });
    }

    @Test
    void testIncompleteTernary() {
        MolangCompiler compiler = MolangCompiler.create();

        // Missing else clause
        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("1 > 2 ? 10 :");
        });
    }

    @Test
    void testBreakOutsideLoop() {
        // Break outside of a loop is caught at compile time
        MolangCompiler compiler = MolangCompiler.create();

        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("temp.a = 5; break; temp.a");
        });
    }

    @Test
    void testContinueOutsideLoop() {
        // Continue outside of a loop is caught at compile time
        MolangCompiler compiler = MolangCompiler.create();

        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("temp.a = 5; continue; temp.a");
        });
    }

    @Test
    void testInvalidLoopSyntax() {
        MolangCompiler compiler = MolangCompiler.create();

        // Loop requires 2 parameters
        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("loop(10)");
        });

        // Missing closing parenthesis
        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("loop(10, {temp.a++}");
        });
    }

    @Test
    void testInvalidIfSyntax() {
        MolangCompiler compiler = MolangCompiler.create();

        // Missing condition
        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("if()");
        });

        // Missing parentheses
        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("if 1 > 2 return 5");
        });
    }

    @Test
    void testStringQuoteMismatch() {
        MolangCompiler compiler = MolangCompiler.create();

        // Mismatched quotes
        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("\"hello'");
        });
    }

    @Test
    void testUnterminatedString() {
        MolangCompiler compiler = MolangCompiler.create();

        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("\"hello");
        });

        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("'hello");
        });
    }

    @Test
    void testInvalidAssignment() {
        MolangCompiler compiler = MolangCompiler.create();

        // Cannot assign to a constant
        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("5 = 10");
        });

        // Cannot assign to math function
        Assertions.assertThrows(MolangException.class, () -> {
            compiler.compile("math.pi = 10");
        });
    }

    @Test
    void testInvalidIncrement() {
        MolangCompiler compiler = MolangCompiler.create();

        // Cannot increment a constant
        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("5++");
        });
    }

    @Test
    void testInvalidDecrement() {
        MolangCompiler compiler = MolangCompiler.create();

        // Cannot decrement a constant
        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("10--");
        });
    }

    @Test
    void testMultipleStatements() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Test multiple statements
        MolangExpression expr = compiler.compile("""
                temp.a = 5;
                temp.b = 10;
                temp.a + temp.b
                """);

        Assertions.assertEquals(15.0f, runtime.resolve(expr).asFloat());
    }

    @Test
    void testComplexSyntaxError() {
        MolangCompiler compiler = MolangCompiler.create();

        // Complex expression with syntax error
        Assertions.assertThrows(MolangSyntaxException.class, () -> {
            compiler.compile("""
                    temp.a = 5;
                    temp.b = temp.a + ;
                    return temp.b;
                    """);
        });
    }

    @Test
    void testNestedErrorLocation() {
        MolangCompiler compiler = MolangCompiler.create();

        try {
            compiler.compile("""
                    temp.a = 5;
                    temp.b = 10;
                    temp.c = temp.a + temp.b +;
                    """);
            Assertions.fail("Expected MolangSyntaxException");
        } catch (MolangSyntaxException e) {
            // Exception should contain location information
            Assertions.assertNotNull(e.getMessage());
            // The error message should indicate the problem
            Assertions.assertTrue(e.getMessage().contains("Trailing") ||
                    e.getMessage().contains("Expected") ||
                    e.getMessage().contains("Unexpected"));
        }
    }

    @Test
    void testDivisionByZeroAllowed() throws MolangException {
        // Division by zero should be allowed (returns Infinity in Java)
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangExpression expr = compiler.compile("5 / 0");
        float result = runtime.resolve(expr).asFloat();

        // Result should be infinity
        Assertions.assertTrue(Float.isInfinite(result));
    }

    @Test
    void testNegativeNumberParsing() throws MolangException {
        // Ensure negative numbers are parsed correctly
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangExpression expr1 = compiler.compile("-5");
        Assertions.assertEquals(-5.0f, runtime.resolve(expr1).asFloat());

        MolangExpression expr2 = compiler.compile("0 - -5");
        Assertions.assertEquals(5.0f, runtime.resolve(expr2).asFloat());
    }
}
