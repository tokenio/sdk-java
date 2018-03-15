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
import static io.token.util.Util.hashString;
import static io.token.util.Util.verifySignature;

import io.token.exceptions.InvalidStateException;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.token.TokenProtos.RequestSignaturePayload;

import java.net.URL;

public abstract class CsrfTokenManager {
    private CsrfTokenManager() {}

    /**
     * Generate a CSRF token containing a nonce and a token request authentication url from a
     * request ID and a state string.
     *
     * @param requestId request id
     * @param state original state
     * @param tokenCluster token cluster
     * @return CSRF Token containing nonce and token request URL
     */
    public static CsrfToken generateCsrfToken(
            String requestId,
            String state,
            TokenCluster tokenCluster) {
        return CsrfToken.create(requestId, state, tokenCluster);
    }

    /**
     * Parse the token request callback URL to extract the state, the token ID and the signature of
     * (state | token ID). Verify that the state contains the nonce's hash, and that the signature
     * of the token request payload is valid. Return the extracted original state.
     *
     * @param member member
     * @param tokenRequestCallbackUrl token request callback URL
     * @param nonce nonce
     * @return original state
     */
    public static String parseTokenRequestCallbackUrl(
            Member member,
            URL tokenRequestCallbackUrl,
            String nonce) {
        final TokenRequestCallbackParser parser = TokenRequestCallbackParser
                .parse(tokenRequestCallbackUrl.getQuery());

        verifyNonceHashInState(hashString(nonce), parser.getState());
        verifySignature(
                member,
                getRequestSignaturePayload(
                        parser.getTokenId(),
                        parser.getSerializedState()),
                parser.getSignature());

        return parser.getState().getState();

    }

    private static void verifyNonceHashInState(String nonceHash, TokenRequestState state) {
        if (!state.getNonceHash().equals(nonceHash)) {
            throw new InvalidStateException(nonceHash);
        }
    }

    private static RequestSignaturePayload getRequestSignaturePayload(
            String tokenId,
            String serializedState) {
        return RequestSignaturePayload.newBuilder()
                .setTokenId(tokenId)
                .setState(serializedState)
                .build();
    }
}
