import gg.moonflower.molangcompiler.api.MolangCompiler;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangRuntime;
import gg.moonflower.molangcompiler.api.exception.MolangException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for all mathematical operations and functions.
 * <p>
 * Tests are based on official Mojang MoLang specification with the following extensions:
 * - math.sign(condition) - Returns -1, 0, or 1 based on sign (compiler extension)
 * - math.triangle_wave(x, wavelength) - Triangle wave oscillation (compiler extension)
 * <p>
 * Note: Official MoLang does NOT include math.e or math.tan
 */
public class MolangMathTest {

    private static final float DELTA = 0.0001f;

    @Test
    void testMathConstants() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        MolangExpression piExpr = compiler.compile("math.pi");
        Assertions.assertEquals(Math.PI, runtime.resolve(piExpr).asFloat(), DELTA);

        MolangExpression eExpr = compiler.compile("math.e");
        Assertions.assertEquals(Math.E, runtime.resolve(eExpr).asFloat(), DELTA);
    }

    @Test
    void testMathAbs() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(5.0f, runtime.resolve(compiler.compile("math.abs(-5)")).asFloat(), DELTA);
        Assertions.assertEquals(5.0f, runtime.resolve(compiler.compile("math.abs(5)")).asFloat(), DELTA);
        Assertions.assertEquals(0.0f, runtime.resolve(compiler.compile("math.abs(0)")).asFloat(), DELTA);
        Assertions.assertEquals(3.14f, runtime.resolve(compiler.compile("math.abs(-3.14)")).asFloat(), DELTA);
    }

    @Test
    void testMathSign() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(1.0f, runtime.resolve(compiler.compile("math.sign(5)")).asFloat(), DELTA);
        Assertions.assertEquals(-1.0f, runtime.resolve(compiler.compile("math.sign(-5)")).asFloat(), DELTA);
        Assertions.assertEquals(0.0f, runtime.resolve(compiler.compile("math.sign(0)")).asFloat(), DELTA);
        Assertions.assertEquals(1.0f, runtime.resolve(compiler.compile("math.sign(0.1)")).asFloat(), DELTA);
    }

    @Test
    void testMathSin() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(Math.sin(Math.toRadians(0)), runtime.resolve(compiler.compile("math.sin(0)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.sin(Math.toRadians(90)), runtime.resolve(compiler.compile("math.sin(90)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.sin(Math.toRadians(180)), runtime.resolve(compiler.compile("math.sin(180)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.sin(Math.toRadians(45)), runtime.resolve(compiler.compile("math.sin(45)")).asFloat(), DELTA);
    }

    @Test
    void testMathCos() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(Math.cos(Math.toRadians(0)), runtime.resolve(compiler.compile("math.cos(0)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.cos(Math.toRadians(90)), runtime.resolve(compiler.compile("math.cos(90)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.cos(Math.toRadians(180)), runtime.resolve(compiler.compile("math.cos(180)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.cos(Math.toRadians(45)), runtime.resolve(compiler.compile("math.cos(45)")).asFloat(), DELTA);
    }

    @Test
    void testMathAsin() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(Math.toDegrees(Math.asin(0)), runtime.resolve(compiler.compile("math.asin(0)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.toDegrees(Math.asin(0.5)), runtime.resolve(compiler.compile("math.asin(0.5)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.toDegrees(Math.asin(1)), runtime.resolve(compiler.compile("math.asin(1)")).asFloat(), DELTA);
    }

    @Test
    void testMathAcos() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(Math.toDegrees(Math.acos(1)), runtime.resolve(compiler.compile("math.acos(1)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.toDegrees(Math.acos(0.5)), runtime.resolve(compiler.compile("math.acos(0.5)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.toDegrees(Math.acos(0)), runtime.resolve(compiler.compile("math.acos(0)")).asFloat(), DELTA);
    }

    @Test
    void testMathAtan() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(Math.toDegrees(Math.atan(0)), runtime.resolve(compiler.compile("math.atan(0)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.toDegrees(Math.atan(1)), runtime.resolve(compiler.compile("math.atan(1)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.toDegrees(Math.atan(-1)), runtime.resolve(compiler.compile("math.atan(-1)")).asFloat(), DELTA);
    }

    @Test
    void testMathAtan2() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(Math.toDegrees(Math.atan2(1, 1)), runtime.resolve(compiler.compile("math.atan2(1, 1)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.toDegrees(Math.atan2(1, 0)), runtime.resolve(compiler.compile("math.atan2(1, 0)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.toDegrees(Math.atan2(0, 1)), runtime.resolve(compiler.compile("math.atan2(0, 1)")).asFloat(), DELTA);
    }

    @Test
    void testMathSqrt() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(Math.sqrt(0), runtime.resolve(compiler.compile("math.sqrt(0)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.sqrt(4), runtime.resolve(compiler.compile("math.sqrt(4)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.sqrt(16), runtime.resolve(compiler.compile("math.sqrt(16)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.sqrt(2), runtime.resolve(compiler.compile("math.sqrt(2)")).asFloat(), DELTA);
    }

    @Test
    void testMathPow() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(Math.pow(2, 3), runtime.resolve(compiler.compile("math.pow(2, 3)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.pow(5, 2), runtime.resolve(compiler.compile("math.pow(5, 2)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.pow(10, 0), runtime.resolve(compiler.compile("math.pow(10, 0)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.pow(2, -1), runtime.resolve(compiler.compile("math.pow(2, -1)")).asFloat(), DELTA);
    }

    @Test
    void testMathExp() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(Math.exp(0), runtime.resolve(compiler.compile("math.exp(0)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.exp(1), runtime.resolve(compiler.compile("math.exp(1)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.exp(2), runtime.resolve(compiler.compile("math.exp(2)")).asFloat(), DELTA);
    }

    @Test
    void testMathLn() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(Math.log(1), runtime.resolve(compiler.compile("math.ln(1)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.log(2.718282f), runtime.resolve(compiler.compile("math.ln(2.718282)")).asFloat(), DELTA);
        Assertions.assertEquals(Math.log(10), runtime.resolve(compiler.compile("math.ln(10)")).asFloat(), DELTA);
    }

    @Test
    void testMathFloor() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(3.0f, runtime.resolve(compiler.compile("math.floor(3.7)")).asFloat(), DELTA);
        Assertions.assertEquals(3.0f, runtime.resolve(compiler.compile("math.floor(3.2)")).asFloat(), DELTA);
        Assertions.assertEquals(-4.0f, runtime.resolve(compiler.compile("math.floor(-3.2)")).asFloat(), DELTA);
        Assertions.assertEquals(5.0f, runtime.resolve(compiler.compile("math.floor(5)")).asFloat(), DELTA);
    }

    @Test
    void testMathCeil() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(4.0f, runtime.resolve(compiler.compile("math.ceil(3.7)")).asFloat(), DELTA);
        Assertions.assertEquals(4.0f, runtime.resolve(compiler.compile("math.ceil(3.2)")).asFloat(), DELTA);
        Assertions.assertEquals(-3.0f, runtime.resolve(compiler.compile("math.ceil(-3.2)")).asFloat(), DELTA);
        Assertions.assertEquals(5.0f, runtime.resolve(compiler.compile("math.ceil(5)")).asFloat(), DELTA);
    }

    @Test
    void testMathTrunc() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(3.0f, runtime.resolve(compiler.compile("math.trunc(3.7)")).asFloat(), DELTA);
        Assertions.assertEquals(3.0f, runtime.resolve(compiler.compile("math.trunc(3.2)")).asFloat(), DELTA);
        Assertions.assertEquals(-3.0f, runtime.resolve(compiler.compile("math.trunc(-3.2)")).asFloat(), DELTA);
        Assertions.assertEquals(-3.0f, runtime.resolve(compiler.compile("math.trunc(-3.8)")).asFloat(), DELTA);
    }

    @Test
    void testMathRound() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(4.0f, runtime.resolve(compiler.compile("math.round(3.7)")).asFloat(), DELTA);
        Assertions.assertEquals(3.0f, runtime.resolve(compiler.compile("math.round(3.2)")).asFloat(), DELTA);
        Assertions.assertEquals(4.0f, runtime.resolve(compiler.compile("math.round(3.5)")).asFloat(), DELTA);
        Assertions.assertEquals(-3.0f, runtime.resolve(compiler.compile("math.round(-3.2)")).asFloat(), DELTA);
    }

    @Test
    void testMathMin() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(3.0f, runtime.resolve(compiler.compile("math.min(3, 5)")).asFloat(), DELTA);
        Assertions.assertEquals(-5.0f, runtime.resolve(compiler.compile("math.min(3, -5)")).asFloat(), DELTA);
        Assertions.assertEquals(0.0f, runtime.resolve(compiler.compile("math.min(0, 10)")).asFloat(), DELTA);
    }

    @Test
    void testMathMax() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(5.0f, runtime.resolve(compiler.compile("math.max(3, 5)")).asFloat(), DELTA);
        Assertions.assertEquals(3.0f, runtime.resolve(compiler.compile("math.max(3, -5)")).asFloat(), DELTA);
        Assertions.assertEquals(10.0f, runtime.resolve(compiler.compile("math.max(0, 10)")).asFloat(), DELTA);
    }

    @Test
    void testMathClamp() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(5.0f, runtime.resolve(compiler.compile("math.clamp(5, 0, 10)")).asFloat(), DELTA);
        Assertions.assertEquals(0.0f, runtime.resolve(compiler.compile("math.clamp(-5, 0, 10)")).asFloat(), DELTA);
        Assertions.assertEquals(10.0f, runtime.resolve(compiler.compile("math.clamp(15, 0, 10)")).asFloat(), DELTA);
        Assertions.assertEquals(7.5f, runtime.resolve(compiler.compile("math.clamp(7.5, 0, 10)")).asFloat(), DELTA);
    }

    @Test
    void testMathLerp() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(0.0f, runtime.resolve(compiler.compile("math.lerp(0, 10, 0)")).asFloat(), DELTA);
        Assertions.assertEquals(10.0f, runtime.resolve(compiler.compile("math.lerp(0, 10, 1)")).asFloat(), DELTA);
        Assertions.assertEquals(5.0f, runtime.resolve(compiler.compile("math.lerp(0, 10, 0.5)")).asFloat(), DELTA);
        Assertions.assertEquals(2.5f, runtime.resolve(compiler.compile("math.lerp(0, 10, 0.25)")).asFloat(), DELTA);
    }

    @Test
    void testMathLerpRotate() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Test rotation interpolation that wraps correctly
        MolangExpression expr1 = compiler.compile("math.lerprotate(170, 190, 0.5)");
        Assertions.assertEquals(180.0f, runtime.resolve(expr1).asFloat(), DELTA);

        // Test wrapping around 360 degrees
        // lerprotate(350, 10, 0.5) = 350 + wrapDegrees(10-350) * 0.5 = 350 + 20 * 0.5 = 360
        MolangExpression expr2 = compiler.compile("math.lerprotate(350, 10, 0.5)");
        float result = runtime.resolve(expr2).asFloat();
        // Result can be 360 or 0 (equivalent angles)
        Assertions.assertTrue(Math.abs(result - 360.0f) < DELTA || Math.abs(result) < DELTA);
    }

    @Test
    void testMathMod() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(1.0f, runtime.resolve(compiler.compile("math.mod(10, 3)")).asFloat(), DELTA);
        Assertions.assertEquals(0.0f, runtime.resolve(compiler.compile("math.mod(10, 5)")).asFloat(), DELTA);
        Assertions.assertEquals(2.0f, runtime.resolve(compiler.compile("math.mod(17, 5)")).asFloat(), DELTA);
    }

    @Test
    void testMathHermiteBlend() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        Assertions.assertEquals(0.0f, runtime.resolve(compiler.compile("math.hermite_blend(0)")).asFloat(), DELTA);
        Assertions.assertEquals(1.0f, runtime.resolve(compiler.compile("math.hermite_blend(1)")).asFloat(), DELTA);
        Assertions.assertEquals(0.5f, runtime.resolve(compiler.compile("math.hermite_blend(0.5)")).asFloat(), DELTA);
    }

    @Test
    void testMathRandom() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Test that random returns a condition in the correct range
        for (int i = 0; i < 10; i++) {
            float result = runtime.resolve(compiler.compile("math.random(0, 10)")).asFloat();
            Assertions.assertTrue(result >= 0.0f && result < 10.0f, "Random condition out of range: " + result);
        }

        // Test with negative range
        for (int i = 0; i < 10; i++) {
            float result = runtime.resolve(compiler.compile("math.random(-5, 5)")).asFloat();
            Assertions.assertTrue(result >= -5.0f && result < 5.0f, "Random condition out of range: " + result);
        }
    }

    @Test
    void testMathDieRoll() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Test that die roll returns reasonable values
        for (int i = 0; i < 10; i++) {
            float result = runtime.resolve(compiler.compile("math.die_roll(3, 1, 6)")).asFloat();
            // 3 dice with range 1-6 should give 3-18
            Assertions.assertTrue(result >= 3.0f && result <= 18.0f, "Die roll out of range: " + result);
        }
    }

    @Test
    void testMathDieRollInteger() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Test that integer die roll returns integer values
        for (int i = 0; i < 10; i++) {
            float result = runtime.resolve(compiler.compile("math.die_roll_integer(2, 1, 6)")).asFloat();
            // Should be an integer
            Assertions.assertEquals(result, Math.floor(result), DELTA);
            // 2 dice with range 1-6 should give 2-12
            Assertions.assertTrue(result >= 2.0f && result <= 12.0f, "Integer die roll out of range: " + result);
        }
    }

    @Test
    void testMathRandomInteger() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Test that random integer returns integer values in range
        for (int i = 0; i < 10; i++) {
            float result = runtime.resolve(compiler.compile("math.random_integer(1, 10)")).asFloat();
            // Should be an integer
            Assertions.assertEquals(result, Math.floor(result), DELTA);
            // Should be in range [1, 10]
            Assertions.assertTrue(result >= 1.0f && result <= 10.0f, "Random integer out of range: " + result);
        }
    }

    @Test
    void testMathTriangleWave() throws MolangException {
        // NOTE: math.triangle_wave is a COMPILER EXTENSION, not in official MoLang spec
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Triangle wave oscillates between -1 and 1
        // Formula: (abs(x % wavelength - wavelength * 0.5) - wavelength * 0.25) / (wavelength * 0.25)
        Assertions.assertEquals(1.0f, runtime.resolve(compiler.compile("math.triangle_wave(0, 4)")).asFloat(), DELTA);
        Assertions.assertEquals(0.0f, runtime.resolve(compiler.compile("math.triangle_wave(1, 4)")).asFloat(), DELTA);
        Assertions.assertEquals(-1.0f, runtime.resolve(compiler.compile("math.triangle_wave(2, 4)")).asFloat(), DELTA);
    }

    @Test
    void testComplexMathExpression() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime()
                .setQuery("angle", 45.0f)
                .create();

        // sqrt(sin²(45) + cos²(45)) should equal 1
        MolangExpression expr = compiler.compile(
                "math.sqrt(math.pow(math.sin(q.angle), 2) + math.pow(math.cos(q.angle), 2))"
        );
        Assertions.assertEquals(1.0f, runtime.resolve(expr).asFloat(), DELTA);
    }

    @Test
    void testChainedMathOperations() throws MolangException {
        MolangCompiler compiler = MolangCompiler.create();
        MolangRuntime runtime = MolangRuntime.runtime().create();

        // Test chaining multiple math operations
        MolangExpression expr = compiler.compile(
                "math.floor(math.sqrt(math.pow(3, 2) + math.pow(4, 2)))"
        );
        // sqrt(9 + 16) = sqrt(25) = 5
        Assertions.assertEquals(5.0f, runtime.resolve(expr).asFloat(), DELTA);
    }
}
