import com.google.common.base.Stopwatch;
import gg.moonflower.molangcompiler.api.MolangCompiler;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangRuntime;
import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.bridge.MolangVariable;
import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public class MolangTest {

    @Test
    void testSpeed() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        compiler.compile("0"); // load

        Stopwatch compileTime = Stopwatch.createStarted();
        MolangExpression expression =
//                MolangCompiler.compile("2");
//                MolangCompiler.compile("temp.my_temp_var = math.sin(90) / 2");
//        MolangCompiler.compile("return math.sin(global.anim_time * 1.23)");
//        MolangCompiler.compile("math.sin(global.anim_time * 1.23)");
//        MolangCompiler.compile("(math.cos(query.life_time * 20.0 * 10.89) * 28.65) + (math.sin(variable.attack_time * 180.0) * 68.76 - (math.sin((1.0 - (1.0 - variable.attack_time) * (1.0 - variable.attack_time)) * 180.0)) * 22.92)");
//        MolangCompiler.compile("temp.my_temp_var = Math.sin(query.anim_time * 1.23);\n" +
//                "temp.my_other_temp_var = Math.cos(query.life_time + 2.0);\n" +
//                "return temp.my_temp_var * temp.my_temp_var + temp.my_other_temp_var;");
                compiler.compile("""
                        math.trunc(math.pi);
                        v.test = q.anim_time;
                        v.test/=30;
                        v.test--;
                        v.test--;
                        v.test--;
                        v.test+=3;
                        return v.test;
                        """);
        compileTime.stop();

        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("anim_time", 90)
                .setQuery("life_time", 0)
                .create();

        System.out.println(expression);

        int iterations = 10000;
        long[] times = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            Stopwatch runTime = Stopwatch.createStarted();
            float result = runtime.resolve(expression).asFloat();
            runTime.stop();

            Assertions.assertEquals(3, result);
            times[i] = runTime.elapsed(TimeUnit.NANOSECONDS);
        }

        System.out.println("Took " + compileTime + " to compile, " + Arrays.stream(times).average().orElse(0) + "ns to execute " + iterations + " times");
    }

    @Test
    void testSimplify() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("math.pi*2+(3/2+53)*((7)/5)");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(82.58318328857422, result);
    }

    @Test
    void testScopes() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("""
                        temp.a = 4;
                        temp.b = 4;
                        {
                        temp.c = 1;
                        }
                        temp.d = 5;
                        return temp.d * temp.b;
                """);

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(20, result);
    }

    @Test
    void testReturnScopes() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("""
                        temp.a = 4;
                        temp.b = 2;
                        temp.c = {
                        4;
                        };
                        return {
                        {temp.a * temp.b;};
                        };
                """);

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(8, result);
    }

    @Test
    void testRandom() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("math.die_roll(1, 0, 10)");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
    }

    MolangVariable testVariable = MolangVariable.create(7);

    @Test
    void testGetVariable() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("""
                t.a = 14;
                return v.test + t.a;
                """);

        MolangRuntime runtime = MolangRuntime.runtime()
                .setVariable("test", this.testVariable).create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(21, result);
    }

    @Test
    void testSetVariable() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("""
                v.test = 2;
                """);

        MolangRuntime runtime = MolangRuntime.runtime()
                .setVariable("test", this.testVariable).create();
        MolangValue result = runtime.resolve(expression);
        MolangValue expectedValue = MolangValue.of(2.0f);
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(expectedValue, result);
        Assertions.assertEquals(expectedValue, this.testVariable.getValue());
    }

    @Test
    void testMultiple() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("v.b = 2;v.a = 3;v.ab = v.b;v.c = 1;");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
    }

    @Test
    void testCondition() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create(0);
        MolangExpression expression = compiler.compile("1 > 2 ? 10 : 20");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(20, result);
    }

    @Test
    void testComplexCondition() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("math.clamp(0.5 + variable.particle_random_4/7 + (variable.particle_random_3>0.2 ? 0.4 : 0), 0, 1)");

        MolangRuntime runtime = MolangRuntime.runtime()
                .setVariable("particle_random_1", MolangExpression.ZERO)
                .setVariable("particle_random_2", MolangExpression.ZERO)
                .setVariable("particle_random_3", MolangExpression.ZERO)
                .setVariable("particle_random_4", MolangExpression.ZERO)
                .create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(0.5, result);
    }

    @Test
    void testNegativeCondition() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("+variable.particle_random_3??0>0.2 ? -10 : -4");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(-4, result);
    }

    @Test
    void testNegativeCondition2() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("+variable.particle_random_3??0.3>0.2 ? -10 : -4");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(-10, result);
    }

    @Test
    void testWeird() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("((((-7))*((((((((variable.particle_random_3>(0.2) * (((4))) ? (-10) : -4))))))))))");

        MolangRuntime runtime = MolangRuntime.runtime()
                .setVariable("particle_random_3", MolangExpression.ZERO)
                .create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(28, result);
    }

    @Test
    void testCopy() {
        MolangRuntime.Builder builder1 = (MolangRuntime.Builder) MolangRuntime.runtime().setVariable("test", MolangVariable.create(10));
        MolangRuntime.Builder builder2 = (MolangRuntime.Builder) MolangRuntime.runtime(builder1).setVariable("test2", MolangVariable.create(100));

        System.out.println("builder1");
        System.out.println(builder1.create().dump());
        System.out.println("builder2");
        System.out.println(builder2.create().dump());
    }

    @Test
    void testContainer() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("v.screen_aspect_ratio > v.aspect_ratio ? q.screen.width : q.screen.height * v.aspect_ratio");

        MolangRuntime runtime = MolangRuntime.runtime()
                .setVariable("screen_aspect_ratio", MolangExpression.of(7))
                .setVariable("aspect_ratio", MolangExpression.of(2))
                .setQuery("screen.width", MolangExpression.of(12))
                .setQuery("screen.height", MolangExpression.of(12))
                .create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(12, result);
    }

    @Test
    void testImmutable() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangVariable test = MolangVariable.create(1.0f);
        MolangVariable testImmutable = test.immutable();

        MolangExpression expression = compiler.compile("v.test=2");

        MolangRuntime runtime = MolangRuntime.runtime().setVariable("test", testImmutable).create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(MolangValue.of(1.0f), test.getValue());
    }

    @Test
    void testCompare() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("5<5");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(0.0f, result);
    }

    @Test
    void testSign() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("math.sign(-4)");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(-1.0f, result);
    }

    @Test
    void testTriangleWave() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("math.triangle_wave(23544/2, 23544)");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(-1.0f, result);
    }

    @Test
    void testEqualsHashCode() throws MolangSyntaxException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression1 = compiler.compile("q.test");
        MolangExpression expression2 = compiler.compile("q.test");

        Assertions.assertEquals(expression1, expression2);
        Assertions.assertEquals(expression1.hashCode(), expression2.hashCode());
        Assertions.assertNotEquals("q.test", expression1);
        Assertions.assertNotEquals("q.test", expression2);
    }

    @Test
    void testAState() throws MolangSyntaxException {
        MolangCompiler compiler = MolangCompiler.create();
        compiler.compile("query.is_gliding == 1.0 ? 1.0 : 0.0");
        compiler.compile("query.is_gliding");
    }

    @Test
    void testBState() throws MolangSyntaxException {
        MolangCompiler compiler = MolangCompiler.create();
        compiler.compile("test.a ? 4 : 0");
    }

    @Test
    void testJavaCondition() throws MolangException {
        record Condition(String action, BiFunction<MolangValue, MolangValue, MolangValue> match) {

        }
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("==", (a, b) -> a.internalEquals(b)));
        conditions.add(new Condition("!=", (a, b) -> a.internalNotEquals(b)));
        conditions.add(new Condition(">", (a, b) -> a.internalGreater(b)));
        conditions.add(new Condition(">=", (a, b) -> a.internalGreaterEquals(b)));
        conditions.add(new Condition("<", (a, b) -> a.internalLess(b)));
        conditions.add(new Condition("<=", (a, b) -> a.internalLessEquals(b)));

        MolangCompiler compiler = MolangCompiler.create();

        MolangRuntime runtime = MolangRuntime.runtime().create();
        MolangValue b = MolangValue.of(0.0f);
        for (int i = -4; i < 6; i++) {
            runtime.edit().setQuery("climb_vertical", (float) i);
            for (Condition condition : conditions) {
                String query = "query.climb_vertical " + condition.action + " 0.0";
                MolangExpression expression = compiler.compile(query);
                MolangValue result = expression.get(runtime);
                MolangValue a = MolangValue.of(i);
                Assertions.assertEquals(condition.match.apply(a, b), result);
            }
        }
    }

    @Test
    void testTrueFalse() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression trueExpression = compiler.compile("true");
        MolangExpression falseExpression = compiler.compile("false");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        Assertions.assertEquals(MolangValue.of(true), runtime.resolve(trueExpression));
        Assertions.assertEquals(MolangValue.of(false), runtime.resolve(falseExpression));
    }

    @Test
    void testLoop() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression loop = compiler.compile("""
                temp.i = 0;
                loop(10, {
                    temp.i++;
                    if(temp.i > 5) {
                        temp.i+=3;
                        break;
                    } else {
                        temp.i+=2;
                        continue;
                    }
                });
                temp.i;
                """);

        MolangRuntime runtime = MolangRuntime.runtime().create();
        Assertions.assertEquals(MolangValue.of(10.0f), runtime.resolve(loop));
    }

    @Test
    void testNullCoalescingLoop() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression loop = compiler.compile("""
                loop(5, {v.test += v.test ?? 4});
                return v.test;
                """);

        MolangRuntime runtime = MolangRuntime.runtime().create();
        Assertions.assertEquals(MolangValue.of(64.0f), runtime.resolve(loop));
    }

    @Test
    void testIf() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression loop = compiler.compile("""
                if(true) {
                    return 4;
                }
                return 1;
                """);

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(loop).asFloat();
        Assertions.assertEquals(4, result);
    }

    @Test
    void testCamelCase() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression loop = compiler.compile("q.testCamel");

        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("testCamel", 4)
                .create();
        float result = runtime.resolve(loop).asFloat();
        Assertions.assertEquals(4, result);
    }

    @Test
    void testStringLiteral() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("\"hello world\"");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(MolangValue.of("hello world"), result);
    }

    @Test
    void testStringWithEscapes() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("\"hello \\\"world\\\"\"");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(MolangValue.of("hello \"world\""), result);
    }

    @Test
    void testSingleQuotedString() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("'hello world'");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(MolangValue.of("hello world"), result);
    }

    @Test
    void testStringEqualityTrue() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("\"purple\" == \"purple\"");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(1.0f, result);
    }

    @Test
    void testStringEqualityFalse() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("\"purple\" == \"blue\"");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(0.0f, result);
    }

    @Test
    void testStringInequalityTrue() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("'red' != 'blue'");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(1.0f, result);
    }

    @Test
    void testStringInequalityFalse() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("'red' != 'red'");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        float result = runtime.resolve(expression).asFloat();
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(0.0f, result);
    }

    @Test
    void testStringFunctionParameter() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression echoFunction = MolangExpression.function(1, ctx -> ctx.get(0));

        MolangExpression expression = compiler.compile("query.test_func('test')");

        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("test_func", echoFunction)
                .create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(MolangValue.of("test"), result);
    }

    @Test
    void testComplexStringExpression() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();

        MolangValue teleporterColorValue = MolangValue.of("block_color");
        MolangValue purpleValue = MolangValue.of("purple");
        MolangExpression blockStateFunction = MolangExpression.function(1, ctx -> {
            MolangValue param = ctx.get(0);
            if (param.equalsValue(teleporterColorValue)) {
                return purpleValue;
            }
            return MolangValue.NULL;
        });

        MolangExpression expression = compiler.compile("query.block_state('block_color') == 'purple'");

        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("block_state", blockStateFunction)
                .create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(MolangValue.of(true), result);
    }

    @Test
    void testArrayLiteral() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("[1, 2, 3]");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);

        Assertions.assertTrue(result.isArray());
        MolangValue[] array = result.getArray();
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals(1.0f, array[0].asFloat());
        Assertions.assertEquals(2.0f, array[1].asFloat());
        Assertions.assertEquals(3.0f, array[2].asFloat());
    }

    @Test
    void testArrayAccess() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("[10, 20, 30][1]");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);

        Assertions.assertEquals(20.0f, result.asFloat());
    }

    @Test
    void testArrayWithStrings() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("[\"hello\", \"world\", \"test\"]");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);

        Assertions.assertTrue(result.isArray());
        MolangValue[] array = result.getArray();
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals("hello", array[0].getString());
        Assertions.assertEquals("world", array[1].getString());
        Assertions.assertEquals("test", array[2].getString());
    }

    @Test
    void testArrayIndexWithExpression() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("[100, 200, 300][1 + 1]");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);

        Assertions.assertEquals(300.0f, result.asFloat());
    }

    @Test
    void testEmptyArray() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("[]");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);

        Assertions.assertTrue(result.isArray());
        Assertions.assertEquals(0, result.getArray().length);
    }

    @Test
    void testNestedArrayAccess() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("[[1, 2], [3, 4], [5, 6]][1][0]");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);

        Assertions.assertEquals(3.0f, result.asFloat());
    }

    @Test
    void testArrayWithExpressions() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("[1 + 1, 2 * 3, 10 - 4]");

        MolangRuntime runtime = MolangRuntime.runtime().create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);

        Assertions.assertTrue(result.isArray());
        MolangValue[] array = result.getArray();
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals(2.0f, array[0].asFloat());
        Assertions.assertEquals(6.0f, array[1].asFloat());
        Assertions.assertEquals(6.0f, array[2].asFloat());
    }

    @Test
    void testArrayStoredInVariable() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("""
                v.my_array = [10, 20, 30, 40, 50];
                return v.my_array[2];
                """);

        MolangRuntime runtime = MolangRuntime.runtime().create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);

        Assertions.assertEquals(MolangValue.of(30.0f), result);
    }

    @Test
    void testArrayInVariableMultipleAccesses() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("""
                v.colors = ["red", "green", "blue"];
                v.first = v.colors[0];
                v.second = v.colors[1];
                v.third = v.colors[2];
                return v.second;
                """);

        MolangRuntime runtime = MolangRuntime.runtime().create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);

        Assertions.assertEquals(MolangValue.of("green"), result);
    }

    @Test
    void testArrayNegativeIndexClamping() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();

        // Negative indices should be clamped to 0
        MolangExpression expr1 = compiler.compile("[100, 200, 300][-1]");
        MolangExpression expr2 = compiler.compile("[100, 200, 300][-100]");

        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangValue result1 = runtime.resolve(expr1);
        System.out.println(expr1 + "\n==RESULT==\n" + result1);
        Assertions.assertEquals(MolangValue.of(100.0f), result1, "Index -1 should clamp to 0");

        MolangValue result2 = runtime.resolve(expr2);
        System.out.println(expr2 + "\n==RESULT==\n" + result2);
        Assertions.assertEquals(MolangValue.of(100.0f), result2, "Index -100 should clamp to 0");
    }

    @Test
    void testArrayOutOfBoundsWrapping() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();

        // Array has length 3, so index 3 wraps to 0, index 4 wraps to 1, etc.
        MolangExpression expr1 = compiler.compile("[10, 20, 30][3]");
        MolangExpression expr2 = compiler.compile("[10, 20, 30][4]");
        MolangExpression expr3 = compiler.compile("[10, 20, 30][5]");
        MolangExpression expr4 = compiler.compile("[10, 20, 30][10]");

        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangValue result1 = runtime.resolve(expr1);
        System.out.println(expr1 + "\n==RESULT==\n" + result1);
        Assertions.assertEquals(MolangValue.of(10.0f), result1, "Index 3 should wrap to 0");

        MolangValue result2 = runtime.resolve(expr2);
        System.out.println(expr2 + "\n==RESULT==\n" + result2);
        Assertions.assertEquals(MolangValue.of(20.0f), result2, "Index 4 should wrap to 1");

        MolangValue result3 = runtime.resolve(expr3);
        System.out.println(expr3 + "\n==RESULT==\n" + result3);
        Assertions.assertEquals(MolangValue.of(30.0f), result3, "Index 5 should wrap to 2");

        MolangValue result4 = runtime.resolve(expr4);
        System.out.println(expr4 + "\n==RESULT==\n" + result4);
        Assertions.assertEquals(MolangValue.of(20.0f), result4, "Index 10 should wrap to 1 (10 % 3 = 1)");
    }

    @Test
    void testArrayFloatIndexCasting() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();

        // Float indices should be cast to int (truncated)
        MolangExpression expr1 = compiler.compile("[100, 200, 300][0.9]");
        MolangExpression expr2 = compiler.compile("[100, 200, 300][1.1]");
        MolangExpression expr3 = compiler.compile("[100, 200, 300][1.9]");
        MolangExpression expr4 = compiler.compile("[100, 200, 300][2.5]");

        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangValue result1 = runtime.resolve(expr1);
        System.out.println(expr1 + "\n==RESULT==\n" + result1);
        Assertions.assertEquals(MolangValue.of(100.0f), result1, "Index 0.9 should truncate to 0");

        MolangValue result2 = runtime.resolve(expr2);
        System.out.println(expr2 + "\n==RESULT==\n" + result2);
        Assertions.assertEquals(MolangValue.of(200.0f), result2, "Index 1.1 should truncate to 1");

        MolangValue result3 = runtime.resolve(expr3);
        System.out.println(expr3 + "\n==RESULT==\n" + result3);
        Assertions.assertEquals(MolangValue.of(200.0f), result3, "Index 1.9 should truncate to 1");

        MolangValue result4 = runtime.resolve(expr4);
        System.out.println(expr4 + "\n==RESULT==\n" + result4);
        Assertions.assertEquals(MolangValue.of(300.0f), result4, "Index 2.5 should truncate to 2");
    }

    @Test
    void testArrayVariableWithWrapping() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangExpression expression = compiler.compile("""
                v.numbers = [5, 10, 15, 20];
                v.a = v.numbers[0];
                v.b = v.numbers[4];
                v.c = v.numbers[7];
                v.d = v.numbers[-5];
                return v.a + v.b + v.c + v.d;
                """);

        MolangRuntime runtime = MolangRuntime.runtime().create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);

        Assertions.assertEquals(MolangValue.of(35.0f), result);
    }

}
