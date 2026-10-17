package com.canvaskanban.dao;

/**
 * Thrown when something goes wrong while talking to the database.
 *
 * <p>It is a {@link RuntimeException}, so the user interface can catch this one
 * exception type around a whole action and show a friendly message.</p>
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
