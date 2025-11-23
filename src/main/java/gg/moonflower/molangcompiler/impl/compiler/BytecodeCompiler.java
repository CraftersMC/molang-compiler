package gg.moonflower.molangcompiler.impl.compiler;

import gg.moonflower.molangcompiler.api.CompilerFlag;
import gg.moonflower.molangcompiler.api.CompilerFlags;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import gg.moonflower.molangcompiler.impl.ast.BinaryOperation;
import gg.moonflower.molangcompiler.impl.ast.Node;
import gg.moonflower.molangcompiler.impl.ast.UnaryOperation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.MethodNode;

import java.util.regex.Pattern;

/**
 * Compiles MoLang AST nodes into Java bytecode for high-performance expression evaluation.
 * <p>
 * This compiler transforms abstract syntax trees (ASTs) into executable Java classes that implement
 * the {@link MolangExpression} interface. By generating bytecode directly, the compiler achieves
 * significantly better runtime performance compared to interpreted evaluation.
 * <p>
 * The bytecode generation process involves:
 * <ol>
 *   <li>Creating a new class that implements MolangExpression</li>
 *   <li>Generating a {@code get(MolangEnvironment)} method that evaluates the expression</li>
 *   <li>Visiting each node in the AST and emitting appropriate JVM instructions</li>
 *   <li>Loading the generated class and instantiating it</li>
 * </ol>
 * <p>
 * The compiler supports optimization through constant folding and can optionally write
 * generated class files to disk for debugging purposes.
 * <p>
 * Each thread maintains its own {@link BytecodeEnvironment} for tracking variables
 * and compilation state during bytecode generation.
 *
 * @author Buddy, Ocelot
 * @since 1.0.0
 */
@ApiStatus.Internal
public abstract class BytecodeCompiler extends ClassLoader {

    protected static final Pattern DASH = Pattern.compile("-");

    protected final ThreadLocal<BytecodeEnvironment> environment;
    protected final CompilerFlags flags;
    protected final boolean enableOptimization;
    protected final boolean writeClasses;
    protected final boolean printClasses;

    /**
     * Creates a new bytecode compiler with the specified flags and parent class loader.
     *
     * @param flags  Compilation flags (e.g., {@link CompilerFlag#OPTIMIZE}, {@link CompilerFlag#WRITE_CLASSES})
     * @param parent The parent class loader for loading generated classes
     */
    protected BytecodeCompiler(CompilerFlags flags, ClassLoader parent, ThreadLocal<BytecodeEnvironment> environment) {
        super(parent);
        this.flags = flags;
        this.enableOptimization = flags.contains(CompilerFlag.OPTIMIZE);
        this.environment = environment;
        this.writeClasses = flags.contains(CompilerFlag.WRITE_CLASSES);
        this.printClasses = flags.contains(CompilerFlag.PRINT_CLASSES);
    }

    /**
     * Compiles an AST node into an executable MolangExpression.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>If optimization is enabled and the node is constant, returns a constant expression</li>
     *   <li>Creates a new class implementing MolangExpression with a unique name</li>
     *   <li>Generates bytecode for the expression evaluation method</li>
     *   <li>Generates equals(), hashCode(), and toString() methods</li>
     *   <li>Loads and instantiates the generated class</li>
     * </ol>
     * <p>
     * If the {@link CompilerFlag#WRITE_CLASSES} is set, the generated class file
     * will be written to disk for debugging purposes.
     *
     * @param node The AST node to compile
     * @return A compiled MolangExpression that can be evaluated with a MolangEnvironment
     * @throws MolangSyntaxException if bytecode generation fails
     */
    public abstract MolangExpression build(Node node) throws MolangSyntaxException;

    /**
     * Writes bytecode instructions to push a constant MolangValue onto the stack.
     * <p>
     * This method generates appropriate instructions based on the value's type (float, string, or boolean),
     * calling the corresponding {@link MolangValue#of} factory method.
     *
     * @param method The method node to write instructions to
     * @param value  The constant value to push onto the stack
     */
    public abstract void writeConst(MethodNode method, MolangValue value);

    /**
     * Writes optimized bytecode instructions to push a float constant onto the stack.
     * <p>
     * This method uses specialized JVM instructions for common float values (0, 1, 2)
     * to reduce bytecode size and improve performance.
     *
     * @param method The method node to write instructions to
     * @param value  The float constant to push
     */
    public abstract void writeFloatConst(MethodNode method, float value);

    /**
     * Writes optimized bytecode instructions to push an integer constant onto the stack.
     * <p>
     * This method uses specialized JVM instructions (ICONST_N, BIPUSH, SIPUSH) for small
     * integers to minimize bytecode size and improve performance.
     *
     * @param method The method node to write instructions to
     * @param value  The integer constant to push
     */
    public abstract void writeIntConst(MethodNode method, int value);

    /**
     * Writes bytecode to perform a binary operation between two MolangValue expressions.
     * <p>
     * This method generates code that:
     * <ol>
     *   <li>Evaluates the left operand and pushes it onto the stack</li>
     *   <li>Evaluates the right operand and pushes it onto the stack</li>
     *   <li>Calls the appropriate MolangValue operation method (e.g., internalAdd, internalMultiply)</li>
     * </ol>
     *
     * @param method        The method node to write instructions to
     * @param environment   The bytecode environment for compilation context
     * @param breakLabel    Label for break statements (null if not in a loop)
     * @param continueLabel Label for continue statements (null if not in a loop)
     * @param left          The left operand node
     * @param right         The right operand node
     * @param op            The binary operation to perform
     * @throws MolangException if bytecode generation fails or the operation is unsupported
     */
    public abstract void writeBinaryOperation(
            MethodNode method, BytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel,
            Node left, Node right, BinaryOperation op) throws MolangException;

    public abstract void writeUnaryOperation(
            MethodNode method, BytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel,
            Node node, UnaryOperation op) throws MolangException;

    public abstract void writeBinaryOperationAsFloat(
            MethodNode method, BytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel,
            Node left, Node right, BinaryOperation op) throws MolangException;

    /**
     * Writes bytecode to convert a MolangValue on the stack to a primitive float.
     * <p>
     * Generates a call to {@link MolangValue#asFloat()}.
     *
     * @param m The method node to write instructions to
     */
    public abstract void unwrapFloat(MethodNode m);

    /**
     * Writes bytecode to convert a MolangValue on the stack to a primitive boolean.
     * <p>
     * Generates a call to {@link MolangValue#asBoolean()}.
     *
     * @param m The method node to write instructions to
     */
    public abstract void unwrapBool(MethodNode m);

    /**
     * Writes bytecode to convert a primitive float on the stack to a MolangValue.
     * <p>
     * Generates a call to {@link MolangValue#of(float)}.
     *
     * @param m The method node to write instructions to
     */
    public abstract void wrapFloat(MethodNode m);

    public boolean isOptimizationEnabled() {
        return enableOptimization;
    }
}