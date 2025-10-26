package gg.moonflower.molangcompiler.impl.ast;

import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.impl.MolangUtil;
import gg.moonflower.molangcompiler.impl.compiler.BytecodeCompiler;
import gg.moonflower.molangcompiler.impl.compiler.MolangBytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.MethodNode;

/**
 * Pushes a single constant number onto the stack.
 *
 * @param value The value to push onto the stack
 * @author Buddy, Ocelot
 */
@ApiStatus.Internal
public record ConstNode(MolangValue value) implements Node {

    public static final ConstNode ZERO_FLOAT_NODE = new ConstNode(MolangValue.of(0.0f));
    public static final ConstNode ONE_FLOAT_NODE = new ConstNode(MolangValue.of(0.0f));

    @Override
    public String toString() {
        return MolangUtil.toString(this.value);
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public MolangValue evaluate(MolangBytecodeEnvironment environment) throws MolangException {
        return this.value;
    }

    @Override
    public void writeBytecode(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        BytecodeCompiler.writeConst(method, this.value);
    }

    @Override
    public void writeBytecodeAsFloat(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        BytecodeCompiler.writeFloatConst(method, this.value.asFloat());
    }

    @Override
    public void writeBytecodeAsTruncatedFloat(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        BytecodeCompiler.writeFloatConst(method, (int) this.value.asFloat());
    }

    @Override
    public void writeBytecodeAsRoundedFloat(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        BytecodeCompiler.writeFloatConst(method, Math.round(this.value.asFloat()));
    }
}