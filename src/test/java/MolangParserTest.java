import gg.moonflower.molangcompiler.api.MolangCompiler;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangRuntime;
import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.exception.MolangException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive parser tests covering edge cases, operator precedence, and complex expressions.
 */
public class MolangParserTest {

    @Test
    void testOperatorPrecedence() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Multiplication before addition
        MolangExpression expr1 = compiler.compile("2 + 3 * 4");
        Assertions.assertEquals(14.0f, runtime.resolve(expr1).asFloat());

        // Division before subtraction
        MolangExpression expr2 = compiler.compile("10 - 8 / 2");
        Assertions.assertEquals(6.0f, runtime.resolve(expr2).asFloat());

        // Parentheses override precedence
        MolangExpression expr3 = compiler.compile("(2 + 3) * 4");
        Assertions.assertEquals(20.0f, runtime.resolve(expr3).asFloat());

        // Multiple levels
        MolangExpression expr4 = compiler.compile("2 + 3 * 4 - 5 / 5");
        Assertions.assertEquals(13.0f, runtime.resolve(expr4).asFloat());
    }

    @Test
    void testChainedComparisons() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // AND with parentheses to handle precedence
        MolangExpression expr1 = compiler.compile("(1 < 2) && (3 < 4)");
        Assertions.assertEquals(1.0f, runtime.resolve(expr1).asFloat());

        MolangExpression expr2 = compiler.compile("(1 > 2) && (3 < 4)");
        Assertions.assertEquals(0.0f, runtime.resolve(expr2).asFloat());

        // OR with comparisons
        MolangExpression expr3 = compiler.compile("(1 > 2) || (3 < 4)");
        Assertions.assertEquals(1.0f, runtime.resolve(expr3).asFloat());

        MolangExpression expr4 = compiler.compile("(1 > 2) || (3 > 4)");
        Assertions.assertEquals(0.0f, runtime.resolve(expr4).asFloat());
    }

    @Test
    void testLogicalOperators() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("true_val", 1.0f)
                .setQuery("false_val", 0.0f)
                .create();

        // Simple AND with parentheses
        MolangExpression expr1 = compiler.compile("(q.false_val) && (q.true_val)");
        Assertions.assertEquals(0.0f, runtime.resolve(expr1).asFloat());

        // Simple OR with parentheses
        MolangExpression expr2 = compiler.compile("(q.true_val) || (q.false_val)");
        Assertions.assertEquals(1.0f, runtime.resolve(expr2).asFloat());

        // AND with true values
        MolangExpression expr3 = compiler.compile("(q.true_val) && (q.true_val)");
        Assertions.assertEquals(1.0f, runtime.resolve(expr3).asFloat());
    }

    @Test
    void testNestedTernary() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Nested ternary operators
        MolangExpression expr1 = compiler.compile("1 > 2 ? 10 : 3 > 4 ? 20 : 30");
        Assertions.assertEquals(30.0f, runtime.resolve(expr1).asFloat());

        MolangExpression expr2 = compiler.compile("1 < 2 ? 10 : 3 > 4 ? 20 : 30");
        Assertions.assertEquals(10.0f, runtime.resolve(expr2).asFloat());

        // Nested with parentheses
        MolangExpression expr3 = compiler.compile("1 < 2 ? (3 < 4 ? 10 : 20) : 30");
        Assertions.assertEquals(10.0f, runtime.resolve(expr3).asFloat());
    }

    @Test
    void testNullCoalescing() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("condition", 42.0f)
                .create();

        // Variable exists
        MolangExpression expr1 = compiler.compile("q.condition ?? 0");
        Assertions.assertEquals(42.0f, runtime.resolve(expr1).asFloat());

        // Variable doesn't exist
        MolangExpression expr2 = compiler.compile("q.nonexistent ?? 100");
        Assertions.assertEquals(100.0f, runtime.resolve(expr2).asFloat());

        // Chained null coalescing
        MolangExpression expr3 = compiler.compile("q.a ?? q.b ?? 99");
        Assertions.assertEquals(99.0f, runtime.resolve(expr3).asFloat());
    }

    @Test
    void testSimpleLoop() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangExpression expr = compiler.compile("""
                temp.sum = 0;
                loop(6, {
                    temp.sum = temp.sum + 1;
                });
                return temp.sum;
                """);

        Assertions.assertEquals(6.0f, runtime.resolve(expr).asFloat());
    }

    @Test
    void testLoopWithBreak() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Test loop with break instead of return
        MolangExpression expr = compiler.compile("""
                temp.i = 0;
                loop(10, {
                    temp.i = temp.i + 1;
                    if(temp.i == 5) {
                        break;
                    }
                });
                return temp.i;
                """);

        Assertions.assertEquals(5.0f, runtime.resolve(expr).asFloat());
    }

    @Test
    void testComplexIfElseChain() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("condition", 15.0f)
                .create();

        MolangExpression expr = compiler.compile("""
                q.condition < 10 ? 1 : (q.condition < 20 ? 2 : 3)
                """);

        Assertions.assertEquals(2.0f, runtime.resolve(expr).asFloat());
    }

    @Test
    void testCompoundAssignmentOperators() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangExpression expr = compiler.compile("""
                temp.val = 10;
                temp.val += 5;
                temp.val -= 3;
                temp.val *= 2;
                temp.val /= 4;
                return temp.val;
                """);

        // (((10 + 5) - 3) * 2) / 4 = 6
        Assertions.assertEquals(6.0f, runtime.resolve(expr).asFloat());
    }

    @Test
    void testIncrementDecrement() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangExpression expr = compiler.compile("""
                temp.val = 10;
                temp.val++;
                temp.val++;
                temp.val--;
                return temp.val;
                """);

        Assertions.assertEquals(11.0f, runtime.resolve(expr).asFloat());
    }

    @Test
    void testDecimalNumbers() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangExpression expr1 = compiler.compile("3.14159");
        Assertions.assertEquals(3.14159f, runtime.resolve(expr1).asFloat(), 0.00001f);

        MolangExpression expr2 = compiler.compile("0.5 + 0.25");
        Assertions.assertEquals(0.75f, runtime.resolve(expr2).asFloat(), 0.00001f);

        MolangExpression expr3 = compiler.compile("1.0 / 3.0");
        Assertions.assertEquals(0.333333f, runtime.resolve(expr3).asFloat(), 0.00001f);
    }

    @Test
    void testNegativeNumbers() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangExpression expr1 = compiler.compile("-5");
        Assertions.assertEquals(-5.0f, runtime.resolve(expr1).asFloat());

        MolangExpression expr2 = compiler.compile("-5 + 10");
        Assertions.assertEquals(5.0f, runtime.resolve(expr2).asFloat());

        MolangExpression expr3 = compiler.compile("10 - -5");
        Assertions.assertEquals(15.0f, runtime.resolve(expr3).asFloat());

        MolangExpression expr4 = compiler.compile("-5 * -5");
        Assertions.assertEquals(25.0f, runtime.resolve(expr4).asFloat());
    }

    @Test
    void testUnaryPlus() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangExpression expr1 = compiler.compile("+5");
        Assertions.assertEquals(5.0f, runtime.resolve(expr1).asFloat());

        MolangExpression expr2 = compiler.compile("+5 + 10");
        Assertions.assertEquals(15.0f, runtime.resolve(expr2).asFloat());
    }

    @Test
    void testLogicalNegation() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangExpression expr1 = compiler.compile("0 != 0");
        Assertions.assertEquals(0.0f, runtime.resolve(expr1).asFloat());

        MolangExpression expr2 = compiler.compile("1 != 0");
        Assertions.assertEquals(1.0f, runtime.resolve(expr2).asFloat());

        MolangExpression expr3 = compiler.compile("(1 > 2) != 1");
        Assertions.assertEquals(1.0f, runtime.resolve(expr3).asFloat());

        MolangExpression expr4 = compiler.compile("!0");
        Assertions.assertEquals(1.0f, runtime.resolve(expr4).asFloat());

        MolangExpression expr5 = compiler.compile("!1");
        Assertions.assertEquals(0.0f, runtime.resolve(expr5).asFloat());

        MolangExpression expr6 = compiler.compile("!(1 > 2)");
        Assertions.assertEquals(1.0f, runtime.resolve(expr6).asFloat());
    }

    @Test
    void testBinaryConditional() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime1 = MolangRuntime.runtime()
                .setVariable("test", true)
                .setVariable("moo", 1.0f)
                .create();
        MolangRuntime runtime2 = MolangRuntime.runtime()
                .setVariable("test", false)
                .setVariable("moo", 0.0f)
                .create();

        MolangExpression expr1 = compiler.compile("v.test ? (v.my_var = 10.0); return v.my_var ?? 0.0;");
        Assertions.assertEquals(10.0f, runtime1.resolve(expr1).asFloat());
        Assertions.assertEquals(0.0f, runtime2.resolve(expr1).asFloat());

        MolangExpression expr2 = compiler.compile("""
                (v.test) ? {
                    v.my_var = 10.0;
                };
                return v.my_var;
                """);
        Assertions.assertEquals(10.0f, runtime1.resolve(expr2).asFloat());
        Assertions.assertEquals(0.0f, runtime2.resolve(expr2).asFloat());

        MolangExpression expr3 = compiler.compile("""
                (v.moo > 0) ? {
                    v.x = math.sin(q.life_time * 45);
                    v.x = v.x * v.x + 17.3;
                    t.sin_x = math.sin(v.x);
                    v.x = t.sin_x * t.sin_x + v.x * v.x;
                    v.x = math.sqrt(v.x) * v.x * math.pi;
                }
                return v.x;
                """);
        Assertions.assertEquals(16273.487f, runtime1.resolve(expr3).asFloat());
        Assertions.assertEquals(0.0f, runtime2.resolve(expr3).asFloat());
    }

    @Test
    void testDotNotationInVariableNames() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("is.valid.name", 42.0f)
                .create();

        MolangExpression expr = compiler.compile("q.is.valid.name");
        Assertions.assertEquals(42.0f, runtime.resolve(expr).asFloat());
    }

    @Test
    void testUnderscoresInNames() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("my_variable_name", 100.0f)
                .create();

        MolangExpression expr = compiler.compile("q.my_variable_name");
        Assertions.assertEquals(100.0f, runtime.resolve(expr).asFloat());
    }

    @Test
    void testNumbersInVariableNames() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("var123test", 77.0f)
                .create();

        MolangExpression expr = compiler.compile("q.var123test");
        Assertions.assertEquals(77.0f, runtime.resolve(expr).asFloat());
    }

    @Test
    void testThisKeyword() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();
        runtime.setThisValue(MolangValue.of(99.0f));

        MolangExpression expr = compiler.compile("this");
        Assertions.assertEquals(99.0f, runtime.resolve(expr).asFloat());
    }

    @Test
    void testEmptyParentheses() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangExpression expr = compiler.compile("(((5)))");
        Assertions.assertEquals(5.0f, runtime.resolve(expr).asFloat());
    }

    @Test
    void testComplexExpression() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("a", 2.0f)
                .setQuery("b", 3.0f)
                .setQuery("c", 5.0f)
                .create();

        // (2 + 3) * 5 - (3 / 2) = 25 - 1.5 = 23.5
        MolangExpression expr = compiler.compile("(q.a + q.b) * q.c - (q.b / 2)");
        Assertions.assertEquals(23.5f, runtime.resolve(expr).asFloat(), 0.00001f);
    }

    @Test
    void testMultilineExpressions() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangExpression expr = compiler.compile("""
                temp.line1 = 1;
                temp.line2 = 2;
                temp.line3 = 3;
                temp.line1 + temp.line2 + temp.line3
                """);

        Assertions.assertEquals(6.0f, runtime.resolve(expr).asFloat());
    }

    @Test
    void testVariableAliases() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("test", 10.0f)
                .setVariable("test", MolangExpression.of(20.0f))
                .create();

        // Test query alias (q = query)
        MolangExpression expr1 = compiler.compile("q.test");
        Assertions.assertEquals(10.0f, runtime.resolve(expr1).asFloat());

        // Test variable alias (v = variable)
        MolangExpression expr2 = compiler.compile("v.test");
        Assertions.assertEquals(20.0f, runtime.resolve(expr2).asFloat());

        // Test temp alias (t = temp)
        MolangExpression expr3 = compiler.compile("t.x = 5; t.x");
        Assertions.assertEquals(5.0f, runtime.resolve(expr3).asFloat());
    }

    @Test
    void testComparisonOperators() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(1.0f, runtime.resolve(compiler.compile("5 > 3")).asFloat());
        Assertions.assertEquals(0.0f, runtime.resolve(compiler.compile("5 > 5")).asFloat());
        Assertions.assertEquals(1.0f, runtime.resolve(compiler.compile("5 >= 5")).asFloat());
        Assertions.assertEquals(1.0f, runtime.resolve(compiler.compile("3 < 5")).asFloat());
        Assertions.assertEquals(0.0f, runtime.resolve(compiler.compile("5 < 5")).asFloat());
        Assertions.assertEquals(1.0f, runtime.resolve(compiler.compile("5 <= 5")).asFloat());
        Assertions.assertEquals(1.0f, runtime.resolve(compiler.compile("5 == 5")).asFloat());
        Assertions.assertEquals(0.0f, runtime.resolve(compiler.compile("5 == 3")).asFloat());
        Assertions.assertEquals(1.0f, runtime.resolve(compiler.compile("5 != 3")).asFloat());
        Assertions.assertEquals(0.0f, runtime.resolve(compiler.compile("5 != 5")).asFloat());
    }

    @Test
    void testSemicolonInsertion() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Semicolons should be automatically inserted after closing braces
        MolangExpression expr = compiler.compile("""
                temp.a = { 1 }
                temp.b = { 2 }
                temp.a + temp.b
                """);

        Assertions.assertEquals(3.0f, runtime.resolve(expr).asFloat());
    }

    @Test
    void testNullCoalescingLoop() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression loop = compiler.compile("loop(5, {v.test ?? 4})");
        MolangRuntime runtime = MolangRuntime.runtime().create();
        Assertions.assertEquals(MolangValue.of(0.0f), runtime.resolve(loop));
    }
}
