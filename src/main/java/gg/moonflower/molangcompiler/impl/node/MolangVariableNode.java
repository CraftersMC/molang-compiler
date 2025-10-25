package gg.moonflower.molangcompiler.impl.node;

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.bridge.MolangVariable;
import gg.moonflower.molangcompiler.impl.MolangUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public record MolangVariableNode(MolangVariable value) implements MolangExpression, MolangVariable {

    @Override
    public MolangValue get(MolangEnvironment environment) {
        return this.value.getValue();
    }

    @Override
    public @NotNull String toString() {
        return MolangUtil.toString(this.value.getValue());
    }

    @Override
    public MolangValue getValue() {
        return this.value.getValue();
    }

    @Override
    public void setValue(MolangValue value) {
        this.value.setValue(value);
    }

    @Override
    public MolangExpression createCopy() {
        return new MolangVariableNode(this.copy());
    }

    @Override
    public MolangVariable copy() {
        return this.value.copy();
    }
}
