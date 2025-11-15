package gg.moonflower.molangcompiler.impl.compiler;

import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import gg.moonflower.molangcompiler.impl.ast.Node;
import gg.moonflower.molangcompiler.impl.ast.ReturnNode;
import org.jetbrains.annotations.ApiStatus;

/**
 * Version-specific parser interface that transforms tokens into an Abstract Syntax Tree (AST).
 * <p>
 * Different versions can implement different parsing rules and syntax support.
 * </p>
 *
 * @author Ocelot
 * @since 4.0.0
 */
@ApiStatus.Internal
public interface MolangParser {

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
    Node parseTokens(MolangLexer.Token[] tokens) throws MolangSyntaxException;

}
