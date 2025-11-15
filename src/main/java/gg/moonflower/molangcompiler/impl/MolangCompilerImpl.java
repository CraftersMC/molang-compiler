package gg.moonflower.molangcompiler.impl;

import gg.moonflower.molangcompiler.api.MolangCompiler;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangVersion;
import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import gg.moonflower.molangcompiler.impl.ast.Node;
import gg.moonflower.molangcompiler.impl.compiler.BytecodeCompiler;
import gg.moonflower.molangcompiler.impl.compiler.MolangLexer;
import gg.moonflower.molangcompiler.impl.compiler.MolangParser;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class MolangCompilerImpl implements MolangCompiler {

    private final CompilerFlags flags;
    private final ClassLoader classLoader;
    private final Map<MolangVersion, BytecodeCompiler> compilerCache = new HashMap<>();
    private final Function<MolangVersion, BytecodeCompiler> mappingFunction;

    public MolangCompilerImpl(CompilerFlags flags) {
        this(flags, ClassLoader.getSystemClassLoader());
    }

    public MolangCompilerImpl(CompilerFlags flags, ClassLoader classLoader) {
        this.flags = flags;
        this.classLoader = classLoader;
        this.mappingFunction = v -> v.createBytecodeCompiler(this.flags, this.classLoader);
    }

    @Override
    public MolangExpression compile(String input, MolangVersion version) throws MolangSyntaxException {
        // Lex the input into tokens
        MolangLexer.Token[] tokens = MolangLexer.createTokens(input);

        // Use the version-specific parser to parse tokens into an AST
        MolangParser parser = version.getParser();
        Node node = parser.parseTokens(tokens);

        // Use the version-specific bytecode compiler to compile the AST
        BytecodeCompiler compiler = compilerCache.computeIfAbsent(version, mappingFunction);
        return compiler.build(node);
    }
}
