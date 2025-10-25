package gg.moonflower.molangcompiler.impl.compiler;

import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import gg.moonflower.molangcompiler.impl.ast.*;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Recursive descent parser that transforms tokens into an Abstract Syntax Tree (AST).
 * <p>
 * This parser converts the flat array of tokens produced by {@link MolangLexer} into a hierarchical
 * tree structure of {@link Node} objects that can be evaluated or compiled to bytecode.
 * <p>
 * The parser implements operator precedence for mathematical expressions:
 * <ul>
 *   <li>Lowest: Logical OR (||), Logical AND (&amp;&amp;)</li>
 *   <li>Comparison: ==, !=, &lt;, &gt;, &lt;=, &gt;=</li>
 *   <li>Addition: +, -</li>
 *   <li>Multiplication: *, /</li>
 *   <li>Highest: Unary negation (!), parentheses, function calls</li>
 * </ul>
 * <p>
 * Supported language constructs include:
 * <ul>
 *   <li>Variable access and assignment: {@code temp.x}, {@code variable.y = 5}</li>
 *   <li>Function calls: {@code math.sin(condition)}, {@code query.is_on_ground()}</li>
 *   <li>Control flow: {@code if(condition) condition}, ternary {@code condition ? true : false}</li>
 *   <li>Loops: {@code loop(10, { statements })}</li>
 *   <li>Break, continue, and return statements</li>
 * </ul>
 *
 * @author Ocelot
 * @since 1.0.0
 */
@ApiStatus.Internal
public final class MolangParser {

    /**
     * Parses an array of tokens into an Abstract Syntax Tree.
     * <p>
     * This is the main entry point for the parser. It automatically wraps the last expression
     * in a {@link ReturnNode} to ensure the expression produces a condition.
     *
     * @param tokens The array of tokens to parse (must not be empty)
     * @return The root node of the AST
     * @throws MolangSyntaxException if the tokens cannot be parsed or are invalid
     */
    public static Node parseTokens(MolangLexer.Token[] tokens) throws MolangSyntaxException {
        if (tokens.length == 0) {
            throw new MolangSyntaxException("Expected token");
        }
        return parseTokensUntil(new TokenReader(tokens), true, token -> false);
    }

    /**
     * Parses tokens until a specific condition is met, optionally wrapping the result in a return statement.
     * <p>
     * This method handles parsing multiple semicolon-separated statements and combines them into
     * a {@link CompoundNode} if there are multiple statements.
     *
     * @param reader       The token reader positioned at the start of the tokens to parse
     * @param insertReturn If true, automatically wraps the last statement in a {@link ReturnNode}
     * @param filter       Predicate that returns true when parsing should stop
     * @return The parsed node (either a single node or a CompoundNode containing multiple statements)
     * @throws MolangSyntaxException if parsing fails
     */
    private static Node parseTokensUntil(TokenReader reader, boolean insertReturn, Predicate<MolangLexer.Token> filter) throws MolangSyntaxException {
        List<Node> nodes = new ArrayList<>(2);

        while (reader.canRead() && !filter.test(reader.peek())) {
            Node node = parseExpression(reader);
            nodes.add(node);

            if (reader.canRead()) {
                MolangLexer.Token token = reader.peek();
                if (token.type().isTerminating()) {
                    reader.skip();
                    continue;
                }
                if (filter.test(token)) {
                    break;
                }
                throw error("Trailing statement", reader);
            }
        }

        if (nodes.isEmpty()) {
            throw new MolangSyntaxException("Expected node");
        }
        if (insertReturn) {
            Node node = nodes.get(nodes.size() - 1);
            if (!(node instanceof ReturnNode)) {
                if (node instanceof OptionalValueNode setNode) {
                    node = setNode.withReturnValue();
                }
                nodes.set(nodes.size() - 1, new ReturnNode(node));
            }
        }
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        return new CompoundNode(nodes.toArray(Node[]::new));
    }

