/**
 * Copyright (c) 2019 Token, Inc.
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

package io.token.user.util;

import io.token.TokenClient.TokenCluster;

import javax.annotation.Nullable;


/**
 * Utility methods.
 */
public class Util extends io.token.util.Util {
    /**
     * Retrieve the access token from the URL fragment, given the full URL.
     *
     * @param fullUrl full url
     * @return oauth access token, or null if not found
     */
    public static @Nullable String parseOauthAccessToken(String fullUrl) {
        String[] urlParts = fullUrl.split("#|&");
        for (int i = urlParts.length - 1; i >= 0; i--) {
            if (urlParts[i].contains("access_token=")) {
                return urlParts[i].substring(13);
            }
        }
        return null;
    }

    /**
     * Get the cluster-dependent url to the web-app.
     *
     * @param cluster Token cluster
     * @return web-app url
     */
    public static String getWebAppUrl(TokenCluster cluster) {
        switch (cluster) {
            case PRODUCTION:
                return "web-app.token.io";
            case INTEGRATION:
                return "web-app.int.token.io";
            case SANDBOX:
                return "web-app.sandbox.token.io";
            case STAGING:
                return "web-app.stg.token.io";
            case PERFORMANCE:
                return "web-app.perf.token.io";
            case DEVELOPMENT:
                return "web-app.dev.token.io";
            default:
                throw new IllegalArgumentException("Unrecognized cluster: " + cluster);
        }
    }
}
