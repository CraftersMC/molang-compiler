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
 * Performs an "if" check on the specified condition and chooses a branch.
 *
 * @param condition The condition to check. If not zero it is considered <code>true</code>
 * @param left  The condition to use when true
 * @param right The condition to use when false
 * @author Buddy
 */
@ApiStatus.Internal
public record TernaryOperationNode(Node condition, Node left, Node right) implements Node {

    @Override
    public String toString() {
        return this.condition + " ? " + this.left + " : " + this.right;
    }

    @Override
    public boolean isConstant() {
        return this.condition.isConstant();
    }

    @Override
    public boolean hasValue() {
        return this.left.hasValue() && this.right.hasValue();
    }

    @Override
    public MolangValue evaluate(BytecodeEnvironment environment) throws MolangException {
        return this.condition.evaluate(environment).asBoolean() ? this.left.evaluate(environment) : this.right.evaluate(environment);
    }

    @Override
    public void writeBytecode(MethodNode method, BytecodeCompiler compiler, BytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        Label label_right = new Label();
        Label label_end = new Label();

        if (compiler.isOptimizationEnabled() && this.condition.isConstant()) {
            if (this.condition.evaluate(environment).asBoolean()) {
                this.left.writeBytecode(method, compiler, environment, breakLabel, continueLabel);
                if (this.left.hasValue() && !this.hasValue()) {
                    method.visitInsn(Opcodes.POP);
                }
            } else {
                this.right.writeBytecode(method, compiler, environment, breakLabel, continueLabel);
                if (this.right.hasValue() && !this.hasValue()) {
                    method.visitInsn(Opcodes.POP);
                }
            }
            return;
        }

        this.condition.writeBytecode(method, compiler, environment, breakLabel, continueLabel);
        compiler.unwrapBool(method);
        method.visitJumpInsn(Opcodes.IFEQ, label_right);

        // [left]
        {
            BytecodeEnvironment localEnvironment = environment.copy();
            this.left.writeBytecode(method, compiler, localEnvironment, breakLabel, continueLabel);
            if (this.left.hasValue() && !this.hasValue()) {
                method.visitInsn(Opcodes.POP);
            }
            localEnvironment.writeModifiedVariables(method);
        }
        method.visitJumpInsn(Opcodes.GOTO, label_end);

        //: [right]
        method.visitLabel(label_right);
        {
            BytecodeEnvironment localEnvironment = environment.copy();
            this.right.writeBytecode(method, compiler, environment, breakLabel, continueLabel);
            if (this.right.hasValue() && !this.hasValue()) {
                method.visitInsn(Opcodes.POP);
            }
            localEnvironment.writeModifiedVariables(method);
        }

        method.visitLabel(label_end);
    }
}