    /**
     * Parses a single node from the token stream.
     * <p>
     * This method handles the parsing of atomic language constructs such as:
     * <ul>
     *   <li>Literals: numbers, strings, booleans</li>
     *   <li>Variables: {@code temp.a}, {@code variable.test}</li>
     *   <li>Control flow: {@code return}, {@code if}, {@code loop}, {@code break}, {@code continue}</li>
     *   <li>Parenthesized expressions: {@code (expression)}</li>
     *   <li>Scopes: {@code { statements }}</li>
     * </ul>
     *
     * @param reader The token reader positioned at the node to parse
     * @return The parsed node
     * @throws MolangSyntaxException if parsing fails
     */
    private static Node parseNode(TokenReader reader) throws MolangSyntaxException {
        expectLength(reader, 1);

        MolangLexer.Token token = reader.peek();
        return switch (token.type()) {
            case RETURN -> {
                reader.skip();

                Node value = parseExpression(reader);
                // Skip ;
                if (reader.canRead() && reader.peek().type().isTerminating()) {
                    reader.skip();
                }
                // Expect end
                boolean scope = reader.canRead() && reader.peek().type() == MolangLexer.TokenType.RIGHT_BRACE;
                if (reader.canRead() && !scope) {
                    throw error("Trailing statement", reader);
                }
                if (value instanceof OptionalValueNode setNode) {
                    value = setNode.withReturnValue();
                }
                yield new ReturnNode(value);
            }
            case LOOP -> {
                reader.skip();
                expect(reader, MolangLexer.TokenType.LEFT_PARENTHESIS);
                reader.skip();

                Node iterations = parseTokensUntil(reader, false, t -> t.type() == MolangLexer.TokenType.COMMA);
                expect(reader, MolangLexer.TokenType.COMMA);
                reader.skip();

                Node body = parseTokensUntil(reader, false, t -> t.type() == MolangLexer.TokenType.RIGHT_PARENTHESIS);
                expect(reader, MolangLexer.TokenType.RIGHT_PARENTHESIS);
                reader.skip();

                // Ignore the top level scope since the loop is already a "scope"
                yield new LoopNode(iterations, body instanceof ScopeNode scopeNode ? scopeNode.node() : body);
            }
            case CONTINUE -> {
                reader.skip();
                yield new ContinueNode();
            }
            case BREAK -> {
                reader.skip();
                yield new BreakNode();
            }
            case IF -> {
                reader.skip();
                expect(reader, MolangLexer.TokenType.LEFT_PARENTHESIS);
                reader.skip();

                // if(condition)
                Node condition = parseExpression(reader);

                expect(reader, MolangLexer.TokenType.RIGHT_PARENTHESIS);
                reader.skip();

                Node branch = parseExpression(reader);
                if (reader.canRead(2) && reader.peek().type().isTerminating() && reader.peekAfter(1).type() == MolangLexer.TokenType.ELSE) {
                    reader.skip(2);
                    yield new TernaryOperationNode(condition, branch, parseExpression(reader));
                }

                // condition ? left
                yield new BinaryConditionalNode(condition, branch);
            }
            case THIS -> {
                reader.skip();
                yield new ThisNode();
            }
            case TRUE -> {
                reader.skip();
                yield new ConstNode(MolangValue.of(true));
            }
            case FALSE -> {
                reader.skip();
                yield new ConstNode(MolangValue.of(false));
            }
            case STRING -> {
                String rawValue = reader.peek().value();
                reader.skip();

                // Remove quotes (first and last character)
                String stringValue = rawValue.substring(1, rawValue.length() - 1);

                // Process escape sequences
                stringValue = stringValue
                    .replace("\\\"", "\"")
                    .replace("\\'", "'")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");

                yield new ConstNode(MolangValue.of(stringValue));
            }
            case NUMERAL -> {
                try {
                    // Read until decimal point
                    float value = Integer.parseInt(reader.peek().value());
                    reader.skip();

                    // Read decimals
                    if (reader.canRead() && reader.peek().type() == MolangLexer.TokenType.DOT) {
                        reader.skip();
                        expect(reader, MolangLexer.TokenType.NUMERAL);

                        String decimalString = reader.peek().value();
                        float decimal = Integer.parseInt(decimalString);
                        reader.skip();

                        if (decimal > 0) {
                            value += (float) (decimal / Math.pow(10, decimalString.length()));
                        }
                    }

                    yield new ConstNode(MolangValue.of(value));
                } catch (Exception e) {
                    e.printStackTrace();
                    throw error("Error parsing numeral", reader);
                }
            }
            case ALPHANUMERIC -> parseAlphanumeric(reader);
            case BINARY_OPERATION -> {
                switch (token.value()) {
                    case "-" -> {
                        if (!reader.canRead(2) || !reader.peekAfter(1).type().canNegate()) {
                            throw error("Cannot negate " + reader.peekAfter(1), reader);
                        }
                        reader.skip();
                        yield new BinaryOperationNode(BinaryOperation.MULTIPLY, new ConstNode(MolangValue.of(-1.0f)), parseNode(reader));
                    }
                    case "+" -> {
                        if (!reader.canRead(2) || !reader.peekAfter(1).type().canNegate()) {
                            throw error("Cannot assign + to " + reader.peekAfter(1), reader);
                        }
                        reader.skip();
                        yield parseNode(reader);
                    }
                    default -> throw error("Expected +num or -num", reader);
                }
            }
            case LEFT_PARENTHESIS -> {
                reader.skip();
                Node node = parseExpression(reader);
                expect(reader, MolangLexer.TokenType.RIGHT_PARENTHESIS);
                reader.skip();
                yield node;
            }
            case LEFT_BRACE -> {
                reader.skip();
                Node node = parseTokensUntil(reader, false, t -> t.type() == MolangLexer.TokenType.RIGHT_BRACE);
                expect(reader, MolangLexer.TokenType.RIGHT_BRACE);
                reader.skip();
                yield new ScopeNode(node);
            }
            case SPECIAL -> {
                switch (token.value()) {
                    case "!" -> {
                        reader.skip();
                        // Unary operation node
                        yield new UnaryOperationNode(UnaryOperation.FLIP, parseNode(reader));
                    }
                    default -> throw error("Unexpected token", reader);
                }
            }
            default -> throw error("Unexpected token", reader);
        };
    }

