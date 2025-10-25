package gg.moonflower.molangcompiler.impl.node;

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangValue;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public record MolangDynamicNode(Supplier<MolangValue> value) implements MolangExpression {

    @Override
    public MolangValue get(MolangEnvironment environment) {
        return this.value.get();
    }

    @Override
    public String toString() {
        return this.value.get().toString();
    }
}
