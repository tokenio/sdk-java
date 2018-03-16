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

package io.token.csrf;

import static io.token.TokenIO.TokenCluster;
import static io.token.util.Util.generateNonce;
import static io.token.util.Util.hashString;
import static io.token.util.Util.verifySignature;
import static java.lang.String.format;

import io.token.exceptions.InvalidStateException;
import io.token.exceptions.MalformedTokenRequestUrlException;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.token.TokenProtos.RequestSignaturePayload;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class CsrfTokenManager {
    private static final String PROTOCOL = "https";
    private static final String PATH_TEMPLATE = "/authorize ?requestId=%s&state=%s";
    private final TokenCluster tokenCluster;

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
     * Construct a new instance of CsrfTokenManager.
     *
     * @param tokenCluster token cluster
     */
    public CsrfTokenManager(TokenCluster tokenCluster) {
        this.tokenCluster = tokenCluster;
    }

    /**
     * Generate a CSRF token (a nonce).
     *
     * @return CSRF token
     */
    public String generateCsrfToken() {
        return generateNonce();
    }

    /**
     * Generate a Token request URL from a request ID, an original state, a CSRF token and a token
     * cluster.
     *
     * @param requestId request ID.
     * @param state state
     * @param csrfToken CSRF Token
     * @return token request url
     */
    public URL generateTokenRequestUrl(
            String requestId,
            String state,
            String csrfToken) {
        try {
            String csrfTokenHash = hashString(csrfToken);
            TokenRequestState tokenRequestState = TokenRequestState.create(csrfTokenHash, state);

            return toUrl(requestId, tokenRequestState.toSerializedState(), tokenCluster);
        } catch (MalformedURLException ex) {
            throw new MalformedTokenRequestUrlException();
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    /**
     * Parse the token request callback URL to extract the state, the token ID and the signature of
     * (state | token ID). Verify that the state contains the csrf token's hash, and that the
     * signature of the token request payload is valid. Return the extracted original state.
     *
     * @param tokenMember member
     * @param tokenRequestCallbackUrl token request callback URL
     * @param csrfToken csrf token
     * @return original state
     */
    public String parseTokenRequestCallbackUrl(
            Member tokenMember,
            URL tokenRequestCallbackUrl,
            String csrfToken) {
        TokenRequestCallbackParameters callbackParameters = TokenRequestCallbackParameters
                .parseUrl(tokenRequestCallbackUrl.getQuery());

        verifyNonceHashInState(hashString(csrfToken), callbackParameters.getState());
        verifySignature(
                tokenMember,
                getRequestSignaturePayload(
                        callbackParameters.getTokenId(),
                        callbackParameters.getSerializedState()),
                callbackParameters.getSignature());

        return callbackParameters.getState().getState();
    }

    private URL toUrl(String requestId, String state, TokenCluster tokenCluster)
            throws MalformedURLException, UnsupportedEncodingException {
        return new URL(
                PROTOCOL,
                TokenWebCluster.valueOf(tokenCluster.name()).getUrl(),
                format(PATH_TEMPLATE, requestId, URLEncoder.encode(state, "UTF-8")));
    }

    private void verifyNonceHashInState(String nonceHash, TokenRequestState state) {
        if (!state.getNonceHash().equals(nonceHash)) {
            throw new InvalidStateException(nonceHash);
        }
    }

    private RequestSignaturePayload getRequestSignaturePayload(
            String tokenId,
            String serializedState) {
        return RequestSignaturePayload.newBuilder()
                .setTokenId(tokenId)
                .setState(serializedState)
                .build();
    }
}
