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
 * A browser abstraction used by the SDK to interact with web content.
 * The user may call goTo(url) to display the url in the browser, or
 * fetchData(url) to fetch data from the URL without displaying the page.
 *
 * <p>Pages will only be displayed in the browser as a result of a call to goTo(url).
 * Hyperlinks and redirects will cause the url() Observable to be notified
 * but will not load the page unless goTo(url) is explicitly called by an Observer.
 */
public interface Browser extends Closeable {
    /**
     * Instructs the browser to load the given url.
     *
     * @param url the url to be loaded
     */
    void goTo(URL url);

    /**
     * Fetch data from URL. Does not load the page.
     *
     * @param url the url to fetch data from
     * @return data observable
     */
    Observable<String> fetchData(URL url);

    /**
     * Returns a url Observable which will notify the user of hyperlinks and redirects.
     * The new page will not be loaded unless the user calls goTo on that URL.
     *
     * @return a url observable
     */
    Observable<URL> url();

    @Override
    void close();
}
