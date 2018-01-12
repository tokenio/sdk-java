package io.token.browser;

/**
 * An exception indicating that a {@link Browser} session has been closed.
 */
public class BrowserClosedException extends RuntimeException {
    /**
     * Constructs a new browser closed exception.
     */
    public BrowserClosedException() {
        super();
    }
}
