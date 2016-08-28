package io.token;

import com.google.protobuf.StringValue;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.PaymentToken;
import io.token.proto.common.token.TokenProtos.SignedToken;
import io.token.rpc.Client;
import rx.Observable;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a funding account in the Token system.
 */
public final class Account {
    private final Member member;
    private final Client client;

    /**
     * @param member account owner
     * @param client RPC client used to perform operations against the server
     */
    Account(Member member, Client client) {
        this.member = member;
        this.client = client;
    }

    /**
     * @return account owner
     */
    public Member getMember() {
        return member;
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
                .setScheme("Pay/1.0");
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
     * Endorses the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to endorse
     * @return endorsed token
     */
    public Observable<SignedToken> endorseToken(SignedToken token) {
        return client.endorseToken(token);
    }

    /**
     * Declines the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to decline
     * @return declined token
     */
    public Observable<SignedToken> declineToken(SignedToken token) {
        return client.declineToken(token);
    }

    /**
     * Revoke the token by signing it. The signature is persisted along
     * with the token. Only applicable to endorsed tokens.
     *
     * @param token token to endorse
     * @return endorsed token
     */
    public Observable<SignedToken> revokeToken(SignedToken token) {
        return client.revokeToken(token);
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
}
