package gg.moonflower.molangcompiler.impl;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable set of compiler flags that control compilation behavior.
 * <p>
 * This class provides a type-safe alternative to integer bit flags,
 * making it easier to understand and maintain compiler configuration.
 * </p>
 *
 * @author irrelevantdev
 * @see CompilerFlag
 * @since 4.0.0
 */
public class CompilerFlags {

    /**
     * No compiler flags enabled.
     */
    public static final CompilerFlags NONE = new CompilerFlags();

    /**
     * Default compiler flags - includes {@link CompilerFlag#OPTIMIZE}.
     * <p>
     * This is the recommended configuration for production use.
     * </p>
     */
    public static final CompilerFlags DEFAULT = of(CompilerFlag.OPTIMIZE);

    private final Set<CompilerFlag> set;

    private CompilerFlags() {
        this.set = EnumSet.noneOf(CompilerFlag.class);
    }

    private CompilerFlags(CompilerFlag first, CompilerFlag... flags) {
        this.set = EnumSet.of(first, flags);
    }

    private CompilerFlags(Set<CompilerFlag> set) {
        this.set = set;
    }

    /**
     * Creates a new {@link CompilerFlags} instance with the specified flag added.
     * <p>
     * This method does not modify the current instance.
     * </p>
     *
     * @param flag The flag to add
     * @return A new CompilerFlags instance with the flag added
     */
    public CompilerFlags add(CompilerFlag flag) {
        var newFlags = EnumSet.copyOf(set);
        newFlags.add(flag);
        return new CompilerFlags(newFlags);
    }

    /**
     * Checks if this configuration contains the specified flag.
     *
     * @param flag The flag to check
     * @return true if the flag is enabled
     */
    public boolean contains(CompilerFlag flag) {
        return set.contains(flag);
    }

    /**
     * Creates a new {@link CompilerFlags} instance with the specified flags.
     *
     * @param first The first flag (required)
     * @param flags Additional flags (optional)
     * @return A new CompilerFlags instance
     */
    public static CompilerFlags of(CompilerFlag first, CompilerFlag... flags) {
        return new CompilerFlags(first, flags);
    }

    /**
     * Creates an empty {@link CompilerFlags} instance with no flags enabled.
     * <p>
     * Equivalent to {@link #NONE}.
     * </p>
     *
     * @return An empty CompilerFlags instance
     */
    public static CompilerFlags of() {
        return NONE;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        CompilerFlags that = (CompilerFlags) object;
        return Objects.equals(set, that.set);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(set);
    }
}
