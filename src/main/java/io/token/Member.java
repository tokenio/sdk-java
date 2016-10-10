package io.token;

import io.token.proto.common.device.DeviceProtos.Platform;
import io.token.proto.common.member.MemberProtos.Address;
import io.token.proto.common.payment.PaymentProtos.Payment;
import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.proto.common.token.TokenProtos.AccessToken;
import io.token.proto.common.token.TokenProtos.AccessToken.Resource;
import io.token.proto.common.token.TokenProtos.PaymentToken;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.security.SecretKey;

import javax.annotation.Nullable;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

/**
 * Represents a Member in the Token system. Each member has an active secret
 * and public key pair that is used to perform authentication.
 */
public final class Member {
    private final MemberAsync async;

    /**
     * Sets the On-Behalf-Of authentication value to be used
     * with this client.  The value must correspond to an existing
     * Access Token ID issued for the client member.
     *
     * @param onBehalfOf the on-behalf-of value
     */
    public void setOnBehalfOf(String onBehalfOf) {
        this.async.setOnBehalfOf(onBehalfOf);
    }

    /**
     * Clears the On-Behalf-Of value used with this client.
     */
    public void clearOnBehalfOf() {
        this.async.clearOnBehalfOf();
    }

    /**
     * @param async real implementation that the calls are delegated to
     */
    public Member(MemberAsync async) {
        this.async = async;
    }

    /**
     * @return asynchronous version of the account API
     */
    public MemberAsync async() {
        return async;
    }

    /**
     * @return a unique ID that identifies the member in the Token system
     */
    public String memberId() {
        return async.memberId();
    }

    /**
     * @return secret/public keys associated with this member instance
     */
    public SecretKey key() {
        return async.key();
    }

    /**
     * @return first alias owned by the user
     */
    public String firstAlias() {
        return async.firstAlias();
    }

    /**
     * @return list of aliases owned by the member
     */
    public List<String> aliases() {
        return async.aliases();
    }

    /**
     * @return list of public keys that are approved for this member
     */
    public List<byte[]> publicKeys() {
        return async.publicKeys();
    }

    /**
     * Checks if a given alias already exists.
     *
     * @param alias alias to check
     * @return {@code true} if alias exists, {@code false} otherwise
     */
    public boolean aliasExists(String alias) {
        return async.aliasExists(alias).toBlocking().single();
    }

    /**
     * Adds a new alias for the member.
     *
     * @param alias alias, e.g. 'john', must be unique
     */
    public void addAlias(String alias) {
        async.addAlias(alias).toBlocking().single();
    }

    /**
     * Removes an alias for the member.
     *
     * @param alias alias, e.g. 'john'
     */
    public void removeAlias(String alias) {
        async.removeAlias(alias).toBlocking().single();
    }

    /**
     * Approves a public key owned by this member. The key is added to the list
     * of valid keys for the member.
     *
     * @param publicKey public key to add to the approved list
     * @param level key security level
     */
    public void approveKey(byte[] publicKey, Level level) {
        async.approveKey(publicKey, level).toBlocking().single();
    }

    /**
     * Removes a public key owned by this member.
     *
     * @param keyId key ID of the key to remove
     */
    public void removeKey(String keyId) {
        async.removeKey(keyId).toBlocking().single();
    }

    /**
     * Subscribes a device to receive push notifications
     *
     * @param provider notification provider (e.g. Token)
     * @param notificationUri uri of the device (e.g. iOS push token)
     * @param platform platform of the device
     * @param tags tags for the device
     * @return nothing
     */
    public void subscribeDevice(String provider, String notificationUri,
                                Platform platform, List<String> tags) {
        async.subscribeDevice(provider, notificationUri, platform, tags)
                .toBlocking()
                .single();
    }

    /**
     * Unsubscribes a device to from push notifications
     *
     * @param provider notification provider (e.g. Token)
     * @param notificationUri uri of the device (e.g. iOS push token)
     * @return nothing
     */
    public void unsubscribeDevice(String provider, String notificationUri) {
        async.unsubscribeDevice(provider, notificationUri)
                .toBlocking()
                .single();
    }

    /**
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @param bankId bank id
     * @param accountLinkPayload account link authorization payload generated
     *                           by the bank
     */
    public List<Account> linkAccounts(String bankId, String accountLinkPayload) {
        return async.linkAccounts(bankId, accountLinkPayload)
                .map(l -> l.stream()
                        .map(AccountAsync::sync)
                        .collect(toList()))
                .toBlocking()
                .single();
    }

    /**
     * Looks up funding bank accounts linked to Token.
     *
     * @return list of accounts
     */
    public List<Account> getAccounts() {
        return async.getAccount()
                .map(l -> l.stream()
                        .map(AccountAsync::sync)
                        .collect(toList()))
                .toBlocking()
                .single();
    }

    /**
     * Looks up a funding bank accounts linked to Token.
     *
     * @param accountId account id
     * @return looked up account
     */
    public Account getAccount(String accountId) {
        return async.getAccount(accountId)
                .map(AccountAsync::sync)
                .toBlocking()
                .single();
    }

    /**
     * Looks up an existing token payment.
     *
     * @param paymentId ID of the payment record
     * @return payment record
     */
    public Payment getPayment(String paymentId) {
        return async.getPayment(paymentId).toBlocking().single();
    }

