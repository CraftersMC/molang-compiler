package gg.moonflower.molangcompiler.impl.compiler;

import gg.moonflower.molangcompiler.api.MolangCompiler;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import gg.moonflower.molangcompiler.impl.ast.BinaryOperation;
import gg.moonflower.molangcompiler.impl.ast.Node;
import gg.moonflower.molangcompiler.impl.ast.UnaryOperation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
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
 * Each thread maintains its own {@link MolangBytecodeEnvironment} for tracking variables
 * and compilation state during bytecode generation.
 *
 * @author Buddy, Ocelot
 * @since 1.0.0
 */
@ApiStatus.Internal
public class BytecodeCompiler extends ClassLoader {

    /**
     * Flag to enable constant folding and other optimizations during compilation
     */
    public static final int FLAG_OPTIMIZE = 1;

    /** Local variable index for the 'this' reference in generated methods */
    public static final int THIS_INDEX = 0;
    /** Local variable index for the MolangEnvironment parameter in generated methods */
    public static final int RUNTIME_INDEX = 1;
    /** Starting index for user variables in the local variable table */
    public static final int VARIABLE_START = 2;

    private static final Pattern DASH = Pattern.compile("-");

    private final ThreadLocal<MolangBytecodeEnvironment> environment;
    private final boolean writeClasses;

    /**
     * Creates a new bytecode compiler with the specified flags and parent class loader.
     *
     * @param flags  Compilation flags (e.g., {@link #FLAG_OPTIMIZE}, {@link MolangCompiler#WRITE_CLASSES_FLAG})
     * @param parent The parent class loader for loading generated classes
     */
    public BytecodeCompiler(int flags, ClassLoader parent) {
        super(parent);
        this.environment = ThreadLocal.withInitial(() -> new MolangBytecodeEnvironment(flags));
        this.writeClasses = (flags & MolangCompiler.WRITE_CLASSES_FLAG) > 0;
    }

