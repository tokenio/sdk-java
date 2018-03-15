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

import com.google.auto.value.AutoValue;
import io.token.exceptions.InvalidTokenRequestQuery;
import io.token.proto.ProtoJson;
import io.token.proto.common.security.SecurityProtos.Signature;

import java.util.HashMap;
import java.util.Map;

@AutoValue
abstract class TokenRequestCallbackParser {
    private static final String TOKEN_ID_FIELD = "token-id";
    private static final String STATE_FIELD = "state";
    private static final String SIGNATURE_FIELD = "signature";

    static TokenRequestCallbackParser parse(String query) {
        String[] params = query.split("&");
        Map<String, String> parameters = new HashMap<>();

        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            parameters.put(name, value);
        }

        verifyParameters(parameters);

        return new AutoValue_TokenRequestCallbackParser(
                parameters.get(TOKEN_ID_FIELD),
                TokenRequestState.fromSerializedState(parameters.get(STATE_FIELD)),
                parameters.get(STATE_FIELD),
                (Signature) ProtoJson.fromJson(
                        parameters.get(SIGNATURE_FIELD),
                        Signature.newBuilder()));
    }

    private static void verifyParameters(Map<String, String> parameters) {
        if (!parameters.containsKey(TOKEN_ID_FIELD)
                || !parameters.containsKey(STATE_FIELD)
                || !parameters.containsKey(SIGNATURE_FIELD)) {
            throw new InvalidTokenRequestQuery();
        }
    }

    abstract String getTokenId();

    abstract TokenRequestState getState();

    abstract String getSerializedState();

    abstract Signature getSignature();
}
