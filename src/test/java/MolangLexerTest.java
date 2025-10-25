import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import gg.moonflower.molangcompiler.impl.compiler.MolangLexer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class MolangLexerTest {

    @Test
    public void testTokenize() throws MolangSyntaxException {
        String input = "temp.a = 14;temp.b=7";
        MolangLexer.Token[] tokens = MolangLexer.createTokens(input);
        System.out.println(Arrays.toString(tokens));
        Assertions.assertEquals(11, tokens.length);
    }

    @Test
    public void testBadTokens() throws MolangSyntaxException {
        String input = "temp.a=temp.b=temp.c=14;temp.b=7";
        MolangLexer.Token[] tokens = MolangLexer.createTokens(input);
        System.out.println(Arrays.toString(tokens));
        Assertions.assertEquals(19, tokens.length);
    }

    @Test
    public void testFunction() throws MolangSyntaxException {
        String input = "math.sin(42)";
        MolangLexer.Token[] tokens = MolangLexer.createTokens(input);
        System.out.println(Arrays.toString(tokens));
        Assertions.assertEquals(6, tokens.length);
    }

    @Test
    public void testFunctionParameters() throws MolangSyntaxException {
        String input = "math.clamp(7, 0, 4)";
        MolangLexer.Token[] tokens = MolangLexer.createTokens(input);
        System.out.println(Arrays.toString(tokens));
        Assertions.assertEquals(10, tokens.length);
    }

    @Test
    public void testDoubleQuotedString() throws MolangSyntaxException {
        String input = "\"hello world\"";
        MolangLexer.Token[] tokens = MolangLexer.createTokens(input);
        System.out.println(Arrays.toString(tokens));
        Assertions.assertEquals(1, tokens.length);
        Assertions.assertEquals(MolangLexer.TokenType.STRING, tokens[0].type());
        Assertions.assertEquals("\"hello world\"", tokens[0].value());
    }

    @Test
    public void testSingleQuotedString() throws MolangSyntaxException {
        String input = "'hello world'";
        MolangLexer.Token[] tokens = MolangLexer.createTokens(input);
        System.out.println(Arrays.toString(tokens));
        Assertions.assertEquals(1, tokens.length);
        Assertions.assertEquals(MolangLexer.TokenType.STRING, tokens[0].type());
        Assertions.assertEquals("'hello world'", tokens[0].value());
    }

    @Test
    public void testStringWithEscapes() throws MolangSyntaxException {
        String input = "\"hello \\\"world\\\"\"";
        MolangLexer.Token[] tokens = MolangLexer.createTokens(input);
        System.out.println(Arrays.toString(tokens));
        Assertions.assertEquals(1, tokens.length);
        Assertions.assertEquals(MolangLexer.TokenType.STRING, tokens[0].type());
        Assertions.assertEquals("\"hello \\\"world\\\"\"", tokens[0].value());
    }

    @Test
    public void testStringInExpression() throws MolangSyntaxException {
        String input = "temp.a = \"test\"";
        MolangLexer.Token[] tokens = MolangLexer.createTokens(input);
        System.out.println(Arrays.toString(tokens));
        Assertions.assertEquals(5, tokens.length);
        Assertions.assertEquals(MolangLexer.TokenType.STRING, tokens[4].type());
    }
}