    /**
     * Creates a new bytecode compiler with the specified flags using the system class loader.
     *
     * @param flags Compilation flags (e.g., {@link #FLAG_OPTIMIZE}, {@link MolangCompiler#WRITE_CLASSES_FLAG})
     */
    public BytecodeCompiler(int flags) {
        this(flags, getSystemClassLoader());
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
     * If the {@link MolangCompiler#WRITE_CLASSES_FLAG} is set, the generated class file
     * will be written to disk for debugging purposes.
     *
     * @param node The AST node to compile
     * @return A compiled MolangExpression that can be evaluated with a MolangEnvironment
     * @throws MolangSyntaxException if bytecode generation fails
     */
    public MolangExpression build(Node node) throws MolangSyntaxException {
        MolangBytecodeEnvironment environment = this.environment.get();
        environment.reset();
        try {
            if (environment.optimize() && node.isConstant()) {
                return MolangExpression.of(node.evaluate(environment));
            }

            ClassNode classNode = new ClassNode(Opcodes.ASM5);
            classNode.version = Opcodes.V1_8;
            classNode.superName = "java/lang/Object";
            classNode.name = "Expression_" + DASH.matcher(UUID.randomUUID().toString()).replaceAll("");
            classNode.access = Opcodes.ACC_PUBLIC;
            classNode.interfaces.add(MolangExpression.class.getName().replaceAll("\\.", "/"));

            MethodNode init = new MethodNode();
            init.access = Opcodes.ACC_PUBLIC;
            init.name = "<init>";
            init.desc = "()V";
            init.visitVarInsn(Opcodes.ALOAD, 0);
            init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            init.visitInsn(Opcodes.RETURN);
            classNode.methods.add(init);

            MethodNode method = new MethodNode();
            method.access = Opcodes.ACC_PUBLIC;
            method.name = "get";
            method.desc = "(Lgg/moonflower/molangcompiler/api/MolangEnvironment;)Lgg/moonflower/molangcompiler/api/MolangValue;";
            method.exceptions = List.of("gg/moonflower/molangcompiler/api/exception/MolangRuntimeException");
            node.writeBytecode(method, environment, null, null);
            // Write modified variables before wrapping and returning
            environment.writeModifiedVariables(method);
            // The node has written a MolangValue to the stack
            method.visitInsn(Opcodes.ARETURN);
            classNode.methods.add(method);

            String compiledSource = node.toString();

            MethodNode equals = new MethodNode();
            Label equalsFail = new Label();
            Label equalsReturn = new Label();
            equals.access = Opcodes.ACC_PUBLIC;
            equals.name = "equals";
            equals.desc = "(Ljava/lang/Object;)Z";

            equals.visitVarInsn(Opcodes.ALOAD, 1);
            equals.visitTypeInsn(Opcodes.INSTANCEOF, "gg/moonflower/molangcompiler/api/MolangExpression");
            equals.visitJumpInsn(Opcodes.IFEQ, equalsFail); // if !(obj instanceof MolangExpression) goto equalsFail

            equals.visitLdcInsn(compiledSource);
            equals.visitVarInsn(Opcodes.ALOAD, 1);
            equals.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;", false);
            equals.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            equals.visitJumpInsn(Opcodes.IFEQ, equalsFail); // if !source.equals(obj.toString()) goto equalsFail

            BytecodeCompiler.writeIntConst(equals, 1);
            equals.visitJumpInsn(Opcodes.GOTO, equalsReturn);

            equals.visitLabel(equalsFail);
            BytecodeCompiler.writeIntConst(equals, 0);

            equals.visitLabel(equalsReturn);
            equals.visitInsn(Opcodes.IRETURN);

            classNode.methods.add(equals);

            MethodNode hashCode = new MethodNode();
            hashCode.access = Opcodes.ACC_PUBLIC;
            hashCode.name = "hashCode";
            hashCode.desc = "()I";
            BytecodeCompiler.writeIntConst(hashCode, compiledSource.hashCode());
            hashCode.visitInsn(Opcodes.IRETURN);
            classNode.methods.add(hashCode);

            MethodNode toString = new MethodNode();
            toString.access = Opcodes.ACC_PUBLIC;
            toString.name = "toString";
            toString.desc = "()Ljava/lang/String;";
            toString.visitLdcInsn(compiledSource);
            toString.visitInsn(Opcodes.ARETURN);
            classNode.methods.add(toString);


            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            /*TraceClassVisitor tcv = new TraceClassVisitor(cw, new PrintWriter(System.out));
            classNode.accept(tcv);*/
            classNode.accept(cw);
            byte[] data = cw.toByteArray();

            if (this.writeClasses) {
                Path path = Paths.get(classNode.name + ".class");
                if (!Files.exists(path)) {
                    Files.createFile(path);
                }
                Files.write(path, data);
            }

            return (MolangExpression) this.defineClass(classNode.name, data, 0, data.length).getConstructor().newInstance();
        } catch (Throwable t) {
            throw new MolangSyntaxException("Failed to convert expression '" + node + "' to bytecode", t);
        }
    }

    /**
     * Writes bytecode instructions to push a constant MolangValue onto the stack.
     * <p>
     * This method generates appropriate instructions based on the value's type (float, string, or boolean),
     * calling the corresponding {@link MolangValue#of} factory method.
     *
     * @param method The method node to write instructions to
     * @param value  The constant value to push onto the stack
     */
    public static void writeConst(MethodNode method, MolangValue value) {
        String owner = "gg/moonflower/molangcompiler/api/MolangValue";

        switch (value.getType()) {
            case FLOAT -> {
                writeFloatConst(method, value.getFloat());
                method.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        owner,
                        "of",
                        "(F)Lgg/moonflower/molangcompiler/api/MolangValue;",
                        false
                );
            }
            case STRING -> {
                method.visitLdcInsn(value.getString());
                method.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        owner,
                        "of",
                        "(Ljava/lang/String;)Lgg/moonflower/molangcompiler/api/MolangValue;",
                        false
                );
            }
            case BOOLEAN -> {
                method.visitLdcInsn(value.getBoolean());
                method.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        owner,
                        "of",
                        "(Z)Lgg/moonflower/molangcompiler/api/MolangValue;",
                        false
                );
            }
        }
    }

    /**
     * Writes optimized bytecode instructions to push a float constant onto the stack.
     * <p>
     * This method uses specialized JVM instructions for common float values (0, 1, 2)
     * to reduce bytecode size and improve performance.
     *
     * @param method The method node to write instructions to
     * @param value  The float constant to push
     */
    public static void writeFloatConst(MethodNode method, float value) {
        if (value == 0.0F) {
            method.visitInsn(Opcodes.FCONST_0);
        } else if (value == 1.0F) {
            method.visitInsn(Opcodes.FCONST_1);
        } else if (value == 2.0F) {
            method.visitInsn(Opcodes.FCONST_2);
        } else {
            method.visitLdcInsn(value);
        }
    }

    /**
     * Writes optimized bytecode instructions to push an integer constant onto the stack.
     * <p>
     * This method uses specialized JVM instructions (ICONST_N, BIPUSH, SIPUSH) for small
     * integers to minimize bytecode size and improve performance.
     *
     * @param method The method node to write instructions to
     * @param value  The integer constant to push
     */
    public static void writeIntConst(MethodNode method, int value) {
        switch (value) {
            case 0 -> method.visitInsn(Opcodes.ICONST_0);
            case 1 -> method.visitInsn(Opcodes.ICONST_1);
            case 2 -> method.visitInsn(Opcodes.ICONST_2);
            case 3 -> method.visitInsn(Opcodes.ICONST_3);
            case 4 -> method.visitInsn(Opcodes.ICONST_4);
            case 5 -> method.visitInsn(Opcodes.ICONST_5);
            default -> {
                if (value < Byte.MAX_VALUE) {
                    method.visitIntInsn(Opcodes.BIPUSH, (byte) value);
                } else if (value < Short.MAX_VALUE) {
                    method.visitIntInsn(Opcodes.SIPUSH, (short) value);
                } else {
                    method.visitLdcInsn(value);
                }
            }
        }
    }

    /**
     * Writes bytecode to negate a MolangValue expression.
     * <p>
     * This generates code that evaluates the node and calls {@link MolangValue#negate} on the result.
     *
     * @param method        The method node to write instructions to
     * @param environment   The bytecode environment for compilation context
     * @param breakLabel    Label for break statements (null if not in a loop)
     * @param continueLabel Label for continue statements (null if not in a loop)
     * @param node          The node to negate
     * @throws MolangException if bytecode generation fails
     */
    public static void writeNegate(
            MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel,
            Node node) throws MolangException {
        // push left
        node.writeBytecode(method, environment, breakLabel, continueLabel);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "gg/moonflower/molangcompiler/api/MolangValue",
                "negate",
                "(Lgg/moonflower/molangcompiler/api/MolangValue;)Lgg/moonflower/molangcompiler/api/MolangValue;",
                false
        );
    }

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
    public static void writeBinaryOperation(
            MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel,
            Node left, Node right, BinaryOperation op) throws MolangException {
        // push left
        left.writeBytecode(method, environment, breakLabel, continueLabel);
        // push right
        right.writeBytecode(method, environment, breakLabel, continueLabel);

        // call the appropriate MolangValue method
        String methodName = switch (op) {
            case ADD -> "internalAdd";
            case SUBTRACT -> "internalSubtract";
            case MULTIPLY -> "internalMultiply";
            case DIVIDE -> "internalDivide";
            case OR -> "internalOr";
            case AND -> "internalAnd";
            case LESS -> "internalLess";
            case EQUALS -> "internalEquals";
            case NOT_EQUALS -> "internalNotEquals";
            case GREATER -> "internalGreater";
            case LESS_EQUALS -> "internalLessEquals";
            case GREATER_EQUALS -> "internalGreaterEquals";
            case NULL_COALESCING -> "internalNullCoalescing";
            default -> throw new IllegalArgumentException("Unsupported operation: " + op);
        };
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "gg/moonflower/molangcompiler/api/MolangValue",
                methodName,
                "(Lgg/moonflower/molangcompiler/api/MolangValue;)Lgg/moonflower/molangcompiler/api/MolangValue;",
                false
        );
    }

    public static void writeUnaryOperation(
            MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel,
            Node node, UnaryOperation op) throws MolangException {
        // push node
        node.writeBytecode(method, environment, breakLabel, continueLabel);

        // call the appropriate MolangValue method
        String methodName = switch (op) {
            case FLIP -> "internalFlip";
            default -> throw new IllegalArgumentException("Unsupported operation: " + op);
        };
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "gg/moonflower/molangcompiler/api/MolangValue",
                methodName,
                "(Lgg/moonflower/molangcompiler/api/MolangValue;)Lgg/moonflower/molangcompiler/api/MolangValue;",
                false
        );
    }

    /**
     * Writes bytecode to convert a MolangValue on the stack to a primitive float.
     * <p>
     * Generates a call to {@link MolangValue#asFloat()}.
     *
     * @param m The method node to write instructions to
     */
    public static void unwrapFloat(MethodNode m) {
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "gg/moonflower/molangcompiler/api/MolangValue", "asFloat", "()F", false);
    }

    /**
     * Writes bytecode to convert a MolangValue on the stack to a primitive boolean.
     * <p>
     * Generates a call to {@link MolangValue#asBoolean()}.
     *
     * @param m The method node to write instructions to
     */
    public static void unwrapBool(MethodNode m) {
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "gg/moonflower/molangcompiler/api/MolangValue", "asBoolean", "()Z", false);
    }

    /**
     * Writes bytecode to convert a primitive float on the stack to a MolangValue.
     * <p>
     * Generates a call to {@link MolangValue#of(float)}.
     *
     * @param m The method node to write instructions to
     */
    public static void wrapFloat(MethodNode m) {
        m.visitMethodInsn(Opcodes.INVOKESTATIC, "gg/moonflower/molangcompiler/api/MolangValue", "of", "(F)Lgg/moonflower/molangcompiler/api/MolangValue;", false);
    }

}