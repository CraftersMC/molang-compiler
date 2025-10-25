package gg.moonflower.molangcompiler.impl.ast;

import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.impl.compiler.BytecodeCompiler;
import gg.moonflower.molangcompiler.impl.compiler.MolangBytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

/**
 * Negates the specified boolean value.
 *
 * @param value The value to negate
 */
@ApiStatus.Internal
public record NegateNode(Node value) implements Node {

    @Override
    public String toString() {
        return "!" + this.value;
    }

    @Override
    public boolean isConstant() {
        return this.value.isConstant();
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public MolangValue evaluate(MolangBytecodeEnvironment environment) throws MolangException {
        return this.value.evaluate(environment).negate();
    }

    @Override
    public void writeBytecode(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        if (environment.optimize() && this.isConstant()) {
            BytecodeCompiler.writeConst(method, this.evaluate(environment));
            return;
        }

        this.value.writeBytecode(method, environment, breakLabel, continueLabel);
        BytecodeCompiler.writeNegate(
                method, environment, breakLabel, continueLabel,
                value
        );
    }
}
