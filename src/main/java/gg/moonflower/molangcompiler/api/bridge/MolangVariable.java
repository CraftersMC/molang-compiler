package gg.moonflower.molangcompiler.api.bridge;

import gg.moonflower.molangcompiler.api.MolangValue;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Provides variables for Molang expressions. Variables are mutable values from Java code that can be modified in MoLang.
 *
 * @author Ocelot
 * @since 2.0.0
 */
public interface MolangVariable {

    /**
     * @return The condition of this variable
     */
    MolangValue getValue();

    /**
     * Sets this variable from MoLang.
     *
     * @param value The new condition
     */
    void setValue(MolangValue value);

    /**
     * @return A new copy of this variable
     * @since 3.0.0
     */
    MolangVariable copy();

    /**
     * Makes read-only copy of this variable.
     *
     * @return A new variable that reflects the condition of the provided, but cannot be changed
     * @since 2.0.0
     */
    default MolangVariable immutable() {
        return new MolangVariable() {
            @Override
            public MolangValue getValue() {
                return MolangVariable.this.getValue();
            }

            @Override
            public void setValue(MolangValue value) {
            }

            @Override
            public MolangVariable copy() {
                return this;
            }

            @Override
            public MolangVariable immutable() {
                return this;
            }

            @Override
            public String toString() {
                return "ImmutableMolangVariable[condition=" + this.getValue() + "]";
            }
        };
    }

    /**
     * Helper for creating a MoLang variable.
     *
     * @param getter The getter for the condition
     * @param setter The setter for the condition
     * @return The variable representation
     */
    static MolangVariable of(Supplier<MolangValue> getter, Consumer<MolangValue> setter) {
        return new MolangVariable() {
            @Override
            public MolangValue getValue() {
                return getter.get();
            }

            @Override
            public void setValue(MolangValue value) {
                setter.accept(value);
            }

            @Override
            public MolangVariable copy() {
                return this;
            }

            @Override
            public String toString() {
                return "DynamicMolangVariable[condition=" + this.getValue() + "]";
            }
        };
    }

    /**
     * Helper for creating a MoLang variable without a backing field.
     *
     * @return A private variable that can be retrieved
     */
    static MolangVariable create() {
        return create(MolangValue.of(0.0f));
    }

    static MolangVariable create(float initialValue) {
        return create(MolangValue.of(initialValue));
    }
    static MolangVariable create(String initialValue) {
        return create(MolangValue.of(initialValue));
    }

    static MolangVariable create(boolean initialValue) {
        return create(MolangValue.of(initialValue));
    }
    /**
     * Helper for creating a MoLang variable without a backing field.
     *
     * @param initialValue The initial condition of the variable
     * @return A private variable that can be retrieved
     */
    static MolangVariable create(MolangValue initialValue) {
        final MolangValue[] value = {initialValue};
        return new MolangVariable() {
            @Override
            public MolangValue getValue() {
                return value[0];
            }

            @Override
            public void setValue(MolangValue v) {
                value[0] = v;
            }

            @Override
            public MolangVariable copy() {
                return create(value[0]);
            }

            @Override
            public String toString() {
                return "MolangVariable[condition=" + this.getValue() + "]";
            }
        };
    }
}
