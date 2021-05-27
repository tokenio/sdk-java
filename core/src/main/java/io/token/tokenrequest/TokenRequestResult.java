/**
 * Copyright (c) 2021 Token, Inc.
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

import static java.util.Optional.empty;

import com.google.auto.value.AutoValue;
import io.token.proto.common.security.SecurityProtos.Signature;

import io.token.proto.common.token.TokenProtos.TokenRequestResultStatus;
import java.util.Optional;

@AutoValue
public abstract class TokenRequestResult {
    /**
     * Creates an instance of TokenRequestResult.
     *
     * @param tokenId token ID
     * @param transferId transfer ID
     * @param standingOrderSubmissionId standing order submission ID
     * @param signature signature
     * @param status status
     * @param statusReasonInformation status reason information
     * @return TokenRequestResult
     */
    public static TokenRequestResult create(
            String tokenId,
            Optional<String> transferId,
            Optional<String> standingOrderSubmissionId,
            Signature signature,
            TokenRequestResultStatus status,
            String statusReasonInformation) {
        return new AutoValue_TokenRequestResult(
                tokenId,
                transferId,
                standingOrderSubmissionId,
                signature,
                Optional.of(status),
                Optional.of(statusReasonInformation));
    }

    /**
     * Creates an instance of TokenRequestResult.
     *
     * @param tokenId token ID
     * @param transferId transfer ID
     * @param standingOrderSubmissionId standing order submission ID
     * @param signature signature
     * @return TokenRequestResult
     */
    public static TokenRequestResult create(
            String tokenId,
            Optional<String> transferId,
            Optional<String> standingOrderSubmissionId,
            Signature signature) {
        return new AutoValue_TokenRequestResult(
                tokenId,
                transferId,
                standingOrderSubmissionId,
                signature,
                empty(),
                empty());
    }

    public abstract String getTokenId();

    public abstract Optional<String> getTransferId();

    public abstract Optional<String> getStandingOrderSubmissionId();

    @Deprecated
    public abstract Signature getSignature();

    public abstract Optional<TokenRequestResultStatus> getStatus();

    public abstract Optional<String> getStatusReasonInformation();
}
