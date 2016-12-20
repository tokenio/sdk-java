package io.token;

import static io.token.util.Util.generateNonce;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import io.token.proto.PagedList;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.AddressRecord;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.proto.common.subscriber.SubscriberProtos.Platform;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TransferBody;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transfer.TransferProtos.TransferPayload;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.Destination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferInstructions;
import io.token.proto.gateway.Gateway.GetTokensRequest;
import io.token.rpc.Client;
import io.token.security.Signer;
import io.token.security.crypto.CryptoRegistry;

import java.security.PublicKey;
import java.util.List;
import javax.annotation.Nullable;
import rx.Observable;

/**
 * Represents a Member in the Token system. Each member has an active secret
 * and public key pair that is used to perform authentication.
 */
public final class MemberAsync {
    private final Signer signer;
    private final Client client;
    private final MemberProtos.Member.Builder member;

    /**
     * Creates an instance of {@link MemberAsync}.
     *
     * @param member internal member representation, fetched from server
     * @param signer the signer to be used with this member
     * @param client RPC client used to perform operations against the server
     */
    MemberAsync(MemberProtos.Member member, Signer signer, Client client) {
        this.signer = signer;
        this.client = client;
        this.member = member.toBuilder();
    }

    /**
     * Gets a sync version of the account API.
     *
     * @return synchronous version of the account API
     */
    public Member sync() {
        return new Member(this);
    }

    /**
     * Gets member ID.
     *
     * @return a unique ID that identifies the member in the Token system
     */
    public String memberId() {
        return member.getId();
    }

    /**
     * Gets {@link Signer} instance.
     *
     * @return the signer associated with this member instance
     */
    public Signer signer() {
        return signer;
    }

    /**
     * Gets the first username owner by the user.
     *
     * @return first username owned by the user
     */
    public String firstUsername() {
        return member.getUsernamesCount() == 0 ? null : member.getUsernames(0);
    }

    /**
     * Gets all usernames owned by the member.
     *
     * @return list of usernames owned by the member
     */
    public List<String> usernames() {
        return member.getUsernamesList();
    }

