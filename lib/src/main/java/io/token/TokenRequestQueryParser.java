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
import io.token.exceptions.InvalidTokenRequestQuery;
import io.token.proto.ProtoJson;
import io.token.proto.common.security.SecurityProtos.Signature;

import java.util.HashMap;
import java.util.Map;

@AutoValue
public abstract class TokenRequestQueryParser {
    private static final String TOKEN_ID_FIELD = "token-id";
    private static final String STATE_FIELD = "state";
    private static final String SIGNATURE_FIELD = "signature";

    /**
     * Create a new instance of TokenRequestQueryParser.
     *
     * @param query query
     * @return instance of TokenRequestQueryParser
     */
    public static TokenRequestQueryParser parse(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<>();

        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }

        verifyParameters(map);

        return new AutoValue_TokenRequestQueryParser(
                map.get(TOKEN_ID_FIELD),
                TokenRequestState.fromSerializedState(map.get(STATE_FIELD)),
                map.get(STATE_FIELD),
                (Signature) ProtoJson.fromJson(map.get(SIGNATURE_FIELD), Signature.newBuilder()));
    }

    private static void verifyParameters(Map<String, String> map) {
        if (!map.containsKey(TOKEN_ID_FIELD)
                || !map.containsKey(STATE_FIELD)
                || !map.containsKey(SIGNATURE_FIELD)) {
            throw new InvalidTokenRequestQuery();
        }
    }

    public abstract String getTokenId();

    public abstract TokenRequestState getState();

    public abstract String getSerializedState();

    public abstract Signature getSignature();
}