    /**
     * Parses a complete expression, handling operators and building the expression tree.
     * <p>
     * This method implements operator precedence by recursively parsing nodes and combining
     * them based on the operators encountered. It handles binary operations (addition, subtraction,
     * multiplication, division), comparison operators, logical operators, and the ternary operator.
     * <p>
     * The method starts by parsing a single node and then repeatedly looks for operators to combine
     * it with subsequent nodes, respecting operator precedence rules.
     *
     * @param reader The token reader positioned at the start of the expression
     * @return The root node of the parsed expression tree
     * @throws MolangSyntaxException if the expression is malformed
     */
    public static Node parseExpression(TokenReader reader) throws MolangSyntaxException {
        Node result = parseNode(reader);
        while (reader.canRead()) {
            MolangLexer.Token token = reader.peek();
            if (token.type() == MolangLexer.TokenType.SEMICOLON || token.type().isOutOfScope()) {
                return result;
            }

            if (result instanceof OptionalValueNode setNode) {
                result = setNode.withReturnValue();
            }

            switch (token.type()) {
                case NUMERAL, ALPHANUMERIC, LEFT_PARENTHESIS, LEFT_BRACE, STRING -> {
                    if (result != null) {
                        throw error("Unexpected token", reader);
                    }
                    result = parseNode(reader);
                }
                case NULL_COALESCING -> {
                    reader.skip();
                    result = new BinaryOperationNode(BinaryOperation.NULL_COALESCING, result, parseNode(reader));
                }
                case EQUAL -> {
                    reader.skip();
                    expect(reader, MolangLexer.TokenType.EQUAL);
                    reader.skip();
                    result = new BinaryOperationNode(BinaryOperation.EQUALS, result, parseNode(reader));
                }
                case SPECIAL -> {
                    switch (token.value()) {
                        // obj.name&&...
                        case "&" -> {
                            expect(reader, MolangLexer.TokenType.SPECIAL, "&");
                            reader.skip(2);
                            result = new BinaryOperationNode(BinaryOperation.AND, result, parseNode(reader));
                        }
                        // obj.name||...
                        case "|" -> {
                            expect(reader, MolangLexer.TokenType.SPECIAL, "|");
                            reader.skip(2);
                            result = new BinaryOperationNode(BinaryOperation.OR, result, parseNode(reader));
                        }
                        // obj.name??... or obj.name?b...
                        case "?" -> {
                            reader.skip();

                            // condition ? left : right
                            Node left = parseExpression(reader);
                            if (reader.canRead() && !reader.peek().type().isTerminating()) {
                                expect(reader, MolangLexer.TokenType.SPECIAL, ":");
                                reader.skip();
                                result = new TernaryOperationNode(result, left, parseExpression(reader));
                                break;
                            }

                            // condition ? left
                            result = new BinaryConditionalNode(result, left);
                        }
                        case "!" -> {
                            reader.skip();

                            if (reader.peek().type() == MolangLexer.TokenType.EQUAL) {
                                reader.skip();
                                result = new BinaryOperationNode(BinaryOperation.NOT_EQUALS, result, parseNode(reader));
                                break;
                            }

                            result = new NegateNode(parseNode(reader));
                        }
                        case ">" -> {
                            reader.skip();

                            if (reader.peek().type() == MolangLexer.TokenType.EQUAL) {
                                reader.skip();
                                result = new BinaryOperationNode(BinaryOperation.GREATER_EQUALS, result, parseNode(reader));
                                break;
                            }

                            result = new BinaryOperationNode(BinaryOperation.GREATER, result, parseNode(reader));
                        }
                        case "<" -> {
                            reader.skip();

                            if (reader.peek().type() == MolangLexer.TokenType.EQUAL) {
                                reader.skip();
                                result = new BinaryOperationNode(BinaryOperation.LESS_EQUALS, result, parseNode(reader));
                                break;
                            }

                            result = new BinaryOperationNode(BinaryOperation.LESS, result, parseNode(reader));
                        }
                        default -> {
                            return result;
                        }
                    }
                }
                case BINARY_OPERATION -> {
                    if (result == null) {
                        throw error("Unexpected token", reader);
                    }
                    result = parseBinaryExpression(result, reader);
                }
                default -> throw error("Unexpected token: " + token, reader);
            }
        }

        return result;
    }

