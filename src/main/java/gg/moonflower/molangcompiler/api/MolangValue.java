package gg.moonflower.molangcompiler.api;

import gg.moonflower.molangcompiler.impl.MolangUtil;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * A condition in MoLang that can be a float, string, or other types.
 * This class provides type-safe access to different condition types while maintaining
 * backward compatibility with float-only operations.
 *
 * @author irrelevantdev
 * @since 4.0.0
 */
@ApiStatus.NonExtendable
public final class MolangValue {

    public static final MolangValue NULL = new MolangValue(Type.NULL, 0.0f, null, false);
    public static final MolangValue MATH_PI = of((float) Math.PI);
    public static final MolangValue MATH_E = of((float) Math.E);

    private final Type type;
    private final float floatValue;
    private final String stringValue;
    private final boolean boolValue;

    private MolangValue(Type type, float floatValue, String stringValue, boolean boolValue) {
        this.type = type;
        this.floatValue = floatValue;
        this.stringValue = stringValue;
        this.boolValue = boolValue;
    }

    /**
     * Creates a MolangValue from a float.
     *
     * @param value The float condition
     * @return A new MolangValue containing the float
     */
    public static MolangValue of(float value) {
        return new MolangValue(Type.FLOAT, value, null, false);
    }

    public static MolangValue of(boolean value) {
        return new MolangValue(Type.BOOLEAN, 0.0f, null, value);
    }

    /**
     * Creates a MolangValue from a string.
     *
     * @param value The string condition
     * @return A new MolangValue containing the string
     */
    public static MolangValue of(String value) {
        Objects.requireNonNull(value, "String condition cannot be null");
        return new MolangValue(Type.STRING, 0.0f, value, false);
    }

    public static MolangValue ofNull() {
        return NULL;
    }

    /**
     * @return The type of this condition
     */
    public Type getType() {
        return this.type;
    }

    /**
     * @return Whether this condition is a float
     */
    public boolean isFloat() {
        return this.type == Type.FLOAT;
    }

    /**
     * @return Whether this condition is a string
     */
    public boolean isString() {
        return this.type == Type.STRING;
    }

    /**
     * Gets this condition as a float. If this is a string, returns the hash code as a float.
     *
     * @return The float representation of this condition
     */
    public float asFloat() {
        return switch (this.type) {
            case FLOAT -> this.floatValue;
            case STRING -> MolangUtil.safeStringToFloat(this.stringValue);
            case BOOLEAN -> this.boolValue ? 1.0f : 0.0f;
            case NULL -> 0.0f;
        };
    }

    public boolean asBoolean() {
        return switch (this.type) {
            case FLOAT -> this.floatValue >= 1.0f;
            case STRING -> MolangUtil.safeStringToBool(this.stringValue);
            case BOOLEAN -> this.boolValue;
            case NULL -> false;
        };
    }

    /**
     * Gets this condition as a string. If this is a float, returns the string representation.
     *
     * @return The string representation of this condition
     */
    public String asString() {
        return switch (this.type) {
            case FLOAT -> String.valueOf(this.floatValue);
            case STRING -> this.stringValue;
            case BOOLEAN -> Boolean.toString(this.boolValue);
            case NULL -> null;
        };
    }

    /**
     * Gets the raw float condition without conversion.
     *
     * @return The float condition
     * @throws IllegalStateException If this condition is not a float
     */
    public float getFloat() {
        if (this.type != Type.FLOAT) {
            throw new IllegalStateException("Value is not a float");
        }
        return this.floatValue;
    }

    /**
     * Gets the raw string condition without conversion.
     *
     * @return The string condition
     * @throws IllegalStateException If this condition is not a string
     */
    public String getString() {
        if (this.type != Type.STRING) {
            throw new IllegalStateException("Value is not a string");
        }
        return this.stringValue;
    }

