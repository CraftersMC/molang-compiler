import gg.moonflower.molangcompiler.api.MolangCompiler;
import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangRuntime;
import gg.moonflower.molangcompiler.api.exception.MolangException;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author Buddy
 */
public class StressTest {
    private static final int TEST_COUNT = 10_000_000;

    private static long elapsedParsingTime = 0;
    private static long elapsedExecutingBytecodeTime = 0;

    private static final MolangCompiler COMPILER = MolangCompiler.create();
    private static final MolangCompiler DEBUG_COMPILER = MolangCompiler.create(MolangCompiler.DEFAULT_FLAGS | MolangCompiler.WRITE_CLASSES_FLAG);
    private static final float[] PREVENT_OPTIMIZING = new float[TEST_COUNT];

    private static String longestTaken;
    private static long longestTakenTime = 0;

    @Test
    void test() throws MolangException {
        test("query.a * query.b * query.c * query.d * query.e * query.f * query.g * query.h", 71192.2148438F,
                Map.of("a", 1.5F,
                        "b", 5.5F,
                        "c", 8.5F,
                        "d", 3.5F,
                        "e", 1.5F,
                        "f", 6.5F,
                        "g", 8.5F,
                        "h", 3.5F
                ));
        test(""
                + "{"
                + "	v.x = 5; "
                + "		{"
                + "		v.y = 2 * v.x;"
                + "			{"
                + "				v.z = 3 * v.y;"
                + "				return v.z;"
                + "			}"
                + "		}"
                + "}", 30.0f, Map.of());
        test("v.x = 20; loop(30, { v.x = v.x + 1 }); return v.x", 50.0f, Map.of());

        test("query.is_dashing * (query.anim_time + query.delta_time)", 8.25F, Map.of("is_dashing", 1.5F, "anim_time", 3.5F, "delta_time", 2.0f));
        test("query.climb_horizontal > 0.0 ? 1.0 - math.mod(0.25 * query.climb_horizontal, 1.0) : 0.25 * query.climb_horizontal", 0.625F, Map.of("climb_horizontal", 1.5F));

        test("5 + 3", 8.0f, Map.of());
        test("8 - 6", 2.0f, Map.of());
        test("6 / 2", 3.0f, Map.of());
        test("9 / 3", 3.0f, Map.of());
        test("9 / 0", Float.POSITIVE_INFINITY, Map.of());
        test("9 * 0", 0.0f, Map.of());
        test("4 * 3", 12.0f, Map.of());
        test("3 * 4", 12.0f, Map.of());
        test("(3*5) * 4", 60.0f, Map.of());
        test("(4*8) / 10", 3.2F, Map.of());

        test("0 && 0", 0.0f, Map.of());
        test("0 && 0 && 0", 0.0f, Map.of());
        test("0 && 0 && 1", 0.0f, Map.of());
        test("0 && 1", 0.0f, Map.of());
        test("0 && 1 && 0", 0.0f, Map.of());
        test("0 && 1 && 1", 0.0f, Map.of());
        test("1 && 0", 0.0f, Map.of());
        test("1 && 0 && 0", 0.0f, Map.of());
        test("1 && 0 && 1", 0.0f, Map.of());
        test("1 && 1", 1.0f, Map.of());
        test("1 && 1 && 0", 0.0f, Map.of());
        test("1 && 1 && 1", 1.0f, Map.of());
        test("0 || 0", 0.0f, Map.of());
        test("0 || 1", 1.0f, Map.of());
        test("1 || 0", 1.0f, Map.of());
        test("1 || 1", 1.0f, Map.of());
        test("0 || 0 || 0", 0.0f, Map.of());
        test("0 || 0 || 1", 1.0f, Map.of());
        test("0 || 1 || 0", 1.0f, Map.of());
        test("0 || 1 || 1", 1.0f, Map.of());
        test("1 || 0 || 0", 1.0f, Map.of());
        test("1 || 0 || 1", 1.0f, Map.of());
        test("1 || 1 || 0", 1.0f, Map.of());
        test("1 || 1 || 1", 1.0f, Map.of());

        test("10 > 10 ? 1 : 0", 0.0f, Map.of());
        test("10 < 10 ? 1 : 0", 0.0f, Map.of());
        test("10 >= 10 ? 1 : 0", 1.0f, Map.of());
        test("10 <= 10 ? 1 : 0", 1.0f, Map.of());
        test("5 > 5 ? 1 : 0", 0.0f, Map.of());
        test("5 < 5 ? 1 : 0", 0.0f, Map.of());
        test("5 >= 5 ? 1 : 0", 1.0f, Map.of());
        test("5 <= 5 ? 1 : 0", 1.0f, Map.of());
        test("5 > 2 ? 1 : 0", 1.0f, Map.of());
        test("2 > 5 ? 1 : 0", 0.0f, Map.of());
        test("5 < 2 ? 1 : 0", 0.0f, Map.of());
        test("2 < 5 ? 1 : 0", 1.0f, Map.of());

        test("(1 ? 16 : 3)", 16.0f, Map.of());
        test("(5 ? 10 : 6)", 10.0f, Map.of());
        test("(0 ? 10 : 9)", 9.0f, Map.of());

        test("query.a*5", 10, Map.of("a", 2.0f));

        for (float a : new float[]{5.0f}) {
            test("(((948 + (515)) - ((761 * (77)))) - (((query.a * (844)) / ((query.a / (query.a))))))", (((948.0f + (515.0f)) - ((761.0f * (77.0f)))) - (((a * (844.0f)) / ((a / (a)))))), Map.of("a", a));
            test("(((588 - (query.a)) + ((query.a - (978)))) * (((query.a + (263)) / ((592 / (189))))))", (((588.0f - (a)) + ((a - (978.0f)))) * (((a + (263.0f)) / ((592.0f / (189.0f)))))), Map.of("a", a));
            test("(((372 / (query.a)) - ((606 + (68)))) / (((query.a + (514)) / ((399 / (678))))))", (((372.0f / (a)) - ((606.0f + (68.0f)))) / (((a + (514.0f)) / ((399.0f / (678.0f)))))), Map.of("a", a));
            test("(((660 + (128)) * ((564 + (585)))) - (((614 + (query.a)) / ((query.a / (911))))))", (((660.0f + (128.0f)) * ((564.0f + (585.0f)))) - (((614.0f + (a)) / ((a / (911.0f)))))), Map.of("a", a));
            test("(((452 / (767)) + ((846 + (query.a)))) + (((552 / (52)) + ((662 - (369))))))", (((452.0f / (767.0f)) + ((846.0f + (a)))) + (((552.0f / (52.0f)) + ((662.0f - (369.0f)))))), Map.of("a", a));
            test("(((92 + (506)) / ((query.a / (query.a)))) - (((303 - (query.a)) / ((978 - (17))))))", (((92.0f + (506.0f)) / ((a / (a)))) - (((303.0f - (a)) / ((978.0f - (17.0f)))))), Map.of("a", a));
            test("(((876 * (query.a)) / ((377 + (query.a)))) * (((268 * (29)) + ((query.a / (58))))))", (((876.0f * (a)) / ((377.0f + (a)))) * (((268.0f * (29.0f)) + ((a / (58.0f)))))), Map.of("a", a));
            test("(((164 * (query.a)) - ((query.a / (850)))) * (((query.a / (query.a)) - ((662 + (307))))))", (((164.0f * (a)) - ((a / (850.0f)))) * (((a / (a)) - ((662.0f + (307.0f)))))), Map.of("a", a));
            test("(((956 * (query.a)) + ((query.a / (query.a)))) * (((query.a * (query.a)) + ((828 * (query.a))))))", (((956.0f * (a)) + ((a / (a)))) * (((a * (a)) + ((828.0f * (a)))))), Map.of("a", a));
            test("(((596 / (759)) / ((776 / (query.a)))) - (((query.a * (295)) + ((query.a + (844))))))", (((596.0f / (759.0f)) / ((776.0f / (a)))) - (((a * (295.0f)) + ((a + (844.0f)))))), Map.of("a", a));
            test("(((380 - (query.a)) + ((797 - (214)))) - (((591 / (480)) - ((query.a / (query.a))))))", (((380.0f - (a)) + ((797.0f - (214.0f)))) - (((591.0f / (480.0f)) - ((a / (a)))))), Map.of("a", a));
            test("(((668 - (query.a)) * ((query.a + (593)))) - (((query.a * (query.a)) - ((518 - (query.a))))))", (((668.0f - (a)) * ((a + (593.0f)))) - (((a * (a)) - ((518.0f - (a)))))), Map.of("a", a));
            test("(((812 - (query.a)) - ((query.a / (596)))) / (((query.a + (query.a)) + ((query.a / (31))))))", (((812.0f - (a)) - ((a / (596.0f)))) / (((a + (a)) + ((a / (31.0f)))))), Map.of("a", a));
            test("(((100 - (query.a)) + ((693 + (query.a)))) / (((query.a / (155)) - ((query.a / (860))))))", (((100.0f - (a)) + ((693.0f + (a)))) / (((a / (155.0f)) - ((a / (860.0f)))))), Map.of("a", a));
            test("(((884 / (498)) * ((query.a * (query.a)))) + (((query.a - (query.a)) + ((336 * (794))))))", (((884.0f / (498.0f)) * ((a * (a)))) + (((a - (a)) + ((336.0f * (794.0f)))))), Map.of("a", a));
            test("(((172 + (query.a)) / ((query.a + (query.a)))) * (((query.a * (query.a)) * ((228 - (292))))))", (((172.0f + (a)) / ((a + (a)))) * (((a * (a)) * ((228.0f - (292.0f)))))), Map.of("a", a));
        }

        System.out.println();
        System.out.println("\n──── Summary ────────────────────────────────");
        System.out.printf("Total parsing time     : %s%n", formatTime(elapsedParsingTime));
        System.out.printf("Avg parsing time     : %s%n", formatTime(elapsedParsingTime / (double) TEST_COUNT));
        System.out.printf("Total execution time   : %s%n", formatTime(elapsedExecutingBytecodeTime));
        System.out.printf("Longest single exec    : %s  (\"%s\")%n", formatTime(longestTakenTime), longestTaken);
        System.out.printf("Average exec per test  : %s%n", formatTime(elapsedExecutingBytecodeTime / (double) TEST_COUNT));
        System.out.println("─────────────────────────────────────────────");

//        if (longestTaken != null) {
//            DEBUG_COMPILER.compile(longestTaken);
//        }
    }

