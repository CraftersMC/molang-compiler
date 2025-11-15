package gg.moonflower.molangcompiler.impl;

public enum CompilerFlag {
    /**
     * Whether to reduce math to constant values if possible. E.g. <code>4 * 4 + 2</code> would become <code>18</code>. This should almost always be on.
     */
    OPTIMIZE,
    /**
     * Whether to write the java bytecode to a class file. This is only for debugging.
     */
    WRITE_CLASSES,
    PRINT_CLASSES

}
