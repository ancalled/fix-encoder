package com.mcscm.fixtools;

public class CoderException extends RuntimeException {

    public CoderException() {
    }

    public CoderException(String message) {
        super(message);
    }

    public CoderException(String message, Throwable cause) {
        super(message, cause);
    }

    public CoderException(Throwable cause) {
        super(cause);
    }

    public CoderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