    private static String formatTime(double ns) {
        if (ns < 1_000) // <1 µs
            return String.format("%.2fns", ns);
        if (ns < 1_000_000) // <1 ms
            return String.format("%.2fµs", ns / 1_000.0);
        if (ns < 1_000_000_000) // <1 s
            return String.format("%.2fms", ns / 1_000_000.0);
        return String.format("%.2fs", ns / 1_000_000_000.0);
    }

    public static void test(String expr, float exp, Map<String, Float> vars) throws MolangException {
        System.out.println("test(\"" + expr + "\", " + exp + "F);");

        MolangEnvironment environment = MolangRuntime.runtime()
                .setVariables(context -> vars.forEach((s, aFloat) -> context.addQuery(s, MolangExpression.of(aFloat))))
                .create();

        long time = System.nanoTime();
        MolangExpression expression = COMPILER.compile(expr);
        elapsedParsingTime += (System.nanoTime() - time);

        time = System.nanoTime();
        for (int i = 0; i < TEST_COUNT; i++) {
            PREVENT_OPTIMIZING[i] = expression.get(environment).asFloat();
        }
        if (PREVENT_OPTIMIZING[0] != exp) {
            throw new RuntimeException("Invalid bytecode value. Expected " + exp + ", but got " + PREVENT_OPTIMIZING[0] + " in expr \"" + expr + "\" transformed to \"" + expression + "\"");
        }
        long t = (System.nanoTime() - time);
        if (t > longestTakenTime) {
            longestTakenTime = t;
            longestTaken = expr;
        }
        elapsedExecutingBytecodeTime += t;
    }
}