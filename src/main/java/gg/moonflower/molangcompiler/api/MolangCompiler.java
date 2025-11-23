package gg.moonflower.molangcompiler.api;

import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import gg.moonflower.molangcompiler.impl.MolangCompilerImpl;

/**
 * <p>Compiles a {@link MolangExpression} from a string input.</p>
 * <p>A compiler instance must be created to allow garbage collection of generated classes when no longer in use.</p>
 *
 * @author Ocelot
 * @see GlobalMolangCompiler
 * @since 3.0.0
 */
public interface MolangCompiler {

    /**
     * Compiles a {@link MolangExpression} from the specified string input.
     *
     * @param input The data to compile
     * @return The compiled expression
     * @throws MolangSyntaxException If any error occurs
     */
    MolangExpression compile(String input, MolangVersion version) throws MolangSyntaxException;

    /**
     * Compiles a {@link MolangExpression} from the specified string input.
     *
     * @param input The data to compile
     * @return The compiled expression
     * @throws MolangSyntaxException If any error occurs
     */
    default MolangExpression compile(String input) throws MolangSyntaxException {
        return compile(input, MolangVersion.LATEST);
    }

    /**
     * Creates a compiler with the {@linkplain CompilerFlags#DEFAULT default flags}.
     *
     * @return The compiler instance
     */
    static MolangCompiler create() {
        return new MolangCompilerImpl(CompilerFlags.DEFAULT);
    }

    /**
     * Creates a compiler with the specified flags.
     *
     * @param flags The compiler flags to use
     * @return The compiler instance
     * @see CompilerFlag
     */
    static MolangCompiler create(CompilerFlags flags) {
        return new MolangCompilerImpl(flags);
    }

    /**
     * Creates a compiler with the specified flags.
     *
     * @param flags  The compiler flags to use
     * @param parent The classloader to use as the parent.
     *               This should only be set when the current class is not using the system class loader
     * @return The compiler instance
     * @see CompilerFlag
     */
    static MolangCompiler create(CompilerFlags flags, ClassLoader parent) {
        return new MolangCompilerImpl(flags, parent);
    }
}
