package io.token.browser;

/**
 * A browser factory.
 */
public interface BrowserFactory {
    /**
     * Creates a new browser.
     *
     * @return a new browser
     */
    Browser create();
}
