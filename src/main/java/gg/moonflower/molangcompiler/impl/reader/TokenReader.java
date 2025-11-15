package gg.moonflower.molangcompiler.impl.reader;

import gg.moonflower.molangcompiler.impl.compiler.MolangLexer;
import org.jetbrains.annotations.ApiStatus;

/**
 * A specialized reader that operates on an array of tokens instead of raw characters.
 * <p>
 * This class extends {@link StringReader} to provide token-based reading functionality for
 * the MoLang parser. Instead of reading individual characters, it reads pre-lexed tokens,
 * which simplifies the parsing stage by working with higher-level language constructs.
 * <p>
 * The cursor position tracks the current token index, and methods provide access to tokens
 * at the current position or relative to it.
 *
 * @author Ocelot
 * @since 1.0.0
 */
@ApiStatus.Internal
public class TokenReader extends StringReader {

    private final MolangLexer.Token[] tokens;

    /**
     * Creates a new TokenReader with the specified array of tokens.
     *
     * @param tokens The array of tokens to read from
     */
    public TokenReader(MolangLexer.Token[] tokens) {
        super("");
        this.tokens = tokens;
    }

    /**
     * Reconstructs the original string from all tokens.
     * <p>
     * This method concatenates the string values of all tokens to produce the complete
     * original input string.
     *
     * @return The reconstructed string from all tokens
     */
    @Override
    public String getString() {
        StringBuilder builder = new StringBuilder();
        for (MolangLexer.Token token : this.tokens) {
            builder.append(token.value());
        }
        return builder.toString();
    }

    /**
     * Checks if the specified number of tokens can be read from the current position.
     *
     * @param length The number of tokens to check
     * @return true if at least {@code length} tokens are available from the current position
     */
    @Override
    public boolean canRead(int length) {
        return this.cursor + length <= this.tokens.length;
    }

    /**
     * Calculates the character offset in the original string corresponding to the current cursor position.
     * <p>
     * This method sums the lengths of all tokens up to and including the current cursor position
     * to determine where in the original character string the cursor is located.
     *
     * @return The character offset in the original string
     */
    public int getCursorOffset() {
        int offset = 0;
        for (int i = 0; i <= Math.min(this.cursor, this.tokens.length - 1); i++) {
            offset += this.tokens[i].value().length();
        }
        return offset;
    }

    /**
     * Peeks at a token at a relative offset from the current cursor position.
     * <p>
     * This method returns the token located {@code amount} positions after the current cursor
     * without advancing the cursor.
     *
     * @param amount The number of positions to look ahead (0 for current, 1 for next, etc.)
     * @return The token at the specified relative position
     * @throws ArrayIndexOutOfBoundsException if the position is out of bounds
     */
    public MolangLexer.Token peekAfter(int amount) {
        return this.tokens[this.cursor + amount];
    }

    /**
     * Peeks at the token at the current cursor position without advancing the cursor.
     *
     * @return The token at the current cursor position
     * @throws ArrayIndexOutOfBoundsException if the cursor is beyond the end of the token array
     */
    public MolangLexer.Token peek() {
        return this.tokens[this.cursor];
    }
}
