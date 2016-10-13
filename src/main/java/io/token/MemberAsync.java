package io.token;

import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.subscriber.SubscriberProtos.Platform;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.Address;
import io.token.proto.common.payment.PaymentProtos.Payment;
import io.token.proto.common.payment.PaymentProtos.PaymentPayload;
import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.proto.common.token.TokenProtos.*;
import io.token.proto.common.token.TokenProtos.Access.Resource;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Source;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.rpc.Client;
import io.token.security.SecretKey;
import io.token.util.codec.ByteEncoding;
import rx.Observable;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static io.token.util.Util.generateNonce;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

/**
 * Represents a Member in the Token system. Each member has an active secret
 * and public key pair that is used to perform authentication.
 */
public final class MemberAsync {
    private final SecretKey key;
    private final Client client;
    private final MemberProtos.Member.Builder member;

    /**
     * @param member internal member representation, fetched from server
     * @param key secret/public key pair
     * @param client RPC client used to perform operations against the server
     */
    MemberAsync(MemberProtos.Member member, SecretKey key, Client client) {
        this.key = key;
        this.client = client;
        this.member = member.toBuilder();
    }

    /**
     * Sets the On-Behalf-Of authentication value to be used
     * with this client.  The value must correspond to an existing
     * Access Token ID issued for the client member.
     *
     * @param accessTokenId the access token id
     */
    public void useAccessToken(String accessTokenId) {
        this.client.useAccessToken(accessTokenId);
    }

    /**
     * Clears the access token id from the authentication context used with this client.
     */
    public void clearAccessToken() {
        this.client.clearAccessToken();
    }

    /**
     * @return synchronous version of the account API
     */
    public Member sync() {
        return new Member(this);
    }

    /**
     * @return a unique ID that identifies the member in the Token system
     */
    public String memberId() {
        return member.getId();
    }

    /**
     * @return secret/public keys associated with this member instance
     */
    public SecretKey key() {
        return key;
    }

    /**
     * @return first alias owned by the user
     */
    public String firstAlias() {
        return member.getAliasesCount() == 0 ? null : member.getAliases(0);
    }

    /**
     * @return list of aliases owned by the member
     */
    public List<String> aliases() {
        return member.getAliasesList();
    }

    /**
     * @return list of public keys that are approved for this member
     */
    public List<byte[]> publicKeys() {
        return member.getKeysBuilderList()
                .stream()
                .map(k -> ByteEncoding.parse(k.getPublicKey()))
                .collect(toList());
    }

    /**
     * Checks if a given alias already exists.
     *
     * @param alias alias to check
     * @return {@code true} if alias exists, {@code false} otherwise
     */
    public Observable<Boolean> aliasExists(String alias) {
        return client.aliasExists(alias);
    }

    /**
     * Adds a new alias for the member.
     *
     * @param alias alias, e.g. 'john', must be unique
     */
    public Observable<Void> addAlias(String alias) {
        return client
                .addAlias(member.build(), alias)
                .map(m -> {
                    member.clear().mergeFrom(m);
                    return null;
                });
    }

    /**
     * Removes an alias for the member.
     *
     * @param alias alias, e.g. 'john'
     */
    public Observable<Void> removeAlias(String alias) {
        return client
                .removeAlias(member.build(), alias)
                .map(m -> {
                    member.clear().mergeFrom(m);
                    return null;
                });
    }

    /**
     * Approves a public key owned by this member. The key is added to the list
     * of valid keys for the member.
     *
     * @param publicKey public key to add to the approved list
     * @param level key security level
     */
    public Observable<Void> approveKey(byte[] publicKey, Level level) {
        return client
                .addKey(member.build(), level, publicKey)
                .map(m -> {
                    member.clear().mergeFrom(m);
                    return null;
                });
    }

    /**
     * Removes a public key owned by this member.
     *
     * @param keyId key ID of the key to remove
     */
    public Observable<Void> removeKey(String keyId) {
        return client
                .removeKey(member.build(), keyId)
                .map(m -> {
                    member.clear().mergeFrom(m);
                    return null;
                });
    }

    /**
     * Creates a subscriber to push notifications
     *
     * @param provider notification provider (e.g. Token)
     * @param target notification target (e.g IOS push token)
     * @param platform platform of the device
     * @return subscriber Subscriber
     */
    public Observable<Subscriber> subscribeToNotifications(String provider, String target,
                                                                            Platform platform) {
        return client.subscribeToNotifications(provider, target, platform);
    }