    /**
     * Parses variable access, assignment, and function calls starting with an alphanumeric token.
     * <p>
     * This method handles complex patterns such as:
     * <ul>
     *   <li>Variable get: {@code temp.x}, {@code query.position}</li>
     *   <li>Variable set: {@code variable.y = 5}, {@code temp.z += 10}</li>
     *   <li>Increment/Decrement: {@code temp.counter++}, {@code temp.condition--}</li>
     *   <li>Function calls: {@code math.sin(condition)}, {@code query.health(param1, param2)}</li>
     * </ul>
     *
     * @param reader The token reader positioned at the alphanumeric token
     * @return The parsed node representing the variable access or function call
     * @throws MolangSyntaxException if parsing fails
     */
    private static Node parseAlphanumeric(TokenReader reader) throws MolangSyntaxException {
        expectLength(reader, 2);

        // object.name
        String object = reader.peek().value();
        if ("t".equalsIgnoreCase(object)) {
            object = "temp";
        }

        reader.skip();
        expect(reader, MolangLexer.TokenType.DOT);
        reader.skip();

        expect(reader, MolangLexer.TokenType.ALPHANUMERIC);
        StringBuilder nameBuilder = new StringBuilder(reader.peek().value());
        reader.skip();
        while (reader.canRead()) {
            MolangLexer.Token token = reader.peek();
            if (!token.type().validVariableName()) {
                break;
            }
            nameBuilder.append(token.value());
            reader.skip();
        }

        String name = nameBuilder.toString();

        MathOperation mathOperation = parseMathOperation(object, name, reader);
        if (mathOperation != null && mathOperation.getParameters() == 0) {
            return new MathNode(mathOperation);
        }

        // obj.name
        if (!reader.canRead() || reader.peek().type().isTerminating()) {
            if (mathOperation != null) {
                throw error("Cannot get condition of a math function", reader);
            }
            return new VariableGetNode(object, name);
        }

        MolangLexer.Token operand = reader.peek();

        // obj.name=...
        if (operand.type() == MolangLexer.TokenType.EQUAL) {
            // obj.name==...
            if (reader.canRead() && reader.peekAfter(1).type() == MolangLexer.TokenType.EQUAL) {
                // == will be handled by the next step
                return new VariableGetNode(object, name);
            }

            if (mathOperation != null) {
                throw error("Cannot set value of a math function", reader);
            }
            reader.skip();
            return new VariableSetNode(object, name, parseExpression(reader));
        }
        // obj.name++
        if (operand.type() == MolangLexer.TokenType.INCREMENT) {
            reader.skip();
            return new VariableSetNode(object, name, new BinaryOperationNode(BinaryOperation.ADD, new VariableGetNode(object, name), new ConstNode(MolangValue.of(1.0f))));
        }
        // obj.name--
        if (operand.type() == MolangLexer.TokenType.DECREMENT) {
            reader.skip();
            return new VariableSetNode(object, name, new BinaryOperationNode(BinaryOperation.SUBTRACT, new VariableGetNode(object, name), new ConstNode(MolangValue.of(1.0f))));
        }
        // obj.name*=, obj.name+=, obj.name-=, obj.name/=
        if (reader.canRead(2) && operand.type() == MolangLexer.TokenType.BINARY_OPERATION) {
            if (mathOperation != null) {
                throw error("Cannot set condition of a math function", reader);
            }

            VariableGetNode left = new VariableGetNode(object, name);
            MolangLexer.Token secondOperand = reader.peekAfter(1);

            // +=, -=, *=, /=
            if (secondOperand.type() == MolangLexer.TokenType.EQUAL) {
                reader.skip(2);
                Node value = switch (operand.value()) {
                    case "-" -> new BinaryOperationNode(BinaryOperation.SUBTRACT, left, parseExpression(reader));
                    case "+" -> new BinaryOperationNode(BinaryOperation.ADD, left, parseExpression(reader));
                    case "*" -> new BinaryOperationNode(BinaryOperation.MULTIPLY, left, parseExpression(reader));
                    case "/" -> new BinaryOperationNode(BinaryOperation.DIVIDE, left, parseExpression(reader));
                    default -> throw error("Unexpected token", reader);
                };
                return new VariableSetNode(object, name, value);
            }
        }
        // obj.func(..
        if (operand.type() == MolangLexer.TokenType.LEFT_PARENTHESIS) {
            reader.skip();

            // obj.func()
            if (reader.peek().type() == MolangLexer.TokenType.RIGHT_PARENTHESIS) {
                reader.skip();
                // Validate number of parameters for math functions
                if (mathOperation != null) {
                    throw error("Expected " + mathOperation.getParameters() + " parameters, got none", reader);
                }
                return new FunctionNode(object, name);
            }

            // obj.func(a, b, ...)
            List<Node> parameters = new ArrayList<>();
            while (reader.canRead()) {
                parameters.add(parseExpression(reader));

                if (!reader.canRead()) {
                    expect(reader, MolangLexer.TokenType.RIGHT_PARENTHESIS);
                }
                if (reader.peek().type() == MolangLexer.TokenType.COMMA) {
                    reader.skip();
                    continue;
                }

                expect(reader, MolangLexer.TokenType.RIGHT_PARENTHESIS);
                reader.skip();

                // Validate number of parameters for math functions
                if (mathOperation != null) {
                    if (mathOperation.getParameters() != parameters.size()) {
                        throw error("Expected " + mathOperation.getParameters() + " parameters, got " + parameters.size(), reader);
                    }
                    return new MathNode(mathOperation, parameters.toArray(Node[]::new));
                }

                return new FunctionNode(object, name, parameters.toArray(Node[]::new));
            }
            expectLength(reader, 1);
        }

        return new VariableGetNode(object, name);
    }

