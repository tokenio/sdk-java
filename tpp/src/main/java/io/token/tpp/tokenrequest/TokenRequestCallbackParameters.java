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

package io.token.tpp.tokenrequest;

import static io.token.tpp.util.Util.urlDecode;

import com.google.auto.value.AutoValue;
import io.token.proto.ProtoJson;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.tpp.exceptions.InvalidTokenRequestQuery;

import java.util.Map;
import java.util.Optional;

@AutoValue
public abstract class TokenRequestCallbackParameters {
    private static final String TOKEN_ID_FIELD = "tokenId";
    private static final String STATE_FIELD = "state";
    private static final String SIGNATURE_FIELD = "signature";
    private static final String COUNTRY_SELECTED_FIELD = "countrySelected";
    private static final String BANK_SELECTED_FIELD = "bankSelected";

    /**
     * Parses the token request callback URL's parameters. Extracts the state, the token ID, and
     * the signature over (state | token ID).
     *
     * @param parameters token request callback query parameters
     * @return TokenRequestCallbackParameters instance
     */
    public static TokenRequestCallbackParameters create(Map<String, String> parameters) {
        if (!parameters.containsKey(COUNTRY_SELECTED_FIELD)
                && (!parameters.containsKey(TOKEN_ID_FIELD)
                || !parameters.containsKey(STATE_FIELD)
                || !parameters.containsKey(SIGNATURE_FIELD))) {
            throw new InvalidTokenRequestQuery();
        }

        if (!parameters.containsKey(TOKEN_ID_FIELD)
                && (!parameters.containsKey(COUNTRY_SELECTED_FIELD)
                || !parameters.containsKey(BANK_SELECTED_FIELD)
                || !parameters.containsKey(STATE_FIELD)
                || !parameters.containsKey(SIGNATURE_FIELD))) {
            throw new InvalidTokenRequestQuery();
        }

        return new AutoValue_TokenRequestCallbackParameters(
                urlDecode(parameters.getOrDefault(TOKEN_ID_FIELD, "")),
                urlDecode(parameters.get(STATE_FIELD)),
                (Signature) ProtoJson.fromJson(
                        urlDecode(parameters.get(SIGNATURE_FIELD)),
                        Signature.newBuilder()),
                parameters.getOrDefault(COUNTRY_SELECTED_FIELD, ""),
                parameters.getOrDefault(BANK_SELECTED_FIELD, ""));
    }

    public abstract String getTokenId();

    public abstract String getSerializedState();

    public abstract Signature getSignature();

    public abstract String getCountrySelected();

    public abstract String getBankSelected();
}