    /**
     * Gets subscribers
     *
     * @return subscribers
     */
    public Observable<List<Subscriber>> getSubscribers() {
        return client.getSubscribers();
    }

    /**
     * Gets a subscriber by id
     *
     * @param subscriberId Id of the subscriber
     * @return subscriber
     */
    public Observable<Subscriber> getSubscriber(String subscriberId) {
        return client.getSubscriber(subscriberId);
    }

    /**
     * Removes a subscriber
     *
     * @param subscriberId subscriberId
     */
    public Observable<Void> unsubscribeFromNotifications(String subscriberId) {
        return client.unsubscribeDevice(subscriberId)
                .map(empty -> null);
    }


    /**
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @param bankId bank id
     * @param accountLinkPayload account link authorization payload generated
     *                           by the bank
     */
    public Observable<List<AccountAsync>> linkAccounts(String bankId, String accountLinkPayload) {
        return client
                .linkAccounts(bankId, accountLinkPayload)
                .map(accounts -> accounts.stream()
                        .map(a -> new AccountAsync(this, a, client))
                        .collect(toList()));
    }

    /**
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @return list of accounts
     */
    public Observable<List<AccountAsync>> getAccount() {
        return client
                .getAccounts()
                .map(accounts -> accounts.stream()
                        .map(a -> new AccountAsync(this, a, client))
                        .collect(toList()));
    }

    /**
     * Looks up a funding bank accounts linked to Token.
     *
     * @param accountId account id
     * @return looked up account
     */
    public Observable<AccountAsync> getAccount(String accountId) {
        return client
                .getAccount(accountId)
                .map(a -> new AccountAsync(this, a, client));
    }

    /**
     * Looks up an existing token payment.
     *
     * @param paymentId ID of the payment record
     * @return payment record
     */
    public Observable<Payment> getPayment(String paymentId) {
        return client.getPayment(paymentId);
    }

    /**
     * Looks up existing token payments.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @param tokenId optional token id to restrict the search
     * @return payment record
     */
    public Observable<List<Payment>> getPayments(int offset, int limit, @Nullable String tokenId) {
        return client.getPayments(offset, limit, tokenId);
    }

    /**
     * Creates a new member address
     *
     * @param name the name of the address
     * @param address the address json
     * @return an address record created
     */
    public Observable<Address> addAddress(String name, String address) {
        return client.addAddress(name, address);
    }

    /**
     * Looks up an address by id
     *
     * @param addressId the address id
     * @return an address record
     */
    public Observable<Address> getAddress(String addressId) {
        return client.getAddress(addressId);
    }

    /**
     * Looks up member addresses
     *
     * @return a list of addresses
     */
    public Observable<List<Address>> getAddresses() {
        return client.getAddresses();
    }

    /**
     * Deletes a member address by its id
     *
     * @param addressId the id of the address
     */
    public Observable<Void> deleteAddress(String addressId) {
        return client.deleteAddress(addressId);
    }

