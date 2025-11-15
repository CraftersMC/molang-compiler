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

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Represents an array literal node in the AST, e.g., [1, 2, 3] or ["hello", "world"].
 *
 * @param elements The elements of the array
 * @author irrelevantdev
 */
@ApiStatus.Internal
public record ArrayLiteralNode(Node[] elements) implements Node {

    @Override
    public String toString() {
        return "[" + Arrays.stream(elements)
                .map(Node::toString)
                .collect(Collectors.joining(", ")) + "]";
    }

    @Override
    public boolean isConstant() {
        for (Node element : elements) {
            if (!element.isConstant()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public MolangValue evaluate(BytecodeEnvironment environment) throws MolangException {
        MolangValue[] values = new MolangValue[elements.length];
        for (int i = 0; i < elements.length; i++) {
            values[i] = elements[i].evaluate(environment);
        }
        return MolangValue.of(values);
    }

    @Override
    public void writeBytecode(MethodNode method, BytecodeCompiler compiler, BytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        // Push array size onto stack
        compiler.writeIntConst(method, elements.length);

        // Create new MolangValue array
        method.visitTypeInsn(Opcodes.ANEWARRAY, "gg/moonflower/molangcompiler/api/MolangValue");

        // Populate array elements
        for (int i = 0; i < elements.length; i++) {
            // Duplicate array reference for storing
            method.visitInsn(Opcodes.DUP);

            // Push index
            compiler.writeIntConst(method, i);

            // Evaluate element
            elements[i].writeBytecode(method, compiler, environment, breakLabel, continueLabel);

            // Store in array: array[i] = value
            method.visitInsn(Opcodes.AASTORE);
        }

        // Now we have MolangValue[] on the stack, wrap it in MolangValue.of(array)
        method.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "gg/moonflower/molangcompiler/api/MolangValue",
                "of",
                "([Lgg/moonflower/molangcompiler/api/MolangValue;)Lgg/moonflower/molangcompiler/api/MolangValue;",
                false
        );
    }
}
