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

import io.token.proto.common.transferinstructions.TransferInstructionsProtos;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.AchDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.SepaDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.SwiftDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.TokenDestination;

import javax.annotation.Nullable;

public final class Destination {
    private Destination() {}

    /**
     * Creates a destination of type token (Paying to a token member account).
     *
     * @param accountId token accountId
     * @param memberId token memberId
     * @return Destination
     */
    public static TransferInstructionsProtos.Destination token(
            String accountId,
            @Nullable String memberId) {
        TokenDestination.Builder builder = TokenDestination.newBuilder()
                .setAccountId(accountId);
        if (memberId != null) {
            builder.setMemberId(memberId);
        }
        return TransferInstructionsProtos.Destination.newBuilder()
                .setTokenDestination(builder.build())
                .build();
    }

    /**
     * Creates a destination of type sepa.
     *
     * @param iban payee's iban
     * @return Destination
     */
    public static TransferInstructionsProtos.Destination sepa(String iban) {
        return TransferInstructionsProtos.Destination.newBuilder()
                .setSepaDestination(SepaDestination.newBuilder()
                        .setIban(iban)
                        .build())
                .build();
    }

    /**
     * Creates a destination of type swift.
     *
     * @param bic bank identification code
     * @param account account number
     * @return Destination
     */
    public static TransferInstructionsProtos.Destination swift(String bic, String account) {
        return TransferInstructionsProtos.Destination.newBuilder()
                .setSwiftDestination(SwiftDestination.newBuilder()
                        .setBic(bic)
                        .setAccount(account)
                        .build())
                .build();
    }

    /**
     * Creates a destination of type ACH.
     *
     * @param routing routing number
     * @param account account number
     * @return Destination
     */
    public static TransferInstructionsProtos.Destination ach(String routing, String account) {
        return TransferInstructionsProtos.Destination.newBuilder()
                .setAchDestination(AchDestination.newBuilder()
                        .setRouting(routing)
                        .setAccount(account)
                        .build())
                .build();
    }
}
