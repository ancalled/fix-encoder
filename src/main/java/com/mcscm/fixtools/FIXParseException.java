package com.mcscm.fixtools;

public class FIXParseException extends RuntimeException {

    public FIXParseException() {
    }

    public FIXParseException(String message) {
        super(message);
    }

    public FIXParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public FIXParseException(Throwable cause) {
        super(cause);
    }

    public FIXParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
