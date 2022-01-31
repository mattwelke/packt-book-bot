package com.mattwelke.packtbookbot;

/**
 * Used when there is an issue sharing the book data to GCP.
 */
public class CouldNotShareDataException extends Exception {
    public CouldNotShareDataException(String errorMessage) {
        super(errorMessage);
    }

    public CouldNotShareDataException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
