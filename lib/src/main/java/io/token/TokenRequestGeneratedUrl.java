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

package io.token;

import static io.token.TokenIO.TokenCluster;
import static io.token.util.Util.generateNonce;
import static io.token.util.Util.hashString;
import static java.lang.String.format;

import com.google.auto.value.AutoValue;
import io.token.exceptions.MalformedTokenRequestUrlException;

import java.net.MalformedURLException;
import java.net.URL;

@AutoValue
public abstract class TokenRequestGeneratedUrl {
    private static final String PROTOCOL = "https";
    private static final String PATH_TEMPLATE = "/authorize ?requestId=%s&state=%s";

    private enum TokenWebCluster {
        PRODUCTION("web-app.token.io"),
        INTEGRATION("web-app.int.token.io"),
        SANDBOX("web-app.sandbox.token.io"),
        STAGING("web-app.stg.token.io"),
        DEVELOPMENT("web-app.dev.token.io");

        private final String envUrl;

        TokenWebCluster(String envUrl) {
            this.envUrl = envUrl;
        }

        public String getUrl() {
            return envUrl;
        }
    }

    /**
     * Create an instance of TokenRequestGeneratedUrl.
     *
     * @param requestId request id
     * @param state state
     * @param tokenCluster token cluster
     * @return instance of TokenRequestGeneratedUrl
     * @throws MalformedTokenRequestUrlException malformed token request url exception
     */
    public static TokenRequestGeneratedUrl create(
            String requestId,
            String state,
            TokenCluster tokenCluster) {
        try {
            String nonce = generateNonce();
            String nonceHash = hashString(nonce);
            TokenRequestState tokenRequestState = TokenRequestState.create(nonceHash, state);

            return new AutoValue_TokenRequestGeneratedUrl(
                    nonce,
                    toUrl(requestId, tokenRequestState.toSerializedState(), tokenCluster));
        } catch (MalformedURLException ex) {
            throw new MalformedTokenRequestUrlException();
        }
    }

    public abstract String getNonce();

    public abstract URL getUrl();

    private static URL toUrl(String requestId, String state, TokenCluster tokenCluster)
            throws MalformedURLException {
        return new URL(
                PROTOCOL,
                TokenWebCluster.valueOf(tokenCluster.name()).getUrl(),
                format(PATH_TEMPLATE, requestId, state));
    }
}

