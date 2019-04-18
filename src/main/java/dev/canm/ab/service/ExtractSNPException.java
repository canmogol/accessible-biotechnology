package dev.canm.ab.service;

/**
 * SNP Extract Exception.
 */
public class ExtractSNPException extends Exception {

    private static final long serialVersionUID = 7374461688091733889L;

    /**
     * Constructor with message and cause.
     *
     * @param message error message
     * @param cause   causing exception
     */
    public ExtractSNPException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
