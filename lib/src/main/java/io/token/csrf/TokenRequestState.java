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
import com.google.gson.Gson;

import java.io.Serializable;

@AutoValue
abstract class TokenRequestState implements Serializable {
    static TokenRequestState create(String nonceHash, String state) {
        return new AutoValue_TokenRequestState(nonceHash, state);
    }

    abstract String getNonceHash();

    abstract String getState();

    String toSerializedState() {
        Gson gson  = new Gson();
        return gson.toJson(this);
    }

    static TokenRequestState fromSerializedState(String state) {
        Gson gson = new Gson();
        return gson.fromJson(state, TokenRequestState.class);
    }
}

