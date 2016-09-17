package io.token;

import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.payment.PaymentProtos.Payment;
import io.token.proto.common.payment.PaymentProtos.PaymentPayload;
import io.token.proto.common.token.TokenProtos.PaymentToken;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.rpc.Client;
import rx.Observable;

import javax.annotation.Nullable;
import java.util.List;

import static io.token.util.Util.generateNonce;

/**
 * Represents a funding account in the Token system.
 */
public final class AccountAsync {
    private final MemberAsync member;
    private final AccountProtos.Account.Builder account;
    private final Client client;

    /**
     * @param member account owner
     * @param account account information
     * @param client RPC client used to perform operations against the server
     */
    AccountAsync(MemberAsync member, AccountProtos.Account account, Client client) {
        this.member = member;
        this.account = account.toBuilder();
        this.client = client;
    }

    /**
     * @return synchronous version of the account API
     */
    public Account sync() {
        return new Account(this);
    }

    /**
     * @return account owner
     */
    public MemberAsync getMember() {
        return member;
    }

    /**
     * @return account id
     */
    public String getId() {
        return account.getId();
    }

    /**
     * @return account name
     */
    public String getName() {
        return account.getName();
    }

    /**
     * Sets a new bank account.
     *
     * @param newName new name to use
     */
    public Observable<Void> setAccountName(String newName) {
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
    public Observable<Token> createToken(double amount, String currency) {
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
    public Observable<Token> createToken(
            double amount,
            String currency,
            @Nullable String redeemer,
            @Nullable String description) {
        PaymentToken.Builder paymentToken = PaymentToken.newBuilder()
                .setScheme("Pay/1.0")
                .setNonce(generateNonce())
                .setPayer(TokenMember.newBuilder()
                        .setId(member.getMemberId()))
                .setCurrency(currency)
                .setAmount(amount)
                .setTransfer(Transfer.newBuilder()
                        .setFrom(TransferProtos.Source.newBuilder()
                                .setAccountId(account.getId())));

        if (redeemer != null) {
            paymentToken.setRedeemer(TokenMember.newBuilder()
                    .setAlias(redeemer));
        }
        if (description != null) {
            paymentToken.setDescription(description);
        }
        return createToken(paymentToken.build());
    }

    /**
     * Creates a new payment token.
     *
     * @param paymentToken payment token
     * @return payment token returned by the server
     */
    public Observable<Token> createToken(PaymentToken paymentToken) {
        return client.createToken(paymentToken);
    }

    /**
     * Looks up a existing token.
     *
     * @param tokenId token id
     * @return payment token returned by the server
     */
    public Observable<Token> lookupToken(String tokenId) {
        return client.lookupToken(tokenId);
    }

    /**
     * Looks up token owned by the member.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return payment tokens owned by the member
     */
    public Observable<List<Token>> lookupTokens(int offset, int limit) {
        return client.lookupTokens(offset, limit);
    }

    /**
     * Endorses the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to endorse
     * @return endorsed token
     */
    public Observable<Token> endorseToken(Token token) {
        return client.endorseToken(token);
    }

    /**
     * Declines the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to decline
     * @return declined token
     */
    public Observable<Token> declineToken(Token token) {
        return client.declineToken(token);
    }

    /**
     * Revoke the token by signing it. The signature is persisted along
     * with the token. Only applicable to endorsed tokens.
     *
     * @param token token to endorse
     * @return endorsed token
     */
    public Observable<Token> revokeToken(Token token) {
        return client.revokeToken(token);
    }

    /**
     * Redeems a payment token.
     *
     * @param token payment token to redeem
     * @return payment record
     */
    public Observable<Payment> redeemToken(Token token) {
        return redeemToken(token, null, null);
    }

    /**
     * Redeems a payment token.
     *
     * @param token payment token to redeem
     * @param amount payment amount
     * @param currency payment currency code, e.g. "EUR"
     * @return payment record
     */
    public Observable<Payment> redeemToken(Token token, @Nullable Double amount, @Nullable String currency) {
        PaymentPayload.Builder payload = PaymentPayload.newBuilder()
                .setNonce(generateNonce())
                .setTokenId(token.getId());

        if (amount != null) {
            payload.getAmountBuilder().setValue(amount);
        }
        if (currency != null) {
            payload.getAmountBuilder().setCurrency(currency);
        }

        return client.redeemToken(payload.build());
    }

    /**
     * Looks up an account balance.
     *
     * @return account balance
     */
    public Observable<Money> lookupBalance() {
        return client.lookupBalance(account.getId());
    }

    /**
     * Looks up an existing transaction. Doesn't have to be a transaction for a token payment.
     *
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Observable<Transaction> lookupTransaction(String transactionId) {
        return client.lookupTransaction(account.getId(), transactionId);
    }

    /**
     * Looks up existing transactions. This is a full list of transactions with token payments
     * being a subset.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return payment record
     */
    public Observable<List<Transaction>> lookupTransactions(int offset, int limit) {
        return client.lookupTransactions(account.getId(), offset, limit);
    }
}