    public boolean getBoolean() {
        if (this.type != Type.BOOLEAN) {
            throw new IllegalStateException("Value is not a boolean");
        }
        return this.boolValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MolangValue other)) return false;

        // If types match, compare directly
        if (this.type == other.type) {
            return switch (this.type) {
                case FLOAT -> Float.compare(this.floatValue, other.floatValue) == 0;
                case STRING -> this.stringValue.equals(other.stringValue);
                case BOOLEAN -> this.boolValue == other.boolValue;
                case NULL -> true;
            };
        }
        // If types don't match, compare as floats for backward compatibility
        return Float.compare(this.asFloat(), other.asFloat()) == 0;
    }

    @Override
    public int hashCode() {
        return switch (this.type) {
            case FLOAT -> Float.hashCode(this.floatValue);
            case STRING -> this.stringValue.hashCode();
            case BOOLEAN -> Boolean.hashCode(this.boolValue);
            case NULL -> 0;
        };
    }

    @Override
    public String toString() {
        return switch (this.type) {
            case FLOAT -> String.valueOf(this.floatValue);
            case STRING -> "\"" + this.stringValue + "\"";
            case BOOLEAN -> Boolean.toString(this.boolValue);
            case NULL -> "null";
        };
    }

    public MolangValue internalAdd(MolangValue other) {
        if (this.isString() || other.isString())
            return MolangValue.of(this.asString() + other.asString());
        return MolangValue.of(this.asFloat() + other.asFloat());
    }

    public MolangValue internalSubtract(MolangValue other) {
        if (this.isString() && other.isString()) {
            String result = this.getString().replace(other.getString(), "");
            return MolangValue.of(result);
        }
        return MolangValue.of(this.asFloat() - other.asFloat());
    }

    public MolangValue internalMultiply(MolangValue other) {
        if (this.isString() && other.isFloat()) {
            int times = Math.max(0, (int) other.getFloat());
            return MolangValue.of(this.getString().repeat(times));
        }
        return MolangValue.of(this.asFloat() * other.asFloat());
    }

    public MolangValue internalDivide(MolangValue other) {
        return MolangValue.of(this.asFloat() / other.asFloat());
    }

    public MolangValue negate() {
        return switch (type) {
            case FLOAT -> MolangValue.of(-floatValue);
            case BOOLEAN -> MolangValue.of(!boolValue);
            case STRING -> this; // ?? todo
            case NULL -> this;
        };
    }

    public MolangValue internalEquals(MolangValue other) {
        return MolangValue.of(this.equals(other));
    }

    public MolangValue internalNotEquals(MolangValue other) {
        return MolangValue.of(!this.equals(other));
    }

    public MolangValue internalLess(MolangValue other) {
        return MolangValue.of(this.asFloat() < other.asFloat());
    }

    public MolangValue internalGreater(MolangValue other) {
        return MolangValue.of(this.asFloat() > other.asFloat());
    }

    public MolangValue internalLessEquals(MolangValue other) {
        return MolangValue.of(this.asFloat() <= other.asFloat());
    }

    public MolangValue internalGreaterEquals(MolangValue other) {
        return MolangValue.of(this.asFloat() >= other.asFloat());
    }

    public MolangValue internalOr(MolangValue other) {
        return MolangValue.of(this.asBoolean() || other.asBoolean());
    }

    public MolangValue internalAnd(MolangValue other) {
        return MolangValue.of(this.asBoolean() && other.asBoolean());
    }

    public MolangValue internalNullCoalescing(MolangValue other) {
        if (this.type == Type.NULL) {
            return other;
        }
        return this;
    }

    public MolangValue internalFlip() {
        if (this.type == Type.NULL) {
            return this;
        }
        return MolangValue.of(!asBoolean());
    }


    /**
     * The type of a MolangValue.
     */
    public enum Type {
        /**
         * A floating-point number.
         */
        FLOAT,
        /**
         * A string condition.
         */
        STRING,
        BOOLEAN,
        NULL
    }
}
