package gg.moonflower.molangcompiler.impl.ast;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * @author irrelevantdev
 */
@ApiStatus.Internal
public enum UnaryOperation {
    FLIP("!"),
    ;

    private final String value;

    UnaryOperation(String value) {
        this.value = Objects.requireNonNull(value, "condition");
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}