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

import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.AchDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.SepaDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.SwiftDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination.TokenDestination;

public final class Destinations {
    private Destinations() {}

    /**
     * Creates a destination of type token (Paying to a token member account).
     *
     * @param memberId token memberId
     * @param accountId token accountId
     * @return Destinations
     */
    public static Destination token(String memberId, String accountId) {
        TokenDestination.Builder builder = TokenDestination.newBuilder()
                .setMemberId(memberId)
                .setAccountId(accountId);
        return Destination.newBuilder()
                .setTokenDestination(builder.build())
                .build();
    }

    /**
     * Creates a destination of type sepa.
     *
     * @param iban payee's iban
     * @return Destinations
     */
    public static Destination sepa(String iban) {
        return Destination.newBuilder()
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
     * @return Destinations
     */
    public static Destination swift(String bic, String account) {
        return Destination.newBuilder()
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
     * @return Destinations
     */
    public static Destination ach(String routing, String account) {
        return Destination.newBuilder()
                .setAchDestination(AchDestination.newBuilder()
                        .setRouting(routing)
                        .setAccount(account)
                        .build())
                .build();
    }
}
