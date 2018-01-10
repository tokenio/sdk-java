package io.token.browser;

import io.reactivex.Observable;
import io.reactivex.subjects.Subject;

import java.net.URL;

/**
 * Android WebView browser implementation.
 */
public class WebViewBrowser implements Browser {
    private final Subject subject;

    @Override
    public void goTo(URL url) {

    }

    @Override
    public Observable<URL> url() {
        return null;
    }

    @Override
    public void close() {

    }
}
