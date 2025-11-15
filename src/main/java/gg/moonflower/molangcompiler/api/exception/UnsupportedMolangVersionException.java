package gg.moonflower.molangcompiler.api.exception;

public class UnsupportedMolangVersionException extends Exception {

    public UnsupportedMolangVersionException(String message) {
        super(message, null, true, true);
    }

    public UnsupportedMolangVersionException(String message, Throwable cause) {
        super(message, cause, true, true);
    }

    public UnsupportedMolangVersionException(Throwable cause) {
        super(null, cause, true, true);
    }
}
