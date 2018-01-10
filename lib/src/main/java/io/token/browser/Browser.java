/**
 * Copyright (c) 2017 Token, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
     * @throws BrowserClosedException if the browser was closed by the user
     */
    void goTo(URL url);

    /**
     * Returns an url observable which will be notified
     * before a new url is loaded into the browser.
     * In case the browser was closed by the user the
     * observable should be notified with a {@link BrowserClosedException}.
     *
     * @return an url observable
     */
    Observable<URL> url();

    @Override
    void close();
}
