package dev.canm.ab.service;

import java.io.IOException;

/**
 * Change chromosome names exception.
 */
public class ChangeChromosomeNamesException extends Exception {

    private static final long serialVersionUID = -3304943578036016580L;

    /**
     * Constructor.
     * @param message error message
     * @param e causing exception
     */
    public ChangeChromosomeNamesException(
        final String message, final IOException e) {
        super(message, e);
    }
}