    /**
     * Gets all public keys for this member.
     *
     * @return list of public keys that are approved for this member
     */
    public List<PublicKey> publicKeys() {
        return member.getKeysBuilderList()
                .stream()
                .map(this::toPublicKey)
                .collect(toList());
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
     * Adds a new username for the member.
     *
     * @param username username, e.g. 'john', must be unique
     */
    public Observable<Void> addUsername(String username) {
        return client
                .addUsername(member.build(), username)
                .map(m -> {
                    member.clear().mergeFrom(m);
                    return null;
                });
    }

    /**
     * Removes an username for the member.
     *
     * @param username username, e.g. 'john'
     */
    public Observable<Void> removeUsername(String username) {
        return client
                .removeUsername(member.build(), username)
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
    public Observable<Void> approveKey(PublicKey publicKey, Level level) {
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
     * @param target notification target (e.g IOS push token)
     * @param platform platform of the device
     * @return subscriber Subscriber
     */
    public Observable<Subscriber> subscribeToNotifications(
            String target,
            Platform platform) {
        return client.subscribeToNotifications(target, platform);
    }

    /**
     * Gets subscribers.
     *
     * @return subscribers
     */
    public Observable<List<Subscriber>> getSubscribers() {
        return client.getSubscribers();
    }

    /**
     * Gets a subscriber by id.
     *
     * @param subscriberId Id of the subscriber
     * @return subscriber
     */
    public Observable<Subscriber> getSubscriber(String subscriberId) {
        return client.getSubscriber(subscriberId);
    }

    /**
     * Removes a subscriber.
     *
     * @param subscriberId subscriberId
     */
    public Observable<Void> unsubscribeFromNotifications(String subscriberId) {
        return client.unsubscribeFromNotifications(subscriberId)
                .map(empty -> null);
    }

    /**
     * Gets a list of the member's notifications.
     *
     * @return list of notifications
     */
    public Observable<List<Notification>> getNotifications() {
        return client.getNotifications();
    }

    /**
     * Gets a notification by id.
     *
     * @param notificationId Id of the notification
     * @return notification
     */
    public Observable<Notification> getNotification(String notificationId) {
        return client.getNotification(notificationId);
    }

    /**
     * Links a funding bank accounts to Token and returns it to the caller.
     *
     * @param bankId bank id
     * @param accountLinkPayloads a list of account payloads to be linked
     */
    public Observable<List<AccountAsync>> linkAccounts(
            String bankId,
            List<SealedMessage> accountLinkPayloads) {
        return client
                .linkAccounts(bankId, accountLinkPayloads)
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
     * Looks up an existing token transfer.
     *
     * @param transferId ID of the transfer record
     * @return transfer record
     */
    public Observable<Transfer> getTransfer(String transferId) {
        return client.getTransfer(transferId);
    }

    /**
     * Looks up existing token transfers.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @param tokenId optional token id to restrict the search
     * @return transfer record
     */
    public Observable<PagedList<Transfer, String>> getTransfers(
            @Nullable String offset,
            int limit,
            @Nullable String tokenId) {
        return client.getTransfers(offset, limit, tokenId);
    }

    /**
     * Creates a new member address.
     *
     * @param name the name of the address
     * @param address the address
     * @return an address record created
     */
    public Observable<AddressRecord> addAddress(String name, Address address) {
        return client.addAddress(name, address);
    }

    /**
     * Looks up an address by id.
     *
     * @param addressId the address id
     * @return an address record
     */
    public Observable<AddressRecord> getAddress(String addressId) {
        return client.getAddress(addressId);
    }

    /**
     * Looks up member addresses.
     *
     * @return a list of addresses
     */
    public Observable<List<AddressRecord>> getAddresses() {
        return client.getAddresses();
    }

    /**
     * Deletes a member address by its id.
     *
     * @param addressId the id of the address
     */
    public Observable<Void> deleteAddress(String addressId) {
        return client.deleteAddress(addressId);
    }

    /**
     * Creates a new transfer token.
     *
     * @param amount transfer amount
     * @param currency currency code, e.g. "USD"
     * @return transfer token returned by the server
     */
    public Observable<Token> createToken(double amount, String currency, String accountId) {
        return createToken(amount, currency, accountId, null, null);
    }

    /**
     * Creates a new transfer token.
     *
     * @param amount transfer amount
     * @param currency currency code, e.g. "USD"
     * @param redeemer redeemer username
     * @param description transfer description, optional
     * @return transfer token returned by the server
     */
    public Observable<Token> createToken(
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
                .setTransfer(TransferBody.newBuilder()
                        .setCurrency(currency)
                        .setLifetimeAmount(Double.toString(amount))
                        .setInstructions(TransferInstructions.newBuilder()
                                .setSource(TransferInstructionsProtos.Source.newBuilder()
                                        .setAccountId(accountId))));

        if (redeemer != null) {
            payload
                    .getTransferBuilder()
                    .setRedeemer(TokenMember.newBuilder().setUsername(redeemer));
        }
        if (description != null) {
            payload.setDescription(description);
        }
        return createToken(payload.build());
    }

    /**
     * Creates a new token.
     *
     * @param payload transfer token payload
     * @return transfer token returned by the server
     */
    public Observable<Token> createToken(TokenPayload payload) {
        return client.createToken(payload);
    }

    /**
     * Creates an access token built from a given {@link AccessTokenBuilder}.
     *
     * @param accessTokenBuilder an {@link AccessTokenBuilder} to create access token from
     * @return the access token created
     */
    public Observable<Token> createAccessToken(AccessTokenBuilder accessTokenBuilder) {
        return createToken(accessTokenBuilder.from(memberId()).build());
    }

    /**
     * Looks up a existing token.
     *
     * @param tokenId token id
     * @return transfer token returned by the server
     */
    public Observable<Token> getToken(String tokenId) {
        return client.getToken(tokenId);
    }

    /**
     * Looks up transfer tokens owned by the member.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return transfer tokens owned by the member
     */
    public Observable<PagedList<Token, String>> getTransferTokens(
            @Nullable String offset,
            int limit) {
        return client.getTokens(GetTokensRequest.Type.TRANSFER, offset, limit);
    }

    /**
     * Looks up access tokens owned by the member.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return transfer tokens owned by the member
     */
    public Observable<PagedList<Token, String>> getAccessTokens(
            @Nullable String offset,
            int limit) {
        return client.getTokens(GetTokensRequest.Type.ACCESS, offset, limit);
    }

    /**
     * Endorses the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to endorse
     * @return result of endorse token
     */
    public Observable<TokenOperationResult> endorseToken(Token token) {
        return client.endorseToken(token);
    }

    /**
     * Cancels the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to cancel
     * @return result of cancel token
     */
    public Observable<TokenOperationResult> cancelToken(Token token) {
        return client.cancelToken(token);
    }

    /**
     * Cancels the existing access token and creates a replacement for it.
     *
     * @param tokenToCancel old token to cancel
     * @param tokenToCreate an {@link AccessTokenBuilder} to create new token from
     * @return result of the replacement operation
     */
    public Observable<TokenOperationResult> replaceAccessToken(
            Token tokenToCancel,
            AccessTokenBuilder tokenToCreate) {
        return client.replace(
                tokenToCancel,
                tokenToCreate.from(memberId()).build());
    }

    /**
     * Cancels the existing access token, creates a replacement and endorses it.
     *
     * @param tokenToCancel old token to cancel
     * @param tokenToCreate an {@link AccessTokenBuilder} to create new token from
     * @return result of the replacement operation
     */
    public Observable<TokenOperationResult> replaceAndEndorseAccessToken(
            Token tokenToCancel,
            AccessTokenBuilder tokenToCreate) {
        return client.replaceAndEndorseToken(
                tokenToCancel,
                tokenToCreate.from(memberId()).build());
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(Token token) {
        return redeemToken(token, null, null, null, null);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param amount transfer amount
     * @param currency transfer currency code, e.g. "EUR"
     * @param description transfer description
     * @param destination the transfer instruction destination
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable Destination destination) {
        TransferPayload.Builder payload = TransferPayload.newBuilder()
                .setNonce(generateNonce())
                .setTokenId(token.getId())
                .setDescription(token
                        .getPayload()
                        .getDescription());

        if (destination != null) {
            payload.addDestinations(destination);
        }
        if (amount != null) {
            payload.getAmountBuilder().setValue(Double.toString(amount));
        }
        if (currency != null) {
            payload.getAmountBuilder().setCurrency(currency);
        }
        if (description != null) {
            payload.setDescription(description);
        }
        if (destination != null) {
            payload.addDestinations(destination);
        }

        return client.createTransfer(payload.build());
    }

    /**
     * Looks up an existing transaction for a given account.
     *
     * @param accountId the account id
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Observable<Transaction> getTransaction(String accountId, String transactionId) {
        return client.getTransaction(accountId, transactionId);
    }

    /**
     * Looks up transactions for a given account.
     *
     * @param accountId the account id
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return a list of transaction records
     */
    public Observable<PagedList<Transaction, String>> getTransactions(
            String accountId,
            @Nullable String offset,
            int limit) {
        return client.getTransactions(accountId, offset, limit);
    }

    /**
     * Looks up account balance.
     *
     * @param accountId the account id
     * @return balance
     */
    public Observable<Money> getBalance(String accountId) {
        return client.getBalance(accountId);
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }

    private PublicKey toPublicKey(SecurityProtos.Key.Builder key) {
        return CryptoRegistry
                .getInstance()
                .cryptoFor(key.getAlgorithm())
                .toPublicKey(key.getPublicKey());
    }
}
