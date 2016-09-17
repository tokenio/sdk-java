package io.token;

import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.payment.PaymentProtos.Payment;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.Transaction;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a funding account in the Token system.
 */
public final class Account {
    private final AccountAsync async;

    /**
     * @param async real implementation that the calls are delegated to
     */
    Account(AccountAsync async) {
        this.async = async;
    }

    /**
     * @return asynchronous version of the account API
     */
    public AccountAsync async() {
        return async;
    }

    /**
     * @return account owner
     */
    public Member getMember() {
        return async.getMember().sync();
    }

    /**
     * @return account id
     */
    public String getId() {
        return async.getId();
    }

    /**
     * @return account name
     */
    public String getName() {
        return async.getName();
    }

    /**
     * Sets a new bank account.
     *
     * @param newName new name to use
     */
    public void setAccountName(String newName) {
        async.setAccountName(newName).toBlocking().single();
    }

    /**
     * Creates a new payment token.
     *
     * @param amount payment amount
     * @param currency currency code, e.g. "USD"
     * @return payment token returned by the server
     */
    public Token createToken(double amount, String currency) {
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
    public Token createToken(
            double amount,
            String currency,
            @Nullable String redeemer,
            @Nullable String description) {
        return async.createToken(amount, currency, redeemer, description)
                .toBlocking()
                .single();
    }

    /**
     * Looks up a existing token.
     *
     * @param tokenId token id
     * @return payment token returned by the server
     */
    public Token lookupToken(String tokenId) {
        return async.lookupToken(tokenId).toBlocking().single();
    }

    /**
     * Looks up token owned by the member.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return payment tokens owned by the member
     */
    public List<Token> lookupTokens(int offset, int limit) {
        return async.lookupTokens(offset, limit).toBlocking().single();
    }

    /**
     * Endorses the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to endorse
     * @return endorsed token
     */
    public Token endorseToken(Token token) {
        return async.endorseToken(token).toBlocking().single();
    }

    /**
     * Declines the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to decline
     * @return declined token
     */
    public Token declineToken(Token token) {
        return async.declineToken(token).toBlocking().single();
    }

    /**
     * Revoke the token by signing it. The signature is persisted along
     * with the token. Only applicable to endorsed tokens.
     *
     * @param token token to endorse
     * @return endorsed token
     */
    public Token revokeToken(Token token) {
        return async.revokeToken(token).toBlocking().single();
    }

    /**
     * Redeems a payment token.
     *
     * @param token payment token to redeem
     * @return payment record
     */
    public Payment redeemToken(Token token) {
        return async.redeemToken(token).toBlocking().single();
    }

    /**
     * Redeems a payment token.
     *
     * @param token payment token to redeem
     * @param amount payment amount
     * @param currency payment currency code, e.g. "EUR"
     * @return payment record
     */
    public Payment redeemToken(Token token, @Nullable Double amount, @Nullable String currency) {
        return async.redeemToken(token, amount, currency).toBlocking().single();
    }

    /**
     * Looks up an account balance.
     *
     * @return account balance
     */
    public Money lookupBalance() {
        return async.lookupBalance().toBlocking().single();
    }

    /**
     * Looks up an existing transaction. Doesn't have to be a transaction for a token payment.
     *
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Transaction lookupTransaction(String transactionId) {
        return async.lookupTransaction(transactionId).toBlocking().single();
    }

    /**
     * Looks up existing transactions. This is a full list of transactions with token payments
     * being a subset.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return payment record
     */
    public List<Transaction> lookupTransactions(int offset, int limit) {
        return async.lookupTransactions(offset, limit).toBlocking().single();
    }
}
