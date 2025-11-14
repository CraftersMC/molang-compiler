package gg.moonflower.molangcompiler.api;

import gg.moonflower.molangcompiler.impl.MolangUtil;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.Collection;
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

    public static final MolangValue NULL = new MolangValue(Type.NULL, 0.0f, null, false, null);
    public static final MolangValue MATH_PI = of((float) Math.PI);
    public static final MolangValue MATH_E = of((float) Math.E);

    private final Type type;
    private final float floatValue;
    private final String stringValue;
    private final boolean boolValue;
    private final MolangValue[] arrayValue;

    private MolangValue(Type type, float floatValue, String stringValue, boolean boolValue, MolangValue[] arrayValue) {
        this.type = type;
        this.floatValue = floatValue;
        this.stringValue = stringValue;
        this.boolValue = boolValue;
        this.arrayValue = arrayValue;
    }

    public static MolangValue ofObject(Object value) {
        if (value instanceof Number f) {
            return of(f.floatValue());
        } else if (value instanceof String s) {
            return of(s);
        } else if (value instanceof MolangValue[] array) {
            return of(array);
        } else if (value instanceof Collection<?> collection) {
            MolangValue[] newArray = new MolangValue[collection.size()];
            int i = 0;
            for (Object o : collection) {
                newArray[i] = ofObject(o);
                i++;
            }
            return of(newArray);
        } else if (value instanceof Object[] array) {
            MolangValue[] newArray = new MolangValue[array.length];
            for (int i = 0; i < array.length; i++) {
                newArray[i] = ofObject(array[i]);
            }
            return of(newArray);
        } else if (value instanceof Boolean bool) {
            return of(bool);
        } else if (value instanceof MolangValue v) {
            return v;
        } else {
            return of(value.toString());
        }
    }

    /**
     * Creates a MolangValue from a float.
     *
     * @param value The float condition
     * @return A new MolangValue containing the float
     */
    public static MolangValue of(float value) {
        return new MolangValue(Type.FLOAT, value, null, false, null);
    }

    public static MolangValue of(boolean value) {
        return new MolangValue(Type.BOOLEAN, 0.0f, null, value, null);
    }

    /**
     * Creates a MolangValue from a string.
     *
     * @param value The string condition
     * @return A new MolangValue containing the string
     */
    public static MolangValue of(String value) {
        Objects.requireNonNull(value, "String condition cannot be null");
        return new MolangValue(Type.STRING, 0.0f, value, false, null);
    }

    /**
     * Creates a MolangValue from an array.
     *
     * @param value The array of MolangValues
     * @return A new MolangValue containing the array
     */
    public static MolangValue of(MolangValue[] value) {
        Objects.requireNonNull(value, "Array cannot be null");
        return new MolangValue(Type.ARRAY, 0.0f, null, false, Arrays.copyOf(value, value.length));
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
     * @return Whether this condition is an array
     */
    public boolean isArray() {
        return this.type == Type.ARRAY;
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
            case ARRAY -> this.arrayValue.length > 0 ? this.arrayValue[0].asFloat() : 0.0f;
        };
    }

    public boolean asBoolean() {
        return switch (this.type) {
            case FLOAT -> this.floatValue >= 1.0f;
            case STRING -> MolangUtil.safeStringToBool(this.stringValue);
            case BOOLEAN -> this.boolValue;
            case NULL -> false;
            case ARRAY -> this.arrayValue.length > 0;
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
            case ARRAY -> {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < this.arrayValue.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(this.arrayValue[i].asString());
                }
                sb.append("]");
                yield sb.toString();
            }
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

    /**
     * Gets the raw array without conversion.
     *
     * @return The array
     * @throws IllegalStateException If this condition is not an array
     */
    public MolangValue[] getArray() {
        if (this.type != Type.ARRAY) {
            throw new IllegalStateException("Value is not an array");
        }
        return this.arrayValue;
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
                case ARRAY -> java.util.Arrays.equals(this.arrayValue, other.arrayValue);
            };
        }
        return false;
    }

    public boolean equalsValue(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MolangValue other)) return false;

        // If types match, compare directly
        if (this.type == other.type) {
            return switch (this.type) {
                case FLOAT -> Float.compare(this.floatValue, other.floatValue) == 0;
                case STRING -> this.stringValue.equals(other.stringValue);
                case BOOLEAN -> this.boolValue == other.boolValue;
                case NULL -> true;
                case ARRAY -> java.util.Arrays.equals(this.arrayValue, other.arrayValue);
            };
        }
        // If types don't match, compare as float
        return Float.compare(this.asFloat(), other.asFloat()) == 0;
    }

    @Override
    public int hashCode() {
        return switch (this.type) {
            case FLOAT -> Float.hashCode(this.floatValue);
            case STRING -> this.stringValue.hashCode();
            case BOOLEAN -> Boolean.hashCode(this.boolValue);
            case NULL -> 0;
            case ARRAY -> java.util.Arrays.hashCode(this.arrayValue);
        };
    }

    @Override
    public String toString() {
        return switch (this.type) {
            case FLOAT -> String.valueOf(this.floatValue);
            case STRING -> "\"" + this.stringValue + "\"";
            case BOOLEAN -> Boolean.toString(this.boolValue);
            case NULL -> "null";
            case ARRAY -> {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < this.arrayValue.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(this.arrayValue[i].toString());
                }
                sb.append("]");
                yield sb.toString();
            }
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
            case ARRAY -> this;
        };
    }

    public MolangValue internalEquals(MolangValue other) {
        return MolangValue.of(this.equalsValue(other));
    }

    public MolangValue internalNotEquals(MolangValue other) {
        return MolangValue.of(!this.equalsValue(other));
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
        NULL,
        /**
         * An array of MolangValues.
         */
        ARRAY
    }
}
