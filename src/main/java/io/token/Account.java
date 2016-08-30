package io.token;

import com.google.protobuf.StringValue;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.payment.PaymentProtos.Payment;
import io.token.proto.common.payment.PaymentProtos.PaymentPayload;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.PaymentToken;
import io.token.proto.common.token.TokenProtos.SignedToken;
import io.token.proto.common.token.TokenProtos.Transfer;
import io.token.rpc.Client;
import rx.Observable;

import javax.annotation.Nullable;
import java.util.List;

import static io.token.util.Util.generateNonce;

/**
 * Represents a funding account in the Token system.
 */
public final class Account {
    private final Member member;
    private final AccountProtos.Account.Builder account;
    private final Client client;

    /**
     * @param member account owner
     * @param account account information
     * @param client RPC client used to perform operations against the server
     */
    Account(Member member, AccountProtos.Account account, Client client) {
        this.member = member;
        this.account = account.toBuilder();
        this.client = client;
    }

    /**
     * @return account owner
     */
    public Member getMember() {
        return member;
    }

    /**
     * @return account information
     */
    public AccountProtos.Account getAccount() {
        return account.build();
    }

    /**
     * Sets a new bank account.
     *
     * @param newName new name to use
     */
    public void setAccountName(String newName) {
        setAccountNameAsync(newName).toBlocking().single();
    }

    /**
     * Sets a new bank account.
     *
     * @param newName new name to use
     */
    public Observable<Void> setAccountNameAsync(String newName) {
        return client
                .setAccountName(account.getId(), newName)
                .map(a -> {
                    this.account.clear().mergeFrom(a);
                    return null;
                });
    }

    /**
     * Creates a new payment token.
     *
     * @param amount payment amount
     * @param currency currency code, e.g. "USD"
     * @return payment token returned by the server
     */
    public SignedToken createToken(double amount, String currency) {
        return createToken(amount, currency, null, null);
    }

    /**
     * Creates a new payment token.
     *
     * @param amount payment amount
     * @param currency currency code, e.g. "USD"
     * @param redeemer redeemer alias
     * @param description payment description, optional
     * @return payment token returned by the server
     */
    public SignedToken createToken(
            double amount,
            String currency,
            @Nullable String redeemer,
            @Nullable String description) {
        return createTokenAsync(amount, currency, redeemer, description)
                .toBlocking()
                .single();
    }

    /**
     * Creates a new payment token.
     *
     * @param amount payment amount
     * @param currency currency code, e.g. "USD"
     * @return payment token returned by the server
     */
    public Observable<SignedToken> createTokenAsync(double amount, String currency) {
        return createTokenAsync(amount, currency, null, null);
    }

    /**
     * Creates a new payment token.
     *
     * @param amount payment amount
     * @param currency currency code, e.g. "USD"
     * @param redeemer redeemer alias
     * @param description payment description, optional
     * @return payment token returned by the server
     */
    public Observable<SignedToken> createTokenAsync(
            double amount,
            String currency,
            @Nullable String redeemer,
            @Nullable String description) {
        PaymentToken.Builder paymentToken = Tokens
                .newToken(member, amount, currency)
                .toBuilder()
                .setScheme("Pay/1.0")
                .setTransfer(Transfer.newBuilder()
                        .setFrom(TokenProtos.Account.newBuilder()
                                .setId(account.getId())
                                .setName(account.getName())));

        if (redeemer != null) {
            paymentToken.setRedeemer(TokenProtos.Member.newBuilder()
                    .setAlias(StringValue.newBuilder().setValue(redeemer).build()));
        }
        if (description != null) {
            paymentToken.setDescription(description);
        }
        return createTokenAsync(paymentToken.build());
    }

    /**
     * Creates a new payment token.
     *
     * @param paymentToken payment token
     * @return payment token returned by the server
     */
    public Observable<SignedToken> createTokenAsync(PaymentToken paymentToken) {
        return client.createToken(paymentToken);
    }

    /**
     * Looks up a existing token.
     *
     * @param tokenId token id
     * @return payment token returned by the server
     */
    public SignedToken lookupToken(String tokenId) {
        return lookupTokenAsync(tokenId).toBlocking().single();
    }

    /**
     * Looks up a existing token.
     *
     * @param tokenId token id
     * @return payment token returned by the server
     */
    public Observable<SignedToken> lookupTokenAsync(String tokenId) {
        return client.lookupToken(tokenId);
    }

