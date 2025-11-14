package gg.moonflower.molangcompiler.impl.compiler;

import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lexical analyzer (tokenizer) that converts MoLang source code into a sequence of tokens.
 * <p>
 * The lexer is the first stage of the compilation pipeline. It takes a raw string input and
 * breaks it down into a flat array of tokens, each representing a meaningful unit of the language
 * (keywords, operators, identifiers, literals, etc.).
 * <p>
 * The lexer uses regular expressions to match token patterns and supports:
 * <ul>
 *   <li>Keywords: {@code return}, {@code loop}, {@code if}, {@code else}, {@code break}, {@code continue}</li>
 *   <li>Literals: numbers, strings (single or double quoted), booleans</li>
 *   <li>Identifiers: alphanumeric names for variables and functions</li>
 *   <li>Operators: arithmetic (+, -, *, /), comparison (&lt;, &gt;, ==), logical (&amp;&amp;, ||, !)</li>
 *   <li>Special symbols: parentheses, braces, dot notation, semicolons</li>
 * </ul>
 * <p>
 * The lexer automatically inserts semicolons after closing braces to simplify parsing.
 *
 * @author Ocelot
 * @since 1.0.0
 */
@ApiStatus.Internal
public final class MolangLexer {

    /**
     * Tokenizes the input string into an array of tokens.
     * <p>
     * This method performs lexical analysis by:
     * <ol>
     *   <li>Removing newlines and tabs</li>
     *   <li>Skipping spaces</li>
     *   <li>Matching token patterns using regex</li>
     *   <li>Automatically inserting semicolons after closing braces</li>
     * </ol>
     *
     * @param input The MoLang source code to tokenize
     * @return An array of tokens representing the input
     * @throws MolangSyntaxException if an unknown token is encountered
     */
    public static Token[] createTokens(String input) throws MolangSyntaxException {
        StringReader reader = new StringReader(input);
        List<Token> tokens = new ArrayList<>();

        while (reader.canRead()) {
            reader.skipWhitespace();
            Token token = getToken(reader);
            if (token == null) {
                continue;
            }
            if (!tokens.isEmpty()) {
                Token lastToken = tokens.get(tokens.size() - 1);
                // Insert semicolon after scopes
                if (lastToken.type == TokenType.RIGHT_BRACE && token.type != TokenType.SEMICOLON) {
                    tokens.add(new Token(TokenType.SEMICOLON, ";"));
                }
            }
            tokens.add(token);
        }

        return tokens.toArray(Token[]::new);
    }

    /**
     * Attempts to match a token at the current position in the reader.
     * <p>
     * This method iterates through all token types and tries to match their regex patterns
     * against the remaining input. The first matching pattern wins.
     *
     * @param reader The string reader positioned at the token to match
     * @return The matched token, or null if no pattern matches
     */
    private static Token getToken(StringReader reader) throws MolangSyntaxException {
        String word = reader.getString().substring(reader.getCursor());
        if (word.isBlank()) {
            return null;
        }
        for (TokenType type : TokenType.values()) {
            Matcher matcher = type.pattern.matcher(word);
            if (matcher.find() && matcher.start() == 0) {
                reader.skip(matcher.end());
                return new Token(type, word.substring(0, matcher.end()));
            }
        }
        throw new MolangSyntaxException("Unknown Token", reader.getString(), reader.getCursor());
    }

    /**
     * Represents a single token with its type and the actual text from the source code.
     *
     * @param type  The classification of this token
     * @param value The raw text that was matched for this token
     */
    public record Token(TokenType type, String value) {
        @Override
        public String toString() {
            return this.type + "[" + this.value + "]";
        }
    }

    /**
     * Enumeration of all token types supported by the MoLang lexer.
     * <p>
     * Each token type is associated with a regex pattern used for matching.
     * The order of enum values matters, as they are tried in sequence during tokenization.
     */
    public enum TokenType {
        RETURN("return(?![A-Za-z0-9_])"),
        LOOP("loop(?![A-Za-z0-9_])"),
        CONTINUE("continue(?![A-Za-z0-9_])"),
        BREAK("break(?![A-Za-z0-9_])"),
        IF("if(?![A-Za-z0-9_])"),
        ELSE("else(?![A-Za-z0-9_])"),
        THIS("this(?![A-Za-z0-9_])"),
        TRUE("true(?![A-Za-z0-9_])"),
        FALSE("false(?![A-Za-z0-9_])"),
        STRING("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'"),
        NUMERAL("\\d+"),
        ALPHANUMERIC("[A-Za-z_][A-Za-z0-9_]*"),
        NULL_COALESCING("\\?\\?"),
        INCREMENT("\\+\\+"),
        DECREMENT("\\-\\-"),
        SPECIAL("[<>&|!?:]"),
        BINARY_OPERATION("[-+*/]"),
        LEFT_PARENTHESIS("\\("),
        RIGHT_PARENTHESIS("\\)"),
        LEFT_BRACE("\\{"),
        RIGHT_BRACE("\\}"),
        LEFT_BRACKET("\\["),
        RIGHT_BRACKET("\\]"),
        DOT("\\."),
        COMMA("\\,"),
        EQUAL("="),
        SEMICOLON(";");

        private final Pattern pattern;

        TokenType(String regex) {
            this.pattern = Pattern.compile(regex);
        }

        /**
         * Checks if this token type can be part of a variable name.
         * <p>
         * Variable names in MoLang can contain alphanumeric characters, underscores, dots, and numbers.
         * For example: {@code temp.player_health} or {@code variable.test123}
         *
         * @return true if this token can appear in a variable name
         */
        public boolean validVariableName() {
            return this == NUMERAL || this == ALPHANUMERIC || this == DOT;
        }

        /**
         * Checks if this token can be negated with a unary minus or plus.
         *
         * @return true if this token can follow a negation operator
         */
        public boolean canNegate() {
            return this == NUMERAL || this == ALPHANUMERIC || this == THIS;
        }

        /**
         * Checks if this token terminates a statement.
         * <p>
         * Currently, only semicolons terminate statements in MoLang.
         *
         * @return true if this token ends a statement
         */
        public boolean isTerminating() {
            return this == SEMICOLON;
        }

        /**
         * Checks if this token indicates the end of a scope or expression.
         * <p>
         * These tokens signal to the parser that it should stop parsing the current expression
         * and return control to the parent context.
         *
         * @return true if this token marks the end of a scope
         */
        public boolean isOutOfScope() {
            return this == RIGHT_PARENTHESIS || this == RIGHT_BRACE || this == RIGHT_BRACKET || this == COMMA;
        }
    }
}
