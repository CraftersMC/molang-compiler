package gg.moonflower.molangcompiler.api;

import gg.moonflower.molangcompiler.api.exception.UnsupportedMolangVersionException;
import gg.moonflower.molangcompiler.impl.CompilerFlags;
import gg.moonflower.molangcompiler.impl.compiler.BytecodeCompiler;
import gg.moonflower.molangcompiler.impl.compiler.BytecodeCompilerV12;
import gg.moonflower.molangcompiler.impl.compiler.MolangParser;
import gg.moonflower.molangcompiler.impl.compiler.MolangParserV12;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Represents a MoLang compiler version. Different versions can parse and generate code differently.
 * <p>
 * Version numbers allow the compiler to maintain backward compatibility while introducing new
 * features or syntax changes in future versions.
 * </p>
 *
 * @author irrelevantdev
 * @since 4.0.0
 */
public final class MolangVersion {

    private static final Map<Integer, MolangVersion> VERSIONS = new LinkedHashMap<>();
    public static final MolangVersion LATEST;

    static {
        LATEST = register(12,
                MolangParserV12::new,
                BytecodeCompilerV12::new);
    }

    @Getter
    private final int version;
    @Getter
    private final MolangParser parser;
    private final BiFunction<CompilerFlags, ClassLoader, BytecodeCompiler> compilerFactory;

    /**
     * Creates a new MolangVersion with the specified version number and version-specific components.
     *
     * @param version         The version number
     * @param parserFactory   Factory to create the version-specific parser
     * @param compilerFactory Factory to create the version-specific bytecode compiler
     */
    private MolangVersion(int version,
                          Supplier<MolangParser> parserFactory,
                          BiFunction<CompilerFlags, ClassLoader, BytecodeCompiler> compilerFactory) {
        this.version = version;
        this.parser = parserFactory.get();
        this.compilerFactory = compilerFactory;
    }

    /**
     * Registers a new version with its version-specific components.no
     *
     * @param version         The version number
     * @param parserFactory   Factory to create the version-specific parser
     * @param compilerFactory Factory to create the version-specific bytecode compiler
     * @return The registered MolangVersion instance
     */
    private static MolangVersion register(int version,
                                          Supplier<MolangParser> parserFactory,
                                          BiFunction<CompilerFlags, ClassLoader, BytecodeCompiler> compilerFactory) {
        MolangVersion molangVersion = new MolangVersion(version, parserFactory, compilerFactory);
        VERSIONS.put(version, molangVersion);
        return molangVersion;
    }

    /**
     * Creates a version-specific bytecode compiler with the specified flags and parent class loader.
     *
     * @param flags  Compilation flags
     * @param parent The parent class loader
     * @return A new bytecode compiler instance
     */
    public BytecodeCompiler createBytecodeCompiler(CompilerFlags flags, ClassLoader parent) {
        return this.compilerFactory.apply(flags, parent);
    }

    /**
     * Creates a MolangVersion from the specified version number.
     *
     * @param version The version number
     * @return The MolangVersion instance
     */
    public static MolangVersion get(int version) throws UnsupportedMolangVersionException {
        MolangVersion instance = VERSIONS.get(version);
        if (instance == null) {
            throw new UnsupportedMolangVersionException(version + " is not implemented");
        }
        return instance;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MolangVersion other)) return false;
        return this.version == other.version;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.version);
    }

    @Override
    public String toString() {
        return "MolangVersion{v" + this.version + "}";
    }
}
