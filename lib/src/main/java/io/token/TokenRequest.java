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

import com.google.auto.value.AutoValue;
import io.token.proto.common.token.TokenProtos.TokenPayload;

import java.util.HashMap;
import java.util.Map;

@AutoValue
public abstract class TokenRequest {
    public enum TokenRequestOptions {
        BANK_ID("bankId"),
        ALIAS("alias"),
        REDIRECT_URL("redirectUrl");

        private final String name;

        /**
         * Name of enum.
         *
         * @param name name
         */
        TokenRequestOptions(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static TokenRequest create(AccessTokenBuilder accessTokenBuilder) {
        return TokenRequest.create(accessTokenBuilder.build(), null);
    }

    public static TokenRequest create(TransferTokenBuilder transferTokenBuilder) {
        return TokenRequest.create(transferTokenBuilder.buildPayload(), null);
    }

    public static TokenRequest create(TokenPayload payload) {
        return TokenRequest.create(payload, null);
    }

    public static TokenRequest create(
            AccessTokenBuilder accessTokenBuilder,
            Map<String, String> options) {
        return TokenRequest.create(accessTokenBuilder.build(), options);
    }

    public static TokenRequest create(
            TransferTokenBuilder transferTokenBuilder,
            Map<String, String> options) {
        return TokenRequest.create(transferTokenBuilder.buildPayload(), options);
    }

    /**
     * Create token request.
     *
     * @param payload token payload
     * @param options options map
     * @return token request
     */
    public static TokenRequest create(TokenPayload payload, Map<String, String> options) {
        if (options == null) {
            options = new HashMap<>();
        }
        return new AutoValue_TokenRequest(payload, options);
    }

    public abstract TokenPayload getTokenPayload();

    public abstract Map<String, String> getOptions();

    public String getOption(TokenRequestOptions option) {
        return getOptions().get(option.getName());
    }

    public TokenRequest setOption(TokenRequestOptions option, String value) {
        getOptions().put(option.getName(), value);
        return this;
    }
}
