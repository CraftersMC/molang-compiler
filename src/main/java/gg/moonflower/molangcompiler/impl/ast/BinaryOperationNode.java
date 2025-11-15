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
 * @param left     The first operand
 * @param right    The second operand
 * @author Buddy
 */
@ApiStatus.Internal
public record BinaryOperationNode(BinaryOperation operator, Node left, Node right) implements Node {

    @Override
    public String toString() {
        return "(" + this.left + " " + this.operator + " " + this.right + ")";
    }

    @Override
    public boolean isConstant() {
        return this.left.isConstant() && (this.operator == BinaryOperation.NULL_COALESCING || this.right.isConstant());
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    private float evaluateFloat(BytecodeEnvironment environment) throws MolangException {
        MolangValue leftValue = this.left.evaluate(environment);
        MolangValue rightValue = this.right.evaluate(environment);

        // For string comparisons, handle EQUALS and NOT_EQUALS specially
        if ((this.operator == BinaryOperation.EQUALS || this.operator == BinaryOperation.NOT_EQUALS) &&
                (leftValue.isString() || rightValue.isString())) {
            boolean equal;
            if (leftValue.isString() && rightValue.isString()) {
                equal = leftValue.getString().equals(rightValue.getString());
            } else {
                // If types don't match, they're not equal
                equal = false;
            }
            return (this.operator == BinaryOperation.EQUALS) ? (equal ? 1.0f : 0.0f) : (equal ? 0.0f : 1.0f);
        }

        float left = leftValue.asFloat();
        float right = rightValue.asFloat();
        return switch (this.operator) {
            case ADD -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE -> left / right;
            case AND -> left != 0 && right != 0 ? 1.0f : 0.0f;
            case OR -> left != 0 || right != 0 ? 1.0f : 0.0f;
            case LESS -> left < right ? 1.0f : 0.0f;
            case LESS_EQUALS -> left <= right ? 1.0f : 0.0f;
            case GREATER -> left > right ? 1.0f : 0.0f;
            case GREATER_EQUALS -> left >= right ? 1.0f : 0.0f;
            case EQUALS -> left == right ? 1.0f : 0.0f;
            case NOT_EQUALS -> left != right ? 1.0f : 0.0f;
            // If the left is constant, then the value always exists and returns the first value
            case NULL_COALESCING -> left;
        };
    }

    @Override
    public MolangValue evaluate(BytecodeEnvironment environment) throws MolangException {
        return MolangValue.of(this.evaluateFloat(environment));
    }

    @Override
    public void writeBytecode(MethodNode method, BytecodeCompiler compiler, BytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        if (compiler.isOptimizationEnabled() && this.isConstant()) {
            compiler.writeConst(method, this.evaluate(environment));
            return;
        }
        compiler.writeBinaryOperation(method, environment, breakLabel, continueLabel,
                left, right, operator);
    }

    @Override
    public void writeBytecodeAsFloat(MethodNode method, BytecodeCompiler compiler, BytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        if (compiler.isOptimizationEnabled() && this.isConstant()) {
            compiler.writeFloatConst(method, this.evaluateFloat(environment));
            return;
        }
        compiler.writeBinaryOperationAsFloat(method, environment, breakLabel, continueLabel,
                left, right, operator);
    }
}
