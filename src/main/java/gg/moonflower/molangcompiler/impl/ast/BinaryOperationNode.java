package gg.moonflower.molangcompiler.impl.ast;

import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.impl.compiler.BytecodeCompiler;
import gg.moonflower.molangcompiler.impl.compiler.MolangBytecodeEnvironment;
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

    private float evaluateFloat(MolangBytecodeEnvironment environment) throws MolangException {
        float left = this.left.evaluate(environment).asFloat();
        float right = this.right.evaluate(environment).asFloat();
        return switch (this.operator) {
            case ADD -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE -> left / right;
            case AND -> left != 0 && right != 0 ? 1.0F : 0.0F;
            case OR -> left != 0 || right != 0 ? 1.0F : 0.0F;
            case LESS -> left < right ? 1.0F : 0.0F;
            case LESS_EQUALS -> left <= right ? 1.0F : 0.0F;
            case GREATER -> left > right ? 1.0F : 0.0F;
            case GREATER_EQUALS -> left >= right ? 1.0F : 0.0F;
            case EQUALS -> left == right ? 1.0F : 0.0F;
            case NOT_EQUALS -> left != right ? 1.0F : 0.0F;
            // If the left is constant, then the value always exists and returns the first value
            case NULL_COALESCING -> left;
        };
    }

    @Override
    public MolangValue evaluate(MolangBytecodeEnvironment environment) throws MolangException {
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
            float result = (this.operator == BinaryOperation.EQUALS) ? (equal ? 1.0F : 0.0F) : (equal ? 0.0F : 1.0F);
            return MolangValue.of(result);
        }

        // For all other operations, use float arithmetic
        return MolangValue.of(this.evaluateFloat(environment));
    }

    @Override
    public void writeBytecode(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        if (environment.optimize() && this.isConstant()) {
            BytecodeCompiler.writeConst(method, this.evaluate(environment));
            return;
        }
        BytecodeCompiler.writeBinaryOperation(method, environment, breakLabel, continueLabel,
                left, right, operator);
    }

}