    /**
     * Attempts to parse a math operation if the object is "math".
     * <p>
     * Validates that the function name corresponds to a known math operation in the
     * {@link MathOperation} enum.
     *
     * @param object The object name (e.g., "math", "query")
     * @param name   The function name (e.g., "sin", "cos", "abs")
     * @param reader The token reader for error reporting
     * @return The matching MathOperation, or null if the object is not "math"
     * @throws MolangSyntaxException if the object is "math" but the function name is unknown
     */
    private static MathOperation parseMathOperation(String object, String name, TokenReader reader) throws MolangSyntaxException {
        if (!"math".equalsIgnoreCase(object)) {
            return null;
        }

        for (MathOperation operation : MathOperation.values()) {
            if (operation.getName().equalsIgnoreCase(name)) {
                return operation;
            }
        }
        throw error("Unknown math function: " + name, reader);
    }

    /**
     * Parses a binary operation (addition, subtraction, multiplication, division) with proper precedence.
     * <p>
     * This method handles multiplication and division with higher precedence than addition and subtraction
     * by calling {@link #parseTerm(TokenReader)} for additive operations.
     *
     * @param left   The left operand node
     * @param reader The token reader positioned at the operator
     * @return The binary operation node
     * @throws MolangSyntaxException if parsing fails
     */
    private static Node parseBinaryExpression(Node left, TokenReader reader) throws MolangSyntaxException {
        MolangLexer.Token token = reader.peek();
        switch (token.value()) {
            case "+" -> {
                reader.skip();
                return new BinaryOperationNode(BinaryOperation.ADD, left, parseTerm(reader));
            }
            case "-" -> {
                reader.skip();
                return new BinaryOperationNode(BinaryOperation.SUBTRACT, left, parseTerm(reader));
            }
            case "*" -> {
                reader.skip();
                return new BinaryOperationNode(BinaryOperation.MULTIPLY, left, parseNode(reader));
            }
            case "/" -> {
                reader.skip();
                return new BinaryOperationNode(BinaryOperation.DIVIDE, left, parseNode(reader));
            }
        }
        return left;
    }

    /**
     * Parses a term with multiplication and division operations.
     * <p>
     * This method implements higher precedence for multiplication and division operations
     * compared to addition and subtraction. It's called by {@link #parseBinaryExpression(Node, TokenReader)}
     * when parsing additive operations.
     *
     * @param reader The token reader positioned at the start of the term
     * @return The parsed term node
     * @throws MolangSyntaxException if parsing fails
     */
    private static Node parseTerm(TokenReader reader) throws MolangSyntaxException {
        Node left = parseNode(reader);
        if (!reader.canRead()) {
            return left;
        }

        MolangLexer.Token token = reader.peek();
        if (token.type() == MolangLexer.TokenType.BINARY_OPERATION) {
            switch (token.value()) {
                case "*" -> {
                    reader.skip();
                    return new BinaryOperationNode(BinaryOperation.MULTIPLY, left, parseNode(reader));
                }
                case "/" -> {
                    reader.skip();
                    return new BinaryOperationNode(BinaryOperation.DIVIDE, left, parseNode(reader));
                }
            }
        }
        return left;
    }

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