    /**
     * Looks up token owned by the member.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return payment tokens owned by the member
     */
    public List<SignedToken> lookupTokens(int offset, int limit) {
        return lookupTokensAsync(offset, limit).toBlocking().single();
    }

    /**
     * Looks up token owned by the member.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return payment tokens owned by the member
     */
    public Observable<List<SignedToken>> lookupTokensAsync(int offset, int limit) {
        return client.lookupTokens(offset, limit);
    }

    /**
     * Endorses the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to endorse
     * @return endorsed token
     */
    public SignedToken endorseToken(SignedToken token) {
        return endorseTokenAsync(token).toBlocking().single();
    }

    /**
     * Endorses the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to endorse
     * @return endorsed token
     */
    public Observable<SignedToken> endorseTokenAsync(SignedToken token) {
        return client.endorseToken(token);
    }

    /**
     * Declines the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to decline
     * @return declined token
     */
    public SignedToken declineToken(SignedToken token) {
        return declineTokenAsync(token).toBlocking().single();
    }

    /**
     * Declines the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to decline
     * @return declined token
     */
    public Observable<SignedToken> declineTokenAsync(SignedToken token) {
        return client.declineToken(token);
    }

    /**
     * Revoke the token by signing it. The signature is persisted along
     * with the token. Only applicable to endorsed tokens.
     *
     * @param token token to endorse
     * @return endorsed token
     */
    public SignedToken revokeToken(SignedToken token) {
        return revokeTokenAsync(token).toBlocking().single();
    }

    /**
     * Revoke the token by signing it. The signature is persisted along
     * with the token. Only applicable to endorsed tokens.
     *
     * @param token token to endorse
     * @return endorsed token
     */
    public Observable<SignedToken> revokeTokenAsync(SignedToken token) {
        return client.revokeToken(token);
    }

    /**
     * Redeems a payment token.
     *
     * @param token payment token to redeem
     * @return payment record
     */
    public Payment redeemToken(SignedToken token) {
        return redeemTokenAsync(token).toBlocking().single();
    }

    /**
     * Redeems a payment token.
     *
     * @param token payment token to redeem
     * @return payment record
     */
    public Observable<Payment> redeemTokenAsync(SignedToken token) {
        return redeemTokenAsync(token, null, null);
    }

    /**
     * Redeems a payment token.
     *
     * @param token payment token to redeem
     * @param amount payment amount
     * @param currency payment currency code, e.g. "EUR"
     * @return payment record
     */
    public Payment redeemToken(SignedToken token, @Nullable Double amount, @Nullable String currency) {
        return redeemTokenAsync(token, amount, currency).toBlocking().single();
    }

    /**
     * Redeems a payment token.
     *
     * @param token payment token to redeem
     * @param amount payment amount
     * @param currency payment currency code, e.g. "EUR"
     * @return payment record
     */
    public Observable<Payment> redeemTokenAsync(SignedToken token, @Nullable Double amount, @Nullable String currency) {
        PaymentPayload.Builder payload = PaymentPayload.newBuilder()
                .setNonce(generateNonce())
                .setTokenId(token.getId());

        if (amount != null) {
            payload.setAmount(amount);
        }
        if (currency != null) {
            payload.setCurrency(currency);
        }

        return client.redeemToken(token, payload.build());
    }

    /**
     * Looks up an existing payment.
     *
     * @param paymentId ID of the payment record
     * @return payment record
     */
    public Payment lookupPayment(String paymentId) {
        return lookupPaymentAsync(paymentId).toBlocking().single();
    }

    /**
     * Looks up an existing payment.
     *
     * @param paymentId ID of the payment record
     * @return payment record
     */
    public Observable<Payment> lookupPaymentAsync(String paymentId) {
        return client.lookupPayment(paymentId);
    }

    /**
     * Looks up existing payments.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @param tokenId optional token id to restrict the search
     * @return payment record
     */
    public List<Payment> lookupPayments(int offset, int limit, @Nullable String tokenId) {
        return lookupPaymentsAsync(offset, limit, tokenId).toBlocking().single();
    }

    /**
     * Looks up existing payments.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @param tokenId optional token id to restrict the search
     * @return payment record
     */
    public Observable<List<Payment>> lookupPaymentsAsync(int offset, int limit, @Nullable String tokenId) {
        return client.lookupPayments(offset, limit, tokenId);
    }
}
