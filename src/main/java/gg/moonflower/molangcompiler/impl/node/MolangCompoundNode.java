package gg.moonflower.molangcompiler.impl.node;

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.exception.MolangRuntimeException;
import org.jetbrains.annotations.ApiStatus;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public record MolangCompoundNode(MolangExpression... expressions) implements MolangExpression {

    @Override
    public MolangValue get(MolangEnvironment environment) throws MolangRuntimeException {
        for (int i = 0; i < this.expressions.length; i++) {
            MolangValue result = environment.resolve(this.expressions[i]);
            // The last expression is expected to have the `return`
            if (i >= this.expressions.length - 1) {
                return result;
            }
        }
        return MolangValue.of(0.0F);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.expressions.length; i++) {
            if (i >= this.expressions.length - 1) {
                builder.append("return ");
            }
            builder.append(this.expressions[i]);
            builder.append(';');
            if (i < this.expressions.length - 1) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }
}
