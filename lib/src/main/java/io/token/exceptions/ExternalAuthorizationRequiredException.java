/**
 * Copyright (c) 2017 Token, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.token.exceptions;

import io.token.TransferTokenException;
import io.token.proto.common.token.TokenProtos.TransferTokenStatus;

/**
 * Thrown when external authorization is required to complete the transaction.
 */
public class ExternalAuthorizationRequiredException extends TransferTokenException {
    private final String authorizationUrl;

    /**
     * Creates an instance of ExternalAuthorizationRequiredException.
     *
     * @param authorizationUrl external authorization url
     */
    public ExternalAuthorizationRequiredException(String authorizationUrl) {
        super(TransferTokenStatus.FAILURE_EXTERNAL_AUTHORIZATION_REQUIRED);
        this.authorizationUrl = authorizationUrl;
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }
}
