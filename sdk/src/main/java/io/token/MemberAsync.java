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

import static io.token.util.Util.generateNonce;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import io.token.proto.PagedList;
import io.token.proto.banklink.Banklink.AccountLinkingPayloads;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.bank.BankProtos.Bank;
import io.token.proto.common.bank.BankProtos.BankInfo;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.AddressRecord;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberRemoveKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberUsernameOperation;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.security.SecurityProtos.Key;
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
import io.token.security.keystore.SecretKeyPair;
import io.token.util.Util;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

import rx.Observable;

/**
 * Represents a Member in the Token system. Each member has an active secret
 * and public key pair that is used to perform authentication.
 */
public final class MemberAsync {
    private final Client client;
    private final MemberProtos.Member.Builder member;

    /**
     * Creates an instance of {@link MemberAsync}.
     *
     * @param member internal member representation, fetched from server
     * @param client RPC client used to perform operations against the server
     */
    MemberAsync(MemberProtos.Member member, Client client) {
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
    public List<Key> keys() {
        return member.getKeysList();
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
     * @return observable that completes when the operation has finished
     */
    public Observable<Void> addUsername(String username) {
        return addUsernames(Collections.singletonList(username));
    }

    /**
     * Adds new usernames for the member.
     *
     * @param usernames usernames, e.g. 'john', must be unique
     * @return observable that completes when the operation has finished
     */
    public Observable<Void> addUsernames(List<String> usernames) {
        return client
                .updateMember(member.build(), usernames
                        .stream()
                        .map(Util::toAddUsernameOperation)
                        .collect(toList()))
                .map(m -> {
                    member.clear().mergeFrom(m);
                    return null;
                });
    }

    /**
     * Removes an username for the member.
     *
     * @param username username, e.g. 'john'
     * @return observable that completes when the operation has finished
     */
    public Observable<Void> removeUsername(String username) {
        return removeUsernames(Collections.singletonList(username));
    }

    /**
     * Removes usernames for the member.
     *
     * @param usernames usernames, e.g. 'john'
     * @return observable that completes when the operation has finished
     */
    public Observable<Void> removeUsernames(List<String> usernames) {
        return client
                .updateMember(member.build(), usernames
                        .stream()
                        .map(u -> MemberOperation.newBuilder()
                                .setRemoveUsername(MemberUsernameOperation.newBuilder()
                                        .setUsername(u))
                                .build())
                        .collect(toList()))
                .map(m -> {
                    member.clear().mergeFrom(m);
                    return null;
                });
    }

    /**
     * Approves a key owned by this member. The key is added to the list
     * of valid keys for the member.
     *
     * @param key key to add to the approved list
     * @param level key privilege level
     * @return observable that completes when the operation has finished
     */
    public Observable<Void> approveKey(SecretKeyPair key, Key.Level level) {
        return approveKey(Key.newBuilder()
                .setId(key.id())
                .setAlgorithm(Util.toProtoAlgorithm(key.cryptoType()))
                .setLevel(level)
                .setPublicKey(key.publicKeyString())
                .build());
    }

    /**
     * Approves a public key owned by this member. The key is added to the list
     * of valid keys for the member.
     *
     * @param key key to add to the approved list
     * @return observable that completes when the operation has finished
     */
    public Observable<Void> approveKey(Key key) {
        return approveKeys(Collections.singletonList(key));
    }

    /**
     * Approves public keys owned by this member. The keys are added to the list
     * of valid keys for the member.
     *
     * @param keys keys to add to the approved list
     * @return observable that completes when the operation has finished
     */
    public Observable<Void> approveKeys(List<Key> keys) {
        return client
                .updateMember(member.build(), keys
                        .stream()
                        .map(Util::toAddKeyOperation)
                        .collect(toList()))
                .map(m -> {
                    member.clear().mergeFrom(m);
                    return null;
                });
    }

    /**
     * Removes a public key owned by this member.
     *
     * @param keyId key ID of the key to remove
     * @return observable that completes when the operation has finished
     */
    public Observable<Void> removeKey(String keyId) {
        return removeKeys(Collections.singletonList(keyId));
    }

    /**
     * Removes public keys owned by this member.
     *
     * @param keyIds key IDs of the keys to remove
     * @return observable that completes when the operation has finished
     */
    public Observable<Void> removeKeys(List<String> keyIds) {
        return client
                .updateMember(member.build(), keyIds
                        .stream()
                        .map(k -> MemberOperation.newBuilder()
                                .setRemoveKey(MemberRemoveKeyOperation.newBuilder()
                                        .setKeyId(k))
                                .build())
                        .collect(toList()))
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
     * @return observable that completes when the operation has finished
     */
    public Observable<Void> unsubscribeFromNotifications(String subscriberId) {
        return client.unsubscribeFromNotifications(subscriberId)
                .map(empty -> null);
    }

    /**
     * Gets a list of the member's notifications.
     *
     * @param offset offset to start
     * @param limit how many notifications to get
     * @return list of notifications
     */
    public Observable<PagedList<Notification, String>> getNotifications(
            @Nullable String offset,
            int limit) {
        return client.getNotifications(offset, limit);
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
     * @return list of linked accounts
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
     * @return observable that completes when the operation has finished
     */
    public Observable<Void> deleteAddress(String addressId) {
        return client.deleteAddress(addressId);
    }

    /**
     * Creates a new transfer token.
     *
     * @param amount transfer amount
     * @param currency currency code, e.g. "USD"
     * @param accountId account id
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
     * @param accountId account id
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
     * @param keyLevel key level to be used to endorse the token
     * @return result of endorse token
     */
    public Observable<TokenOperationResult> endorseToken(Token token, Key.Level keyLevel) {
        return client.endorseToken(token, keyLevel);
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

    /**
     * Returns a list of all token enabled banks.
     *
     * @return a list of banks
     */
    public Observable<List<Bank>> getBanks() {
        return client.getBanks();
    }

    /**
     * Returns linking information for the specified bank id.
     *
     * @param bankId the bank id
     * @return bank linking information
     */
    public Observable<BankInfo> getBankInfo(String bankId) {
        return client.getBankInfo(bankId);
    }

    /**
     * Creates a test bank account in a fake bank.
     *
     * @param balance account balance to set
     * @param currency currency code, i.e. "EUR"
     * @return account linking payloads
     */
    public Observable<AccountLinkingPayloads> createTestBankAccount(
            double balance,
            String currency) {
        return client.createTestBankAccount(Money.newBuilder()
                .setCurrency(currency)
                .setValue(Double.toString(balance))
                .build());
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
