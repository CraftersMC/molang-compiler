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
 * Performs an "if" check on the specified value and executes the branch if it passes.
 *
 * @param condition  The value to check. If not zero it is considered <code>true</code>
 * @param branch The value to use when the check passes
 * @author Buddy
 */
@ApiStatus.Internal
public record BinaryConditionalNode(Node condition, Node branch) implements Node {

    @Override
    public String toString() {
        return this.condition + " ? " + this.branch;
    }

    @Override
    public boolean isConstant() {
        return this.condition.isConstant();
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public MolangValue evaluate(MolangBytecodeEnvironment environment) throws MolangException {
        if (this.condition.evaluate(environment).asBoolean()) {
            return this.branch.evaluate(environment);
        }
        return MolangValue.of(0.0f);
    }

    @Override
    public void writeBytecode(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        if (environment.optimize() && this.condition.isConstant()) {
            if (this.condition.evaluate(environment).asBoolean()) {
                this.branch.writeBytecode(method, environment, breakLabel, continueLabel);
            }
            return;
        }
        Label label_end = new Label();
        this.condition.writeBytecode(method, environment, breakLabel, continueLabel);
        BytecodeCompiler.unwrapBool(method);
        method.visitJumpInsn(Opcodes.IFEQ, label_end);
        {
            MolangBytecodeEnvironment localEnvironment = new MolangBytecodeEnvironment(environment);
            this.branch.writeBytecode(method, localEnvironment, breakLabel, continueLabel);
            if (this.branch.hasValue() && !this.hasValue()) {
                method.visitInsn(Opcodes.POP);
            }
            localEnvironment.writeModifiedVariables(method);
        }
        method.visitLabel(label_end);
    }
}
