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

package io.token.tokenrequest;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import io.token.util.codec.ByteEncoding;

@AutoValue
public abstract class TokenRequestState {
    private static final Gson gson = new Gson();

    public static TokenRequestState create(String csrfTokenHash, String state) {
        return new AutoValue_TokenRequestState(csrfTokenHash, state);
    }

    /**
     * Parse a serialized state into a TokenRequestState instance.
     *
     * @param serialized serialized token request state
     * @return TokenRequestState instance
     */
    public static TokenRequestState parse(String serialized) {
        byte[] data = ByteEncoding.parse(serialized);
        String json = new String(data, UTF_8);
        return gson.fromJson(json, AutoValue_TokenRequestState.class);
    }

    public abstract String getCsrfTokenHash();

    public abstract String getInnerState();

    /**
     * Serialize into JSON format and encode.
     *
     * @return serialized state
     */
    public String serialize() {
        String json = gson.toJson(this);
        return ByteEncoding.serialize(json.getBytes(UTF_8));
    }
}
