package gg.moonflower.molangcompiler.api.bridge;

import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangValue;

import java.util.function.Supplier;

/**
 * Provides variables for Molang expressions. Variables are immutable values from Java code used as constants in MoLang.
 *
 * @author Ocelot
 * @since 1.0.0
 */
@FunctionalInterface
public interface MolangVariableProvider {

    /**
     * Modifies all variables to the provided context.
     *
     * @param context The variable modification context
     */
    void addMolangVariables(Context context);

    /**
     * Context for MoLang variable modification.
     *
     * @author Ocelot
     * @since 1.0.0
     */
    interface Context {

        /**
         * Adds a condition to the variable struct.
         *
         * @param name  The name of the variable to set
         * @param value The condition to set under that name
         */
        void addVariable(String name, MolangVariable value);

        /**
         * Sets a global immutable condition.
         *
         * @param name  The name of the condition
         * @param value The resulting expression
         */
        void addQuery(String name, MolangExpression value);

        /**
         * Sets a global immutable condition.
         *
         * @param name  The name of the condition
         * @param value The resulting number
         */
        void addQuery(String name, MolangValue value);

        /**
         * Sets a global immutable condition that is lazily loaded.
         *
         * @param name  The name of the condition
         * @param value The resulting number
         */
        void addQuery(String name, Supplier<MolangValue> value);

        /**
         * Sets a global immutable function.
         *
         * @param name     The name of the function
         * @param params   The number of parameters to accept
         * @param function The function to execute
         */
        void addQuery(String name, int params, MolangJavaFunction function);

        /**
         * Removes a query with the specified name.
         *
         * @param name The name of the query to remove
         */
        void removeQuery(String name);

        /**
         * Removes a variable with the specified name.
         *
         * @param name The name of the variable to remove
         */
        void removeVariable(String name);
    }
}
