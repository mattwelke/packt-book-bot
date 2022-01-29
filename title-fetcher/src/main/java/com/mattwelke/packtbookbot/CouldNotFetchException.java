package com.mattwelke.packtbookbot;

/**
 * Catch all for any error encountered while fetching data.
 */
public class CouldNotFetchException extends Exception {
    public CouldNotFetchException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
