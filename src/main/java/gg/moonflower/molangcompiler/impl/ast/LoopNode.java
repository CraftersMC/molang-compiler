package gg.moonflower.molangcompiler.impl.ast;

import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.impl.compiler.BytecodeCompiler;
import gg.moonflower.molangcompiler.impl.compiler.MolangBytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

/**
 * Runs the body based on the number of iterations requested.
 *
 * @param iterations The number of iterations to loop
 * @param body       The body of the loop
 * @author Buddy
 */
@ApiStatus.Internal
public record LoopNode(Node iterations, Node body) implements Node {

    @Override
    public String toString() {
        return "loop(" + this.iterations + ", {" + this.body + "})";
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public void writeBytecode(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        boolean bodyHasValue = this.body.hasValue();

        if (this.iterations.isConstant()) {
            // constant iterations
            int iterations = (int) this.iterations.evaluate(environment).asFloat();
            if (iterations < 128) {
                Label end = new Label();
                for (int i = 0; i < iterations; i++) {
                    Label next = new Label();
                    this.body.writeBytecode(method, environment, end, next);
                    if (bodyHasValue) { // Must return void
                        method.visitInsn(Opcodes.POP);
                    }
                    method.visitLabel(next);
                }
                method.visitLabel(end);
                return;
            }
        }

        Label next = new Label();
        Label end = new Label();
        Label begin = new Label();
        // iterations
        this.iterations.writeBytecodeAsFloat(method, environment, breakLabel, continueLabel);
        method.visitInsn(Opcodes.F2I);

        BytecodeCompiler.writeIntConst(method, 0); // int i = 0;
        method.visitLabel(begin);

        this.body.writeBytecode(method, environment, end, next);
        if (bodyHasValue) { // Must return void
            method.visitInsn(Opcodes.POP);
        }

        method.visitLabel(next);
        method.visitInsn(Opcodes.ICONST_1);
        method.visitInsn(Opcodes.IADD); // i++
        method.visitInsn(Opcodes.DUP2);
        method.visitJumpInsn(Opcodes.IF_ICMPGT, begin);
        method.visitLabel(end);
    }
}
