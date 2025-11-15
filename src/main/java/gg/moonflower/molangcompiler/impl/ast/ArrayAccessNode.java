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
 * Represents an array access operation in the AST, e.g., arr[0] or array[index + 1].
 *
 * @param array The node that evaluates to the array
 * @param index The node that evaluates to the index
 * @author irrelevantdev
 */
@ApiStatus.Internal
public record ArrayAccessNode(Node array, Node index) implements Node {

    /**
     * Helper method to clamp and wrap array indices.
     * Negative indices are clamped to 0.
     * Indices >= length are wrapped using modulo.
     */
    public static int clampAndWrapIndex(int idx, int length) {
        if (length == 0) {
            throw new ArrayIndexOutOfBoundsException("Cannot index empty array");
        }
        if (idx < 0) {
            return 0;
        }
        if (idx >= length) {
            return idx % length;
        }
        return idx;
    }

    @Override
    public String toString() {
        return array.toString() + "[" + index.toString() + "]";
    }

    @Override
    public boolean isConstant() {
        return array.isConstant() && index.isConstant();
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public MolangValue evaluate(BytecodeEnvironment environment) throws MolangException {
        MolangValue arrayValue = array.evaluate(environment);
        MolangValue indexValue = index.evaluate(environment);

        if (!arrayValue.isArray()) {
            throw new MolangException("Cannot index non-array type: " + arrayValue.getType());
        }

        int idx = (int) indexValue.asFloat();
        MolangValue[] elements = arrayValue.getArray();

        idx = clampAndWrapIndex(idx, elements.length);

        return elements[idx];
    }

    @Override
    public void writeBytecode(MethodNode method, BytecodeCompiler compiler, BytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        // Evaluate the array expression
        array.writeBytecode(method, compiler, environment, breakLabel, continueLabel);

        // Call getArray() on the MolangValue to get the MolangValue[] array
        method.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "gg/moonflower/molangcompiler/api/MolangValue",
                "getArray",
                "()[Lgg/moonflower/molangcompiler/api/MolangValue;",
                false
        );
        // Stack: [array]

        // Duplicate the array to get length
        method.visitInsn(Opcodes.DUP);
        // Stack: [array, array]

        method.visitInsn(Opcodes.ARRAYLENGTH);
        // Stack: [array, length]

        // Evaluate the index expression as a float and convert to int
        index.writeBytecodeAsFloat(method, compiler, environment, breakLabel, continueLabel);
        method.visitInsn(Opcodes.F2I);
        // Stack: [array, length, idx]

        // Swap to get the correct argument order for clampAndWrapIndex(idx, length)
        method.visitInsn(Opcodes.SWAP);
        // Stack: [array, idx, length]

        // Call clampAndWrapIndex(idx, length) to get the wrapped index
        method.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "gg/moonflower/molangcompiler/impl/ast/ArrayAccessNode",
                "clampAndWrapIndex",
                "(II)I",
                false
        );
        // Stack: [array, wrappedIdx]

        // Load the element from the array: array[wrappedIdx]
        method.visitInsn(Opcodes.AALOAD);
        // Stack: [MolangValue] - the element at the index
    }
}
