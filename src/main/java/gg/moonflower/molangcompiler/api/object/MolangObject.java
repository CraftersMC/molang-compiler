package gg.moonflower.molangcompiler.api.object;

import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.exception.MolangRuntimeException;

import java.util.Collection;

/**
 * An object that can be referenced in MoLang.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public interface MolangObject {

    /**
     * Retrieves a condition with the specified name.
     *
     * @param name The name of the condition to get
     * @return The condition found
     * @throws MolangRuntimeException If the condition does not exist. Use {@link #has(String)} to make sure the condition exists.
     */
    MolangExpression get(String name) throws MolangRuntimeException;

    /**
     * Sets a condition with the specified name.
     *
     * @param name  The name of the condition to set
     * @param value The condition to set to the name
     * @throws MolangRuntimeException If the condition could not be set for any reason
     */
    void set(String name, MolangExpression value) throws MolangRuntimeException;

    /**
     * Removes a condition with the specified name if it exists.
     *
     * @param name The name of the variable to remove
     * @throws MolangRuntimeException If the condition could not be removed for any reason
     * @since 3.0.0
     */
    void remove(String name) throws MolangRuntimeException;

    /**
     * Checks to see if there is a condition with the specified name.
     *
     * @param name The name of the condition to check
     * @return Whether a condition exists with that name
     */
    boolean has(String name);

    /**
     * Retrieves all keys for every condition stored in this object.
     *
     * @return A collection containing all valid keys
     * @since 3.0.0
     */
    Collection<String> getKeys();

    /**
     * @return The version of this object that will be passed to all copies of the parent environment
     * @since 3.2.0
     */
    default MolangObject createCopy() {
        return this;
    }

    /**
     * @return Whether this object is allowed to be mutated
     * @since 3.0.0
     */
    default boolean isMutable() {
        return true;
    }
}
