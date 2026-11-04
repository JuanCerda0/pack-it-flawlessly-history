package dev.noby.history.exception;

public class InvalidHistoryMessageException extends RuntimeException {
    public InvalidHistoryMessageException(String message) {
        super(message);
    }

    public InvalidHistoryMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
