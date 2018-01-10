package io.token.browser;

import io.reactivex.Observable;

import java.io.Closeable;
import java.net.URL;

/**
 * A browser abstraction used by the SDK
 * to interact with web content.
 */
public interface Browser extends Closeable {
    /**
     * Instructs the browser to load the given url.
     *
     * @param url the url to be loaded
     */
    void goTo(URL url);

    /**
     * Returns an url observable which will be notified
     * before a new url is loaded into the browser.
     *
     * @return an url observable
     */
    Observable<URL> url();

    @Override
    void close();
}
