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

package io.token.user;

import com.google.auto.value.AutoValue;
import io.reactivex.annotations.Nullable;
import io.token.AutoValue_TokenRequest;
import io.token.proto.common.member.MemberProtos.Customization;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload;

import java.util.HashMap;
import java.util.Map;

@AutoValue
public abstract class TokenRequest {
    @Deprecated
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

    @Nullable public abstract TokenRequestPayload getTokenRequestPayload();

    @Nullable public abstract TokenProtos.TokenRequestOptions getTokenRequestOptions();

    @Deprecated
    @Nullable public abstract TokenPayload getTokenPayload();

    @Deprecated
    @Nullable public abstract Map<String, String> getOptions();

    @Deprecated
    @Nullable public abstract String getUserRefId();

    @Deprecated
    @Nullable public abstract String getCustomizationId();

    @Nullable public abstract Customization getCustomization();

    @Deprecated
    public String getOption(TokenRequestOptions option) {
        return getOptions().get(option.getName());
    }

    /**
     * Create a new Builder instance from a token request payload.
     *
     * @param requestPayload token request payload
     * @return Builder instance
     */
    public static Builder newBuilder(TokenRequestPayload requestPayload) {
        return new Builder(requestPayload);
    }

    /**
     * Create a new Builder instance from an access token.
     *
     * @deprecated Use newBuilder(TokenRequestPayload requestPayload) instead.
     * @param accessTokenBuilder access token builder
     * @return Builder instance
     */
    @Deprecated
    public static Builder newBuilder(AccessTokenBuilder accessTokenBuilder) {
        return new Builder(accessTokenBuilder.build());
    }

    /**
     * Create a new Builder instance from a transfer token.
     *
     * @deprecated Use newBuilder(TokenRequestPayload requestPayload) instead.
     * @param transferTokenBuilder transfer token builder
     * @return Builder instance
     */
    @Deprecated
    public static Builder newBuilder(TransferTokenBuilder transferTokenBuilder) {
        return new Builder(transferTokenBuilder.buildPayload());
    }

    /**
     * Create a new Builder instance from a token payload.
     *
     * @deprecated Use newBuilder(TokenRequestPayload requestPayload) instead.
     * @param tokenPayload token payload
     * @return Builder instance
     */
    @Deprecated
    public static Builder newBuilder(TokenPayload tokenPayload) {
        return new Builder(tokenPayload);
    }

    public static class Builder {
        private TokenRequestPayload requestPayload = null;
        private TokenProtos.TokenRequestOptions.Builder requestOptions;
        @Deprecated private TokenPayload tokenPayload;
        @Deprecated private Map<String, String> options;
        @Deprecated private String userRefId;
        @Deprecated private String customizationId;
        private Customization customization;

        Builder(TokenRequestPayload requestPayload) {
            this.requestPayload = requestPayload;
            this.requestOptions = TokenProtos.TokenRequestOptions.newBuilder();
        }

        @Deprecated
        Builder(TokenPayload tokenPayload) {
            this.tokenPayload = tokenPayload;
            this.options = new HashMap<>();
        }

        public Builder setBankId(String bankId) {
            this.requestOptions.setBankId(bankId);
            return this;
        }

        public Builder setFrom(TokenMember member) {
            this.requestOptions.setFrom(member);
            return this;
        }

        public Builder setSourceAccount(String sourceAccountId) {
            this.requestOptions.setSourceAccountId(sourceAccountId);
            return this;
        }

        public Builder setReceiptRequested(boolean receiptRequested) {
            this.requestOptions.setReceiptRequested(receiptRequested);
            return this;
        }

        public Builder setTokenRequestOptions(TokenProtos.TokenRequestOptions options) {
            this.requestOptions = options.toBuilder();
            return this;
        }

        @Deprecated
        public Builder addOption(TokenRequestOptions option, String value) {
            options.put(option.getName(), value);
            return this;
        }

        @Deprecated
        public Builder addAllOptions(Map<String, String> options) {
            this.options.putAll(options);
            return this;
        }

        @Deprecated
        public Builder setUserRefId(String userRefId) {
            this.userRefId = userRefId;
            return this;
        }

        @Deprecated
        public Builder setCustomizationId(String customizationId) {
            this.customizationId = customizationId;
            return this;
        }

        public Builder setCustomization(Customization customization) {
            this.customization = customization;
            return this;
        }

        /**
         * Builds the token payload.
         *
         * @return TokenRequest instance
         */
        public TokenRequest build() {
            // TODO(RD-1515) remove backwards compatibility with token payload
            return requestPayload == null
                    ? new AutoValue_TokenRequest(
                            null,
                    null,
                            tokenPayload,
                            options,
                            userRefId,
                            customizationId,
                            customization)
                    : new AutoValue_TokenRequest(
                            requestPayload,
                            requestOptions.build(),
                            null,
                            null,
                            null,
                            null,
                            customization);
        }
    }

    // Deprecated constructor methods

    @Deprecated
    public static TokenRequest create(AccessTokenBuilder accessTokenBuilder) {
        return TokenRequest.create(accessTokenBuilder.build(), null);
    }

    @Deprecated
    public static TokenRequest create(TransferTokenBuilder transferTokenBuilder) {
        return TokenRequest.create(transferTokenBuilder.buildPayload(), null);
    }

    @Deprecated
    public static TokenRequest create(TokenPayload payload) {
        return TokenRequest.create(payload, null);
    }

    @Deprecated
    public static TokenRequest create(
            AccessTokenBuilder accessTokenBuilder,
            Map<String, String> options) {
        return TokenRequest.create(accessTokenBuilder.build(), options);
    }

    @Deprecated
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
    @Deprecated
    public static TokenRequest create(TokenPayload payload, Map<String, String> options) {
        if (options == null) {
            options = new HashMap<>();
        }
        return new AutoValue_TokenRequest(
                null,
                null,
                payload,
                options,
                null,
                null,
                null);
    }

    @Deprecated
    public TokenRequest setOption(TokenRequestOptions option, String value) {
        getOptions().put(option.getName(), value);
        return this;
    }
}
