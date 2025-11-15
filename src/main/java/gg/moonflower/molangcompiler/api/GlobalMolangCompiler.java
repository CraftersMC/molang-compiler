package gg.moonflower.molangcompiler.api;

import gg.moonflower.molangcompiler.impl.CompilerFlag;
import gg.moonflower.molangcompiler.impl.CompilerFlags;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores a global instance of the compiler for ease of access.
 *
 * @author Ocelot
 * @since 3.0.0
 */
public final class GlobalMolangCompiler {

    private static final Map<CompilerFlags, MolangCompiler> GLOBAL_COMPILERS = new ConcurrentHashMap<>();

    /**
     * Retrieves a compiler with the {@linkplain CompilerFlags#DEFAULT default flags}.
     *
     * @return The compiler instance
     */
    public static MolangCompiler get() {
        return get(CompilerFlags.DEFAULT);
    }

    /**
     * Retrieves a compiler with the specified flags. If unsure use {@link #get()}.
     *
     * @param flags The compiler flags to use
     * @return The compiler instance
     * @see CompilerFlag
     */
    public static MolangCompiler get(CompilerFlags flags) {
        return GLOBAL_COMPILERS.computeIfAbsent(flags, MolangCompiler::create);
    }

    /**
     * Deletes the current instance of the compiler to allow compiled expression classes to be garbage collected.
     */
    public static void clear() {
        GLOBAL_COMPILERS.clear();
    }
}
