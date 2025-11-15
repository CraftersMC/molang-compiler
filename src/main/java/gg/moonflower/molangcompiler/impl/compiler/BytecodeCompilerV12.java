package gg.moonflower.molangcompiler.impl.compiler;

import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangValue;
import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import gg.moonflower.molangcompiler.impl.CompilerFlag;
import gg.moonflower.molangcompiler.impl.CompilerFlags;
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
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Version 12 implementation of the bytecode compiler.
 * <p>
 * This compiler implements the default code generation behavior for MoLang v12.
 * </p>
 *
 * @author irrelevantdev
 * @since 4.0.0
 */
@ApiStatus.Internal
public class BytecodeCompilerV12 extends BytecodeCompiler {

    public BytecodeCompilerV12(CompilerFlags flags, ClassLoader parent) {
        super(flags, parent,
                ThreadLocal.withInitial(() -> new BytecodeEnvironmentV12(0, 1, 2))
        );
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
     * If the {@link CompilerFlag#OPTIMIZE} is set, the generated class file
     * will be written to disk for debugging purposes.
     *
     * @param node The AST node to compile
     * @return A compiled MolangExpression that can be evaluated with a MolangEnvironment
     * @throws MolangSyntaxException if bytecode generation fails
     */
    public MolangExpression build(Node node) throws MolangSyntaxException {
        BytecodeEnvironment environment = this.environment.get();
        environment.reset();
        try {
            if (isOptimizationEnabled() && node.isConstant()) {
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
            node.writeBytecode(method, this, environment, null, null);
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

            writeIntConst(equals, 1);
            equals.visitJumpInsn(Opcodes.GOTO, equalsReturn);

            equals.visitLabel(equalsFail);
            writeIntConst(equals, 0);

            equals.visitLabel(equalsReturn);
            equals.visitInsn(Opcodes.IRETURN);

            classNode.methods.add(equals);

            MethodNode hashCode = new MethodNode();
            hashCode.access = Opcodes.ACC_PUBLIC;
            hashCode.name = "hashCode";
            hashCode.desc = "()I";
            writeIntConst(hashCode, compiledSource.hashCode());
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
            if (printClasses) {
                TraceClassVisitor tcv = new TraceClassVisitor(cw, new PrintWriter(System.out));
                classNode.accept(tcv);
            } else {
                classNode.accept(cw);
            }
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
    public void writeConst(MethodNode method, MolangValue value) {
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
    public void writeFloatConst(MethodNode method, float value) {
        if (value == 0.0f) {
            method.visitInsn(Opcodes.FCONST_0);
        } else if (value == 1.0f) {
            method.visitInsn(Opcodes.FCONST_1);
        } else if (value == 2.0f) {
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
    public void writeIntConst(MethodNode method, int value) {
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
    public void writeBinaryOperation(
            MethodNode method, BytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel,
            Node left, Node right, BinaryOperation op) throws MolangException {
        // push left
        left.writeBytecode(method, this, environment, breakLabel, continueLabel);
        // push right
        right.writeBytecode(method, this, environment, breakLabel, continueLabel);

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

    public void writeUnaryOperation(
            MethodNode method, BytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel,
            Node node, UnaryOperation op) throws MolangException {
        // push node
        node.writeBytecode(method, this, environment, breakLabel, continueLabel);

        // call the appropriate MolangValue method
        String methodName = switch (op) {
            case FLIP -> "internalFlip";
            default -> throw new IllegalArgumentException("Unsupported operation: " + op);
        };
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "gg/moonflower/molangcompiler/api/MolangValue",
                methodName,
                "()Lgg/moonflower/molangcompiler/api/MolangValue;",
                false
        );
    }

    public void writeBinaryOperationAsFloat(
            MethodNode method, BytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel,
            Node left, Node right, BinaryOperation op) throws MolangException {
        writeBinaryOperation(method, environment, breakLabel, continueLabel, left, right, op);
        // Unwrap the result back to a float
        unwrapFloat(method);
    }

    /**
     * Writes bytecode to convert a MolangValue on the stack to a primitive float.
     * <p>
     * Generates a call to {@link MolangValue#asFloat()}.
     *
     * @param m The method node to write instructions to
     */
    public void unwrapFloat(MethodNode m) {
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "gg/moonflower/molangcompiler/api/MolangValue", "asFloat", "()F", false);
    }

    /**
     * Writes bytecode to convert a MolangValue on the stack to a primitive boolean.
     * <p>
     * Generates a call to {@link MolangValue#asBoolean()}.
     *
     * @param m The method node to write instructions to
     */
    public void unwrapBool(MethodNode m) {
        m.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "gg/moonflower/molangcompiler/api/MolangValue", "asBoolean", "()Z", false);
    }

    /**
     * Writes bytecode to convert a primitive float on the stack to a MolangValue.
     * <p>
     * Generates a call to {@link MolangValue#of(float)}.
     *
     * @param m The method node to write instructions to
     */
    public void wrapFloat(MethodNode m) {
        m.visitMethodInsn(Opcodes.INVOKESTATIC, "gg/moonflower/molangcompiler/api/MolangValue", "of", "(F)Lgg/moonflower/molangcompiler/api/MolangValue;", false);
    }

}
