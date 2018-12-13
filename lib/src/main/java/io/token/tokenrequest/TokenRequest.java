/**
 * Copyright (c) 2018 Token, Inc.
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

package io.token.tokenrequest;

import com.google.auto.value.AutoValue;
import io.reactivex.annotations.Nullable;
import io.token.proto.common.member.MemberProtos.Customization;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload;

@AutoValue
public abstract class TokenRequest {
    @Nullable public abstract TokenRequestPayload getTokenRequestPayload();

    @Nullable public abstract TokenProtos.TokenRequestOptions getTokenRequestOptions();

    @Nullable public abstract Customization getCustomization();

    /**
     * Create a new Builder instance from a token request payload.
     *
     * @param requestPayload token request payload
     * @return Builder instance
     */
    public static Builder newBuilder(TokenRequestPayload requestPayload) {
        return new Builder(requestPayload);
    }

    public static class Builder {
        private TokenRequestPayload requestPayload;
        private TokenProtos.TokenRequestOptions.Builder requestOptions;
        private Customization customization;

        Builder(TokenRequestPayload requestPayload) {
            this.requestPayload = requestPayload;
            this.requestOptions = TokenProtos.TokenRequestOptions.newBuilder();
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
            return new AutoValue_TokenRequest(
                            requestPayload,
                            requestOptions.build(),
                            customization);
        }
    }
}