    /**
     * Creates a new payment token.
     *
     * @param amount payment amount
     * @param currency currency code, e.g. "USD"
     * @return payment token returned by the server
     */
    public Observable<Token> createPaymentToken(double amount, String currency, String accountId) {
        return createPaymentToken(amount, currency, accountId, null, null);
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
    public Observable<Token> createPaymentToken(
            double amount,
            String currency,
            String accountId,
            @Nullable String redeemer,
            @Nullable String description) {
        TokenPayload.Builder payload = TokenPayload.newBuilder()
                .setVersion("1.0")
                .setNonce(generateNonce())
                .setFrom(TokenMember.newBuilder()
                        .setId(member.getId()))
                .setBankTransfer(BankTransfer.newBuilder()
                        .setCurrency(currency)
                        .setAmount(Double.toString(amount))
                        .setTransfer(Transfer.newBuilder()
                                .setSource(Source.newBuilder()
                                        .setAccountId(accountId))));

        if (redeemer != null) {
            payload.getBankTransferBuilder().setRedeemer(TokenMember.newBuilder().setAlias(redeemer));
        }
        if (description != null) {
            payload.setDescription(description);
        }
        return createPaymentToken(payload.build());
    }

    /**
     * Creates a new payment token.
     *
     * @param payload payment token payload
     * @return payment token returned by the server
     */
    public Observable<Token> createPaymentToken(TokenPayload payload) {
        return client.createPaymentToken(payload);
    }

    /**
     * Looks up a existing token.
     *
     * @param tokenId token id
     * @return payment token returned by the server
     */
    public Observable<Token> getPaymentToken(String tokenId) {
        return client.getPaymentToken(tokenId);
    }

    /**
     * Looks up token owned by the member.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return payment tokens owned by the member
     */
    public Observable<List<Token>> getPaymentTokens(int offset, int limit) {
        return client.getPaymentTokens(offset, limit);
    }

    /**
     * Endorses the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to endorse
     * @return endorsed token
     */
    public Observable<Token> endorsePaymentToken(Token token) {
        return client.endorsePaymentToken(token);
    }

    /**
     * Cancels the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to cancel
     * @return cancelled token
     */
    public Observable<Token> cancelPaymentToken(Token token) {
        return client.cancelPaymentToken(token);
    }

    /**
     * Redeems a payment token.
     *
     * @param token payment token to redeem
     * @return payment record
     */
    public Observable<Payment> redeemPaymentToken(Token token) {
        return redeemPaymentToken(token, null, null);
    }

    /**
     * Redeems a payment token.
     *
     * @param token payment token to redeem
     * @param amount payment amount
     * @param currency payment currency code, e.g. "EUR"
     * @return payment record
     */
    public Observable<Payment> redeemPaymentToken(Token token, @Nullable Double amount, @Nullable String currency) {
        PaymentPayload.Builder payload = PaymentPayload.newBuilder()
                .setNonce(generateNonce())
                .setTokenId(token.getId());

        if (amount != null) {
            payload.getAmountBuilder().setValue(Double.toString(amount));
        }
        if (currency != null) {
            payload.getAmountBuilder().setCurrency(currency);
        }

        return client.redeemPaymentToken(payload.build());
    }

    /**
     * Looks up an existing transaction for a given account
     *
     * @param accountId the account id
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Observable<Transaction> getTransaction(String accountId, String transactionId) {
        return client.getTransaction(accountId, transactionId);
    }

    /**
     * Looks up transactions for a given account
     *
     * @param accountId the account id
     * @return a list of transaction records
     */
    public Observable<List<Transaction>> getTransactions(String accountId, int offset, int limit) {
        return client.getTransactions(accountId, offset, limit);
    }

    /**
     * Creates an access token for a list of resources
     *
     * @param redeemer the redeemer alias
     * @param resources a list of resources
     * @return the access token created
     */
    public Observable<Token> createAccessToken(String redeemer, List<Resource> resources) {
        TokenPayload.Builder payload = TokenPayload.newBuilder()
                .setVersion("1.0")
                .setNonce(generateNonce())
                .setFrom(TokenMember.newBuilder()
                        .setId(member.getId()))
                .setTo(TokenMember.newBuilder()
                        .setAlias(redeemer));

        payload.getAccessBuilder()
                .addAllResources(resources)
                .build();

        return client.createAccessToken(payload.build());
    }

    /**
     * Creates an address access token
     *
     * @param redeemer the redeemer alias
     * @param addressId an optional address id
     * @return the address access token created
     */
    public Observable<Token> createAddressAccessToken(
            String redeemer,
            @Nullable String addressId) {
        Resource.Address.Builder address = Resource.Address.newBuilder();
        if(addressId != null) {
            address.setAddressId(addressId);
        }
        Resource resource = Resource.newBuilder()
                .setAddress(address.build())
                .build();
        return createAccessToken(redeemer, Collections.singletonList(resource));
    }

    /**
     * Creates an account access token
     *
     * @param redeemer the redeemer alias
     * @param accountId an optional account id
     * @return the account access token created
     */
    public Observable<Token> createAccountAccessToken(
            String redeemer,
            @Nullable String  accountId) {
        Resource.Account.Builder account = Resource.Account.newBuilder();
        if(accountId != null) {
            account.setAccountId(accountId);
        }
        Resource resource = Resource.newBuilder()
                .setAccount(account.build())
                .build();
        return createAccessToken(redeemer, Collections.singletonList(resource));
    }

    /**
     * Creates a transaction access token
     *
     * @param redeemer the redeemer alias
     * @param accountId an optional account id
     * @return the transaction access token created
     */
    public Observable<Token> createTransactionAccessToken(
            String redeemer,
            @Nullable String accountId) {
        Resource.Transaction.Builder transaction = Resource.Transaction.newBuilder();
        if(accountId != null) {
            transaction.setAccountId(accountId);
        }
        Resource resource = Resource.newBuilder()
                .setTransaction(transaction.build())
                .build();
        return createAccessToken(redeemer, Collections.singletonList(resource));
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
