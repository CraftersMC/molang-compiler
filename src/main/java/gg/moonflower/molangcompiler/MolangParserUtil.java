package gg.moonflower.molangcompiler;

import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import gg.moonflower.molangcompiler.impl.compiler.MolangLexer;
import gg.moonflower.molangcompiler.impl.reader.TokenReader;

/**
 * Utility class providing common parsing operations for MoLang parsers.
 * <p>
 * This class contains helper methods for token validation, error reporting,
 * and other parsing utilities used across different parser implementations.
 * </p>
 *
 * @author Ocelot
 * @since 4.0.0
 */
public class MolangParserUtil {

    /**
     * Validates that the next token matches the expected type.
     *
     * @param reader The token reader
     * @param token  The expected token type
     * @throws MolangSyntaxException if the token doesn't match or no tokens are available
     */
    public static void expect(TokenReader reader, MolangLexer.TokenType token) throws MolangSyntaxException {
        if (!reader.canRead() || reader.peek().type() != token) {
            throw error("Expected " + token, reader);
        }
    }

    /**
     * Validates that the next token matches the expected type and condition.
     *
     * @param reader The token reader
     * @param token  The expected token type
     * @param value  The expected token condition
     * @throws MolangSyntaxException if the token doesn't match
     */
    public static void expect(TokenReader reader, MolangLexer.TokenType token, String value) throws MolangSyntaxException {
        expect(reader, token);
        if (!value.equals(reader.peek().value())) {
            throw error("Expected " + value, reader);
        }
    }

    /**
     * Validates that at least the specified number of tokens are available.
     *
     * @param reader The token reader
     * @param amount The minimum number of tokens required
     * @throws MolangSyntaxException if insufficient tokens are available
     */
    public static void expectLength(TokenReader reader, int amount) throws MolangSyntaxException {
        if (!reader.canRead(amount)) {
            throw new MolangSyntaxException("Trailing statement", reader.getString(), reader.getString().length());
        }
    }

    /**
     * Creates a syntax exception with the current parsing context.
     *
     * @param error  The error message
     * @param reader The token reader providing the position and source string
     * @return A MolangSyntaxException with context information
     */
    public static MolangSyntaxException error(String error, TokenReader reader) {
        return new MolangSyntaxException(error, reader.getString(), reader.getCursorOffset());
    }
}