    /**
     * Looks up existing token payments.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @param tokenId optional token id to restrict the search
     * @return payment record
     */
    public List<Payment> getPayments(int offset, int limit, @Nullable String tokenId) {
        return async.getPayments(offset, limit, tokenId).toBlocking().single();
    }

    /**
     * Creates a new member address record.
     *
     * @param name the name of the address
     * @param address the address json
     * @return the address record created
     */
    public Address addAddress(String name, String address) {
        return async.addAddress(name, address).toBlocking().single();
    }

    /**
     * Looks up an address by id.
     *
     * @param addressId the address id
     * @return an address record
     */
    public Address getAddress(String addressId) {
        return async.getAddress(addressId).toBlocking().single();
    }

    /**
     * Looks up member addresses.
     *
     * @return a list of addresses
     */
    public List<Address> getAddresses() {
        return async.getAddresses().toBlocking().single();
    }

    /**
     * Deletes a member address by its id.
     *
     * @param addressId the id of the address
     */
    public void deleteAddress(String addressId) {
        async.deleteAddress(addressId).toBlocking().single();
    }

    /**
     * Creates a new payment token.
     *
     * @param amount payment amount
     * @param currency currency code, e.g. "USD"
     * @param accountId the funding account id
     * @return payment token returned by the server
     */
    public PaymentToken createPaymentToken(double amount, String currency, String accountId) {
        return createPaymentToken(amount, currency, accountId, null, null);
    }

    /**
     * Creates a new payment token.
     *
     * @param amount payment amount
     * @param currency currency code, e.g. "USD"
     * @param accountId the funding account id
     * @param redeemer redeemer alias
     * @param description payment description, optional
     * @return payment token returned by the server
     */
    public PaymentToken createPaymentToken(
            double amount,
            String currency,
            String accountId,
            @Nullable String redeemer,
            @Nullable String description) {
        return async.createPaymentToken(amount, currency, accountId, redeemer, description)
                .toBlocking()
                .single();
    }

    /**
     * Looks up a existing token.
     *
     * @param tokenId token id
     * @return payment token returned by the server
     */
    public PaymentToken getPaymentToken(String tokenId) {
        return async.getPaymentToken(tokenId).toBlocking().single();
    }

    /**
     * Looks up tokens owned by the member.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return payment tokens owned by the member
     */
    public List<PaymentToken> getPaymentTokens(int offset, int limit) {
        return async.getPaymentTokens(offset, limit).toBlocking().single();
    }

    /**
     * Endorses the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to endorse
     * @return endorsed token
     */
    public PaymentToken endorsePaymentToken(PaymentToken token) {
        return async.endorsePaymentToken(token).toBlocking().single();
    }

    /**
     * Cancels the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to cancel
     * @return cancelled token
     */
    public PaymentToken cancelPaymentToken(PaymentToken token) {
        return async.cancelPaymentToken(token).toBlocking().single();
    }

    /**
     * Redeems a payment token.
     *
     * @param token payment token to redeem
     * @return payment record
     */
    public Payment redeemPaymentToken(PaymentToken token) {
        return async.redeemPaymentToken(token).toBlocking().single();
    }

    /**
     * Redeems a payment token.
     *
     * @param token payment token to redeem
     * @param amount payment amount
     * @param currency payment currency code, e.g. "EUR"
     * @return payment record
     */
    public Payment redeemPaymentToken(PaymentToken token, @Nullable Double amount, @Nullable String currency) {
        return async.redeemPaymentToken(token, amount, currency).toBlocking().single();
    }

    /**
     * Looks up an existing transaction for a given account
     *
     * @param accountId the account id
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Transaction getTransaction(String accountId, String transactionId) {
        return async.getTransaction(accountId, transactionId)
                .toBlocking()
                .single();
    }

    /**
     * Looks up transactions for a given account
     *
     * @param accountId the account id
     * @return a list of transaction record
     */
    public List<Transaction> getTransactions(String accountId, int offset, int limit) {
        return async.getTransactions(accountId, offset, limit)
                .toBlocking()
                .single();
    }

    /**
     * Creates an access token for a list of resources
     *
     * @param redeemer the redeemer alias
     * @param resources a list of resources
     * @return the access token created
     */
    public AccessToken createAccessToken(String redeemer, List<Resource> resources) {
        return async.createAccessToken(redeemer, resources)
                .toBlocking()
                .single();
    }

    /**
     * Creates an address access token
     *
     * @param redeemer the redeemer alias
     * @param addressId an optional address id
     * @return the address access token created
     */
    public AccessToken createAddressAccessToken(String redeemer, @Nullable String addressId) {
        return async.createAddressAccessToken(redeemer, addressId)
                .toBlocking()
                .single();
    }

    /**
     * Creates an account access token
     *
     * @param redeemer the redeemer alias
     * @param accountId an optional account id
     * @return the account access token created
     */
    public AccessToken createAccountAccessToken(String redeemer, @Nullable String accountId) {
        return async.createAccountAccessToken(redeemer, accountId)
                .toBlocking()
                .single();
    }

    /**
     * Creates a transaction access token
     *
     * @param redeemer the redeemer alias
     * @param accountId an optional account id
     * @return the transaction access token created
     */
    public AccessToken createTransactionAccessToken(String redeemer, @Nullable String  accountId) {
        return async.createTransactionAccessToken(redeemer, accountId)
                .toBlocking()
                .single();
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
