package io.token;

import io.token.proto.PagedList;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.Address;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.proto.common.subscriber.SubscriberProtos.Platform;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TransferBody;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Source;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transfer.TransferProtos.TransferInstructions;
import io.token.proto.gateway.Gateway.GetTokensRequest;
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
     * @return first username owned by the user
     */
    public String firstUsername() {
        return member.getUsernamesCount() == 0 ? null : member.getUsernames(0);
    }

    /**
     * @return list of usernames owned by the member
     */
    public List<String> usernames() {
        return member.getUsernamesList();
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
     * Checks if a given username already exists.
     *
     * @param username username to check
     * @return {@code true} if username exists, {@code false} otherwise
     */
    public Observable<Boolean> usernameExists(String username) {
        return client.usernameExists(username);
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
    public Observable<Subscriber> subscribeToNotifications(
            String provider,
            String target,
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
        return client.unsubscribeFromNotifications(subscriberId)
                .map(empty -> null);
    }


    /**
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @param bankId bank id
     * @param accountLinkPayload account link authorization payload generated
     * by the bank
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
                        .setAmount(Double.toString(amount))
                        .setInstructions(TransferInstructions.newBuilder()
                                .setSource(Source.newBuilder()
                                        .setAccountId(accountId))));

        if (redeemer != null) {
            payload.getTransferBuilder().setRedeemer(TokenMember.newBuilder().setUsername(redeemer));
        }
        if (description != null) {
            payload.setDescription(description);
        }
        return createToken(payload.build());
    }

    /**
     * Creates an access token for a list of resources.
     *
     * @param redeemer the redeemer username
     * @param resources a list of resources
     * @return the access token created
     */
    public Observable<Token> createToken(String redeemer, List<Resource> resources) {
        TokenPayload.Builder payload = TokenPayload.newBuilder()
                .setVersion("1.0")
                .setNonce(generateNonce())
                .setFrom(TokenMember.newBuilder()
                        .setId(member.getId()))
                .setTo(TokenMember.newBuilder()
                        .setUsername(redeemer));

        payload.getAccessBuilder()
                .addAllResources(resources)
                .build();

        return createToken(payload.build());
    }

    /**
     * Creates an access token for any address.
     *
     * @param redeemer the redeemer username
     * @return the address access token created
     */
    public Observable<Token> createAddressesAccessToken(String redeemer) {
        Resource resource = Resource.newBuilder()
                .setAllAddresses(Resource.AllAddresses.getDefaultInstance())
                .build();
        return createToken(redeemer, Collections.singletonList(resource));
    }

    /**
     * Creates an address access token for a given address id.
     *
     * @param redeemer the redeemer username
     * @param addressId an address id
     * @return the address access token created
     */
    public Observable<Token> createAddressAccessToken(
            String redeemer,
            String addressId) {
        Resource resource = Resource.newBuilder()
                .setAddress(Resource.Address.newBuilder()
                        .setAddressId(addressId)
                        .build())
                .build();
        return createToken(redeemer, Collections.singletonList(resource));
    }

    /**
     * Creates an access token for any account.
     *
     * @param redeemer the redeemer username
     * @return the account access token created
     */
    public Observable<Token> createAccountsAccessToken(String redeemer) {
        Resource resource = Resource.newBuilder()
                .setAllAccounts(Resource.AllAccounts.getDefaultInstance())
                .build();
        return createToken(redeemer, Collections.singletonList(resource));
    }

    /**
     * Creates an account access token for a given account id.
     *
     * @param redeemer the redeemer username
     * @param accountId an account id
     * @return the account access token created
     */
    public Observable<Token> createAccountAccessToken(
            String redeemer,
            String accountId) {
        Resource resource = Resource.newBuilder()
                .setAccount(Resource.Account.newBuilder()
                        .setAccountId(accountId)
                        .build())
                .build();
        return createToken(redeemer, Collections.singletonList(resource));
    }

    /**
     * Creates an access token for any account transactions.
     *
     * @param redeemer the redeemer username
     * @return the transaction access token created
     */
    public Observable<Token> createTransactionsAccessToken(String redeemer) {
        Resource resource = Resource.newBuilder()
                .setAllTransactions(Resource.AllAccountTransactions.getDefaultInstance())
                .build();
        return createToken(redeemer, Collections.singletonList(resource));
    }

    /**
     * Creates a transaction access token for a given account id.
     *
     * @param redeemer the redeemer username
     * @param accountId an account id
     * @return the transaction access token created
     */
    public Observable<Token> createTransactionsAccessToken(
            String redeemer,
            String accountId) {
        Resource resource = Resource.newBuilder()
                .setTransactions(Resource.AccountTransactions.newBuilder()
                        .setAccountId(accountId)
                        .build())
                .build();
        return createToken(redeemer, Collections.singletonList(resource));
    }

    /**
     * Creates an access token for any account balance.
     *
     * @param redeemer the redeemer username
     * @return the balance access token created
     */
    public Observable<Token> createBalancesAccessToken(String redeemer) {
        Resource resource = Resource.newBuilder()
                .setAllBalances(Resource.AllAccountBalances.getDefaultInstance())
                .build();
        return createToken(redeemer, Collections.singletonList(resource));
    }

    /**
     * Creates a balance access token for a given account id.
     *
     * @param redeemer the redeemer username
     * @param accountId an account id
     * @return the balance access token created
     */
    public Observable<Token> createBalanceAccessToken(
            String redeemer,
            String accountId) {
        Resource resource = Resource.newBuilder()
                .setBalance(Resource.AccountBalance.newBuilder()
                        .setAccountId(accountId)
                        .build())
                .build();
        return createToken(redeemer, Collections.singletonList(resource));
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
     * @return endorsed token
     */
    public Observable<Token> endorseToken(Token token) {
        return client.endorseToken(token);
    }

    /**
     * Cancels the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to cancel
     * @return cancelled token
     */
    public Observable<Token> cancelToken(Token token) {
        return client.cancelToken(token);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @return transfer record
     */
    public Observable<Transfer> createTransfer(Token token) {
        return createTransfer(token, null, null);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param amount transfer amount
     * @param currency transfer currency code, e.g. "EUR"
     * @return transfer record
     */
    public Observable<Transfer> createTransfer(
            Token token,
            @Nullable Double amount,
            @Nullable String currency) {
        Transfer.Payload.Builder payload = Transfer.Payload.newBuilder()
                .setNonce(generateNonce())
                .setTokenId(token.getId());

        if (amount != null) {
            payload.getAmountBuilder().setValue(Double.toString(amount));
        }
        if (currency != null) {
            payload.getAmountBuilder().setCurrency(currency);
        }

        return client.createTransfer(payload.build());
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
     * Looks up account balance
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
}
