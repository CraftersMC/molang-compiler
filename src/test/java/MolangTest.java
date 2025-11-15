import com.google.common.base.Stopwatch;
import gg.moonflower.molangcompiler.api.MolangCompiler;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangRuntime;
import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.bridge.MolangVariable;
import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import gg.moonflower.molangcompiler.impl.CompilerFlags;
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
        MolangCompiler compiler = MolangCompiler.create(CompilerFlags.NONE);
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
        conditions.add(new Condition("==", MolangValue::internalEquals));
        conditions.add(new Condition("!=", MolangValue::internalNotEquals));
        conditions.add(new Condition(">", MolangValue::internalGreater));
        conditions.add(new Condition(">=", MolangValue::internalGreaterEquals));
        conditions.add(new Condition("<", MolangValue::internalLess));
        conditions.add(new Condition("<=", MolangValue::internalLessEquals));

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
    void testComplexStringExpression2() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();

        MolangValue wallPostBit = MolangValue.of("wall_post_bit");
        MolangValue wallConnectionTypeNorth = MolangValue.of("wall_connection_type_north");
        MolangValue wallConnectionTypeSouth = MolangValue.of("wall_connection_type_south");
        MolangValue falseValue = MolangValue.of(false);
        MolangValue tallValue = MolangValue.of("tall");
        MolangExpression blockStateFunction = MolangExpression.function(1, ctx -> {
            MolangValue param = ctx.get(0);
            if (param.equalsValue(wallConnectionTypeNorth) || param.equalsValue(wallConnectionTypeSouth)) {
                return tallValue;
            }
            if (param.equalsValue(wallPostBit)) {
                return falseValue;
            }
            return MolangValue.NULL;
        });

        MolangExpression expression = compiler.compile("!q.block_state('wall_post_bit') && q.block_state('wall_connection_type_north') == 'tall' && q.block_state('wall_connection_type_south') == 'tall'");

        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("block_state", blockStateFunction)
                .create();
        MolangValue result = runtime.resolve(expression);
        System.out.println(expression + "\n==RESULT==\n" + result);
        Assertions.assertEquals(MolangValue.of(true), result);
    }

    @Test
    void testOperatorPrecedenceAndOr() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();

        // Test: 1 == 1 && 2 == 2 should be true (1.0)
        MolangExpression expr1 = compiler.compile("1 == 1 && 2 == 2");
        MolangRuntime runtime = MolangRuntime.runtime().create();
        Assertions.assertEquals(1.0f, runtime.resolve(expr1).asFloat());

        // Test: 1 == 2 && 2 == 2 should be false (0.0)
        MolangExpression expr2 = compiler.compile("1 == 2 && 2 == 2");
        Assertions.assertEquals(0.0f, runtime.resolve(expr2).asFloat());

        // Test: 1 == 2 || 2 == 2 should be true (1.0)
        MolangExpression expr3 = compiler.compile("1 == 2 || 2 == 2");
        Assertions.assertEquals(1.0f, runtime.resolve(expr3).asFloat());

        // Test: 1 == 1 || 2 == 3 should be true (1.0)
        MolangExpression expr4 = compiler.compile("1 == 1 || 2 == 3");
        Assertions.assertEquals(1.0f, runtime.resolve(expr4).asFloat());
    }

    @Test
    void testOperatorPrecedenceComparison() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Test: 5 > 3 && 2 < 4 should be true (1.0)
        MolangExpression expr1 = compiler.compile("5 > 3 && 2 < 4");
        Assertions.assertEquals(1.0f, runtime.resolve(expr1).asFloat());

        // Test: 5 >= 5 && 2 <= 2 should be true (1.0)
        MolangExpression expr2 = compiler.compile("5 >= 5 && 2 <= 2");
        Assertions.assertEquals(1.0f, runtime.resolve(expr2).asFloat());

        // Test: 1 != 2 && 3 != 3 should be false (0.0)
        MolangExpression expr3 = compiler.compile("1 != 2 && 3 != 3");
        Assertions.assertEquals(0.0f, runtime.resolve(expr3).asFloat());

        // Test: 1 != 2 || 3 != 3 should be true (1.0)
        MolangExpression expr4 = compiler.compile("1 != 2 || 3 != 3");
        Assertions.assertEquals(1.0f, runtime.resolve(expr4).asFloat());
    }

    @Test
    void testOperatorPrecedenceNegation() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Test: !0 && 1 == 1 should be true (1.0) (! has higher precedence than &&)
        MolangExpression expr1 = compiler.compile("!0 && 1 == 1");
        Assertions.assertEquals(1.0f, runtime.resolve(expr1).asFloat());

        // Test: !1 || 2 == 2 should be true (1.0)
        MolangExpression expr2 = compiler.compile("!1 || 2 == 2");
        Assertions.assertEquals(1.0f, runtime.resolve(expr2).asFloat());

        // Test: !0 && !0 should be true (1.0)
        MolangExpression expr3 = compiler.compile("!0 && !0");
        Assertions.assertEquals(1.0f, runtime.resolve(expr3).asFloat());

        // Test: !(1 == 2) should be true (1.0)
        MolangExpression expr4 = compiler.compile("!(1 == 2)");
        Assertions.assertEquals(1.0f, runtime.resolve(expr4).asFloat());
    }

    @Test
    void testOperatorPrecedenceArithmetic() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Test: 2 + 3 == 5 should be true (1.0) (arithmetic before comparison)
        MolangExpression expr1 = compiler.compile("2 + 3 == 5");
        Assertions.assertEquals(1.0f, runtime.resolve(expr1).asFloat());

        // Test: 2 * 3 == 6 && 4 + 1 == 5 should be true (1.0)
        MolangExpression expr2 = compiler.compile("2 * 3 == 6 && 4 + 1 == 5");
        Assertions.assertEquals(1.0f, runtime.resolve(expr2).asFloat());

        // Test: 10 / 2 > 4 should be true (1.0)
        MolangExpression expr3 = compiler.compile("10 / 2 > 4");
        Assertions.assertEquals(1.0f, runtime.resolve(expr3).asFloat());

        // Test: 3 + 2 * 2 == 7 should be true (1.0) (multiplication before addition)
        MolangExpression expr4 = compiler.compile("3 + 2 * 2 == 7");
        Assertions.assertEquals(1.0f, runtime.resolve(expr4).asFloat());
    }

    @Test
    void testOperatorPrecedenceMixed() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Test: !0 && 2 + 2 == 4 || 5 > 10 should be true (1.0)
        MolangExpression expr1 = compiler.compile("!0 && 2 + 2 == 4 || 5 > 10");
        Assertions.assertEquals(1.0f, runtime.resolve(expr1).asFloat());

        // Test: 1 == 1 && 2 == 2 && 3 == 3 should be true (1.0)
        MolangExpression expr2 = compiler.compile("1 == 1 && 2 == 2 && 3 == 3");
        Assertions.assertEquals(1.0f, runtime.resolve(expr2).asFloat());

        // Test: 1 == 1 || 2 == 2 || 3 == 4 should be true (1.0)
        MolangExpression expr3 = compiler.compile("1 == 1 || 2 == 2 || 3 == 4");
        Assertions.assertEquals(1.0f, runtime.resolve(expr3).asFloat());

        // Test: 1 + 2 == 3 && 4 - 1 == 3 && 2 * 3 == 6 should be true (1.0)
        MolangExpression expr4 = compiler.compile("1 + 2 == 3 && 4 - 1 == 3 && 2 * 3 == 6");
        Assertions.assertEquals(1.0f, runtime.resolve(expr4).asFloat());
    }

    @Test
    void testOperatorPrecedenceChaining() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Test: 1 < 2 && 2 < 3 && 3 < 4 should be true (1.0)
        MolangExpression expr1 = compiler.compile("1 < 2 && 2 < 3 && 3 < 4");
        Assertions.assertEquals(1.0f, runtime.resolve(expr1).asFloat());

        // Test: 1 == 1 && 2 != 3 && 4 > 2 should be true (1.0)
        MolangExpression expr2 = compiler.compile("1 == 1 && 2 != 3 && 4 > 2");
        Assertions.assertEquals(1.0f, runtime.resolve(expr2).asFloat());

        // Test: 5 >= 5 || 3 <= 2 || 1 == 2 should be true (1.0)
        MolangExpression expr3 = compiler.compile("5 >= 5 || 3 <= 2 || 1 == 2");
        Assertions.assertEquals(1.0f, runtime.resolve(expr3).asFloat());
    }

    @Test
    void testOperatorPrecedenceWithVariables() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();

        // Test with variables: v.a == 5 && v.b == 10 should be true (1.0)
        MolangExpression expr1 = compiler.compile("v.a == 5 && v.b == 10");
        MolangRuntime runtime1 = MolangRuntime.runtime()
                .setVariable("a", 5.0f)
                .setVariable("b", 10.0f)
                .create();
        Assertions.assertEquals(1.0f, runtime1.resolve(expr1).asFloat());

        // Test: v.a + v.b == 15 should be true (1.0)
        MolangExpression expr2 = compiler.compile("v.a + v.b == 15");
        Assertions.assertEquals(1.0f, runtime1.resolve(expr2).asFloat());

        // Test: !v.flag && v.count > 0 should be true (1.0)
        MolangExpression expr3 = compiler.compile("!v.flag && v.count > 0");
        MolangRuntime runtime2 = MolangRuntime.runtime()
                .setVariable("flag", 0.0f)
                .setVariable("count", 5.0f)
                .create();
        Assertions.assertEquals(1.0f, runtime2.resolve(expr3).asFloat());
    }

    @Test
    void testOperatorPrecedenceParentheses() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Test: (1 == 1) && (2 == 2) should be true (1.0)
        MolangExpression expr1 = compiler.compile("(1 == 1) && (2 == 2)");
        Assertions.assertEquals(1.0f, runtime.resolve(expr1).asFloat());

        // Test: (2 + 3) * 2 == 10 should be true (1.0)
        MolangExpression expr2 = compiler.compile("(2 + 3) * 2 == 10");
        Assertions.assertEquals(1.0f, runtime.resolve(expr2).asFloat());

        // Test: !(1 == 2) && (3 == 3) should be true (1.0)
        MolangExpression expr3 = compiler.compile("!(1 == 2) && (3 == 3)");
        Assertions.assertEquals(1.0f, runtime.resolve(expr3).asFloat());
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

    private static final List<String> examples = List.of(
            "q.get_equipped_item_name=='milk_bucket'?0:2",
            "!v.is_holding_right?math.cos(q.life_time*180)*1.6",
            "q.get_equipped_item_name(0)==''?math.cos(q.anim_time)*2.6",
            "math.sin(q.rotation_to_camera(1)-q.head_y_rotation(0))<-0.4?1.1",
            "math.round(math.clamp(q.rotation_to_camera(0)*-0.03,-1,!q.is_angry))",
            "math.sin(q.rotation_to_camera(1)-q.head_y_rotation(0))>0.4?-1.1",
            "math.round(math.clamp(q.rotation_to_camera(0)*-0.03,-1,!q.is_angry))",
            "32+query.get_root_locator_offset('armor_offset.helmet',1)",
            "c.item_slot=='main_hand'?0.6:-0.6",
            "c.item_slot=='main_hand'?0.5:-0.5",
            "c.item_slot=='main_hand'?90:-90",
            "c.item_slot=='main_hand'?-1.5:1.5",
            "c.item_slot=='main_hand'?85:-85",
            "c.item_slot=='main_hand'?4:-4",
            "c.item_slot=='main_hand'?42:-42",
            "c.item_slot=='main_hand'?-110:110",
            "c.item_slot=='main_hand'?1:-1",
            "c.item_slot=='main_hand'?10:-10",
            "c.item_slot=='main_hand'?-45:45",
            "c.item_slot=='main_hand'?190:-190",
            "c.item_slot=='main_hand'?0.6:-0.6",
            "-this+query.get_default_bone_pivot('body',1)-query.get_default_bone_pivot('waist',1)",
            "-query.get_root_locator_offset('armor_offset.default_neck','y')",
            "!q.is_item_name_any('slot.weapon.mainhand','minecraft:torch','minecraft:soul_torch','minecraft:lantern','minecraft:soul_lantern')?-90-math.sin(query.anim_time*79.2)*32:0",
            "!q.is_item_name_any('slot.weapon.mainhand','minecraft:torch','minecraft:soul_torch','minecraft:lantern','minecraft:soul_lantern')?50+math.cos(query.anim_time*79.2)*20:0",
            "q.is_item_name_any('slot.weapon.mainhand','minecraft:trident')?0:45+math.cos(query.anim_time*190)*20",
            "q.is_item_name_any('slot.weapon.mainhand','minecraft:trident')?0:-45+math.cos(query.anim_time*190)*-20",
            "math.mod(q.life_time,1.7)>1.55?-2",
            "query.get_root_locator_offset('armor_offset.default_neck', 1)",
            "q.is_item_name_any('slot.weapon.mainhand','minecraft:torch','minecraft:soul_torch','minecraft:lantern','minecraft:soul_lantern')?0:-this*(q.anim_time*-q.anim_time+1)",
            "query.get_root_locator_offset('armor_offset.helmet',1)",
            "!query.is_moving",
            "!query.is_moving",
            "q.any_animation_finished||!q.is_on_ground",
            "!q.is_on_ground||q.modified_move_speed<0.86",
            "!q.is_on_ground",
            "!q.is_on_ground&&!q.is_in_water&&q.vertical_speed!=0.0",
            "!query.is_on_ground&&!query.is_in_water&&query.vertical_speed>0",
            "!q.is_on_ground",
            "!q.is_on_ground",
            "q.is_on_ground&&!q.is_in_water",
            "!q.is_on_fire",
            "!query.is_moving",
            "!query.is_moving",
            "q.any_animation_finished||!q.is_on_ground",
            "!q.is_on_ground||q.modified_move_speed<0.86",
            "!q.is_on_ground",
            "!q.is_on_ground&&!q.is_in_water&&q.vertical_speed!=0.0",
            "!query.is_on_ground&&!query.is_in_water&&query.vertical_speed>0",
            "!q.is_on_ground",
            "!q.is_on_ground",
            "q.is_on_ground&&!q.is_in_water",
            "!q.is_on_fire",
            "!query.is_moving",
            "!query.is_moving",
            "q.any_animation_finished||!q.is_on_ground",
            "!q.is_on_ground||q.modified_move_speed<0.86",
            "!q.is_on_ground",
            "!q.is_on_ground&&!q.is_in_water&&q.vertical_speed!=0.0",
            "!query.is_on_ground&&!query.is_in_water&&query.vertical_speed>0",
            "!q.is_on_ground",
            "!q.is_on_ground",
            "q.is_on_ground&&!q.is_in_water",
            "!q.is_on_fire",
            "!query.is_moving",
            "!query.is_moving",
            "q.any_animation_finished||!q.is_on_ground",
            "!q.is_on_ground||q.modified_move_speed<0.86",
            "!q.is_on_ground",
            "!q.is_on_ground&&!q.is_in_water&&q.vertical_speed!=0.0",
            "!query.is_on_ground&&!query.is_in_water&&query.vertical_speed>0",
            "!q.is_on_ground",
            "!q.is_on_ground",
            "q.is_on_ground&&!q.is_in_water",
            "!q.is_on_fire",
            "!query.is_moving",
            "!query.is_moving",
            "q.any_animation_finished||!q.is_on_ground",
            "!q.is_on_ground||q.modified_move_speed<0.86",
            "!q.is_on_ground",
            "!q.is_on_ground&&!q.is_in_water&&q.vertical_speed!=0.0",
            "!query.is_on_ground&&!query.is_in_water&&query.vertical_speed>0",
            "!q.is_on_ground",
            "!q.is_on_ground",
            "q.is_on_ground&&!q.is_in_water",
            "!q.is_on_fire",
            "q.block_state('minecraft:cardinal_direction')=='west'",
            "q.block_state('update_bit')",
            "!q.block_state('update_bit')"
    );

    @Test
    void testComplexMultiple() throws MolangException {
        for (String testString : examples) {
            tryCompile(testString);
        }
    }

    @Test
    void testWhitespace() throws MolangException {
        for (String testString : examples) {
            tryCompile(testString + " ");
            tryCompile(" " + testString + " ");
        }
    }

    private void tryCompile(String query) throws MolangSyntaxException {
        MolangCompiler compiler = MolangCompiler.create();
        compiler.compile(query);
    }

}
