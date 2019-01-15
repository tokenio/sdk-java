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
import io.token.AutoValue_TokenRequestCallback;

/**
 * Represents callback in Token Request Flow. Contains tokenID and state.
 */

@AutoValue
public abstract class TokenRequestCallback {
    public static TokenRequestCallback create(String tokenId, String state) {
        return new AutoValue_TokenRequestCallback(tokenId, state);
    }

    /**
     * Get the token ID returned at the end of the Token Request Flow.
     *
     * @return token id
     */
    public abstract String getTokenId();

    /**
     * Get the state returned at the end of the Token Request Flow. This corresponds to the state
     * set at the beginning of the flow.
     *
     * @return state
     */
    public abstract String getState();
}
