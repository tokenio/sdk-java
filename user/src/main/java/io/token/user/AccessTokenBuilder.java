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

package io.token.user;

import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.RequestBodyCase.ACCESS_BODY;
import static io.token.util.Util.generateNonce;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.token.TokenProtos.AccessBody;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource.AccountBalance;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource.AccountStandingOrders;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource.AccountTransactions;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource.Address;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource.FundsConfirmation;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource.TransferDestinations;
import io.token.proto.common.token.TokenProtos.ActingAs;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenRequest;

import javax.annotation.Nullable;

/**
 * Helps building an access token payload.
 */
public final class AccessTokenBuilder {
    private static final int REF_ID_MAX_LENGTH = 18;

    private final TokenPayload.Builder payload;

    // Token request ID
    private String tokenRequestId;

    private AccessTokenBuilder() {
        payload = TokenPayload.newBuilder()
                .setVersion("1.0")
                .setRefId(generateNonce())
                .setAccess(AccessBody.getDefaultInstance());
    }

    private AccessTokenBuilder(TokenPayload.Builder payload, @Nullable String tokenRequestId) {
        this.payload = payload;
        this.tokenRequestId = tokenRequestId;
    }

    /**
     * Creates an instance of {@link AccessTokenBuilder}.
     *
     * @param redeemerAlias redeemer alias
     * @return instance of {@link AccessTokenBuilder}
     */
    public static AccessTokenBuilder create(Alias redeemerAlias) {
        return new AccessTokenBuilder().to(redeemerAlias);
    }

    /**
     * Creates an instance of {@link AccessTokenBuilder}.
     *
     * @param redeemerMemberId redeemer member id
     * @return instance of {@link AccessTokenBuilder}
     */
    public static AccessTokenBuilder create(String redeemerMemberId) {
        return new AccessTokenBuilder().to(redeemerMemberId);
    }

    /**
     * Creates an instance of {@link AccessTokenBuilder} from an existing token payload.
     *
     * @param payload payload to initialize from
     * @return instance of {@link AccessTokenBuilder}
     */
    public static AccessTokenBuilder fromPayload(TokenPayload payload) {
        TokenPayload.Builder builder = payload.toBuilder()
                .clearAccess()
                .setRefId(generateNonce());
        return new AccessTokenBuilder(builder, null);
    }

    /**
     * Creates an instance of {@link AccessTokenBuilder} from a token request.
     *
     * @param tokenRequest token request
     * @return instance of {@link AccessTokenBuilder}
     */
    public static AccessTokenBuilder fromTokenRequest(TokenRequest tokenRequest) {
        if (tokenRequest.getRequestPayload().getRequestBodyCase() != ACCESS_BODY) {
            throw new IllegalArgumentException("Require token request with access body.");
        }
        TokenPayload.Builder builder = TokenPayload.newBuilder()
                .setVersion("1.0")
                .setRefId(tokenRequest.getRequestPayload().getRefId())
                .setFrom(tokenRequest.getRequestOptions().getFrom())
                .setTo(tokenRequest.getRequestPayload().getTo())
                .setDescription(tokenRequest.getRequestPayload().getDescription())
                .setReceiptRequested(tokenRequest.getRequestOptions().getReceiptRequested());
        if (tokenRequest.getRequestPayload().hasActingAs()) {
            builder.setActingAs(tokenRequest.getRequestPayload().getActingAs());
        }
        return new AccessTokenBuilder(builder, tokenRequest.getId());
    }

    /**
     * Sets the referenceId of the token.
     *
     * @param refId the reference Id, at most 18 characters long
     * @return builder
     */
    public AccessTokenBuilder setRefId(String refId) {
        if (refId.length() > REF_ID_MAX_LENGTH) {
            throw new IllegalArgumentException(String.format(
                    "The length of the refId is at most %s, got: %s",
                    REF_ID_MAX_LENGTH,
                    refId.length()));
        }
        payload.setRefId(refId);
        return this;
    }

    /**
     * Grants access to a given {@code addressId}.
     *
     * @param addressId address ID to grant access to
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forAddress(String addressId) {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setAddress(Address.newBuilder().setAddressId(addressId)));
        return this;
    }

    /**
     * Grants access to a given {@code accountId}.
     *
     * @param accountId account ID to grant access to
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forAccount(String accountId) {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setAccount(Resource.Account.newBuilder().setAccountId(accountId)));
        return this;
    }

    /**
     * Grants access to a given account transactions.
     *
     * @param accountId account ID to grant access to transactions
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forAccountTransactions(String accountId) {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setTransactions(AccountTransactions.newBuilder().setAccountId(accountId)));
        return this;
    }

    /**
     * Grants access to a given account standing orders.
     *
     * @param accountId account ID to grant access to standing orders
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forAccountStandingOrders(String accountId) {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setStandingOrders(AccountStandingOrders.newBuilder()
                                .setAccountId(accountId)));
        return this;
    }

    /**
     * Grants access to a given account balances.
     *
     * @param accountId account ID to grant access to balances
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forAccountBalances(String accountId) {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setBalance(AccountBalance.newBuilder().setAccountId(accountId)));
        return this;
    }

    /**
     * Grants access to all transfer destinations at the given account.
     *
     * @param accountId account id
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forTransferDestinations(String accountId) {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setTransferDestinations(TransferDestinations.newBuilder()
                                .setAccountId(accountId)));
        return this;
    }

    /**
     * Grants the ability to confirm whether the account has sufficient funds to cover
     * a given charge.
     *
     * @param accountId account ID
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forFundsConfirmation(String accountId) {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setFundsConfirmation(FundsConfirmation.newBuilder()
                                .setAccountId(accountId)
                                .build()));
        return this;
    }

    /**
     * Sets "from" field on the payload.
     *
     * @param memberId token member ID to set
     * @return {@link AccessTokenBuilder}
     */
    AccessTokenBuilder from(String memberId) {
        payload.setFrom(TokenMember.newBuilder().setId(memberId));
        return this;
    }

    /**
     * Sets "to" field on the payload.
     *
     * @param redeemerAlias redeemer alias
     * @return {@link AccessTokenBuilder}
     */
    AccessTokenBuilder to(Alias redeemerAlias) {
        payload.setTo(TokenMember.newBuilder()
                .setAlias(redeemerAlias));
        return this;
    }

    /**
     * Sets "to" field on the payload.
     *
     * @param redeemerMemberId redeemer member id
     * @return {@link AccessTokenBuilder}
     */
    AccessTokenBuilder to(String redeemerMemberId) {
        payload.setTo(TokenMember.newBuilder()
                .setId(redeemerMemberId));
        return this;
    }

    /**
     * Sets "acting as" field on the payload.
     *
     * @param actingAs entity the redeemer is acting on behalf of
     * @return {@link AccessTokenBuilder}
     */
    AccessTokenBuilder actingAs(ActingAs actingAs) {
        payload.setActingAs(actingAs);
        return this;
    }

    /**
     * Builds the {@link TokenPayload} with all specified settings.
     *
     * @return {@link AccessTokenBuilder}
     */
    TokenPayload build() {
        if (payload.getAccess().getResourcesList().isEmpty()) {
            throw new IllegalArgumentException("At least one access resource must be set");
        }

        return payload.build();
    }

    /**
     * Gets the token request ID.
     *
     * @return token request ID
     */
    @Nullable String getTokenRequestId() {
        return tokenRequestId;
    }
}
