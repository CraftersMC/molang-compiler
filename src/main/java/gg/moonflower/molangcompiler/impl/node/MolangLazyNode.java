package gg.moonflower.molangcompiler.impl.node;

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangValue;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class MolangLazyNode implements MolangExpression {

    private final Supplier<MolangValue> value;

    public MolangLazyNode(Supplier<MolangValue> value) {
        this.value = new Supplier<>() {
            private MolangValue result = null;

            @Override
            public MolangValue get() {
                if (this.result == null) {
                    this.result = value.get();
                }
                return this.result;
            }
        };
    }

    @Override
    public MolangValue get(MolangEnvironment environment) {
        return this.value.get();
    }

    @Override
    public String toString() {
        return this.value.get().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        MolangLazyNode that = (MolangLazyNode) o;
        return this.value.get().equals(that.value.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value.get());
    }
}
