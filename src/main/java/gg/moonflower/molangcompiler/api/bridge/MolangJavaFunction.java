package gg.moonflower.molangcompiler.api.bridge;

import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.exception.MolangRuntimeException;

/**
 * Executes java code from MoLang expressions.
 *
 * @author Ocelot
 * @since 1.0.0
 */
@FunctionalInterface
public interface MolangJavaFunction {

    /**
     * Resolves a value from a set of parameters.
     *
     * @param context The parameters to execute using
     * @return The resulting value
     * @throws MolangRuntimeException If any error occurs
     * @since 4.0.0
     */
    MolangValue resolve(Context context) throws MolangRuntimeException;

    /**
     * Provides parameters for MoLang Java functions.
     *
     * @author Ocelot
     * @since 1.0.0
     */
    record Context(MolangValue[] parameters) {

        /**
         * Resolves the specified parameter as a MolangValue.
         *
         * @param parameter The parameter to resolve
         * @return The MolangValue result of that parameter
         * @throws MolangRuntimeException If the expression could not be resolved
         * @since 3.2.0
         */
        public MolangValue get(int parameter) throws MolangRuntimeException {
            if (parameter < 0 || parameter >= this.parameters.length) {
                throw new MolangRuntimeException("Invalid parameter: " + parameter);
            }
            return this.parameters[parameter];
        }

        /**
         * @return The number of parameters provided
         */
        public int getParameters() {
            return this.parameters.length;
        }
    }
}
