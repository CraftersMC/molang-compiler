package gg.moonflower.molangcompiler.impl.ast;

import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.impl.compiler.BytecodeCompiler;
import gg.moonflower.molangcompiler.impl.compiler.BytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.MethodNode;

/**
 * Compares the two values and runs an operation on them.
 *
 * @param operator The operator to apply
 * @param node     The operand
 * @author irrelevantdev
 */
@ApiStatus.Internal
public record UnaryOperationNode(UnaryOperation operator, Node node) implements Node {

    @Override
    public String toString() {
        return "(" + this.operator + " " + this.node + ")";
    }

    @Override
    public boolean isConstant() {
        return this.node.isConstant();
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public MolangValue evaluate(BytecodeEnvironment environment) throws MolangException {
        MolangValue value = this.node.evaluate(environment);
        return value.internalFlip();
    }

    @Override
    public void writeBytecode(MethodNode method, BytecodeCompiler compiler, BytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        if (isConstant()) {
            compiler.writeConst(method, this.evaluate(environment));
            return;
        }
        compiler.writeUnaryOperation(method, environment, breakLabel, continueLabel,
                node, operator);
    }

}
