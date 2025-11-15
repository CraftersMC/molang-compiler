package gg.moonflower.molangcompiler.impl.ast;

import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.impl.compiler.BytecodeCompiler;
import gg.moonflower.molangcompiler.impl.compiler.BytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

/**
 * Returns the condition on the stack from the specified condition.
 *
 * @param value The condition to return
 * @author Buddy
 */
@ApiStatus.Internal
public record ReturnNode(Node value) implements Node {

    @Override
    public String toString() {
        return "return " + this.value.toString();
    }

    @Override
    public boolean isConstant() {
        return this.value.isConstant();
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public MolangValue evaluate(BytecodeEnvironment environment) throws MolangException {
        return this.value.evaluate(environment);
    }

    @Override
    public void writeBytecode(MethodNode method, BytecodeCompiler compiler, BytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        // Write the condition to return
        if (compiler.isOptimizationEnabled() && this.isConstant()) {
            compiler.writeConst(method, this.evaluate(environment));
        } else {
            this.value.writeBytecode(method, compiler, environment, breakLabel, continueLabel);
            if (!this.value.hasValue()) {
                ConstNode.ZERO_FLOAT_NODE.writeBytecode(method, compiler, environment, breakLabel, continueLabel);
            }
        }
        environment.writeModifiedVariables(method);
        method.visitInsn(Opcodes.ARETURN);
    }
}