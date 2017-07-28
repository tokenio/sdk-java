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

import static io.token.proto.common.blob.BlobProtos.Blob.AccessMode.PUBLIC;
import static io.token.util.Util.generateNonce;
import static io.token.util.Util.hashAlias;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import com.google.protobuf.ByteString;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.proto.PagedList;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos.Bank;
import io.token.proto.common.bank.BankProtos.BankInfo;
import io.token.proto.common.blob.BlobProtos.Attachment;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.blob.BlobProtos.Blob.AccessMode;
import io.token.proto.common.blob.BlobProtos.Blob.Payload;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.AddressRecord;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberRemoveKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberAliasOperation;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.member.MemberProtos.ProfilePictureSize;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transfer.TransferProtos.TransferPayload;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.proto.gateway.Gateway.GetTokensRequest;
import io.token.rpc.Client;
import io.token.rpc.util.Unit;
import io.token.security.keystore.SecretKeyPair;
import io.token.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Member in the Token system. Each member has an active secret
 * and public key pair that is used to perform authentication.
 */
public final class MemberAsync {
    private static final Logger logger = LoggerFactory.getLogger(MemberAsync.class);

    private final Client client;
    private final MemberProtos.Member.Builder member;

    private List<Alias> aliases;

    /**
     * Creates an instance of {@link MemberAsync}.
     *
     * @param member internal member representation, fetched from server
     * @param client RPC client used to perform operations against the server
     */
    MemberAsync(MemberProtos.Member member, Client client) {
        this.client = client;
        this.member = member.toBuilder();
        this.aliases = new ArrayList<>(); //TODO(PR1005): Sync aliases with server
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
     * Gets the first alias owner by the user.
     *
     * @return first alias owned by the user
     */
    public Alias firstAlias() {
        return aliases.isEmpty() ? null : aliases.get(0);
    }

    /**
     * Gets all aliases owned by the member.
     *
     * @return list of aliases owned by the member
     */
    public List<Alias> aliases() {
        return aliases;
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
     * Adds a new alias for the member.
     *
     * @param alias alias, e.g. 'john', must be unique
     * @return observable that completes when the operation has finished
     */
    public Observable<Unit> addAlias(Alias alias) {
        return addAliases(Collections.singletonList(alias));
    }

    /**
     * Adds new aliases for the member.
     *
     * @param aliases aliases, e.g. 'john', must be unique
     * @return observable that completes when the operation has finished
     */
    public Observable<Unit> addAliases(final List<Alias> aliases) {
        List<MemberOperation> operations = new LinkedList<>();
        for (Alias alias : aliases) {
            operations.add(Util.toAddAliasOperation(alias));
        }
        return client
                .updateMember(member.build(), operations)
                .map(new Function<MemberProtos.Member, Unit>() {
                    public Unit apply(MemberProtos.Member proto) {
                        member.clear().mergeFrom(proto);
                        MemberAsync.this.aliases.addAll(aliases);
                        return Unit.INSTANCE;
                    }
                });
    }

    /**
     * Removes an alias for the member.
     *
     * @param alias alias, e.g. 'john'
     * @return observable that completes when the operation has finished
     */
    public Observable<Unit> removeAlias(Alias alias) {
        return removeAliases(Collections.singletonList(alias));
    }

    /**
     * Removes aliases for the member.
     *
     * @param aliases aliases, e.g. 'john'
     * @return observable that completes when the operation has finished
     */
    public Observable<Unit> removeAliases(final List<Alias> aliases) {
        List<MemberOperation> operations = new LinkedList<>();
        for (Alias alias : aliases) {
            operations.add(MemberOperation
                    .newBuilder()
                    .setRemoveAlias(MemberAliasOperation
                            .newBuilder()
                            .setAliasHash(hashAlias(alias)))
                    .build());
        }
        return client
                .updateMember(member.build(), operations)
                .map(new Function<MemberProtos.Member, Unit>() {
                    public Unit apply(MemberProtos.Member proto) {
                        MemberAsync.this.aliases.removeAll(aliases);
                        member.clear().mergeFrom(proto);
                        return Unit.INSTANCE;
                    }
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
    public Observable<Unit> approveKey(SecretKeyPair key, Key.Level level) {
        return approveKey(Key.newBuilder()
                .setId(key.id())
                .setAlgorithm(key.cryptoType().getKeyAlgorithm())
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
    public Observable<Unit> approveKey(Key key) {
        return approveKeys(Collections.singletonList(key));
    }

    /**
     * Approves public keys owned by this member. The keys are added to the list
     * of valid keys for the member.
     *
     * @param keys keys to add to the approved list
     * @return observable that completes when the operation has finished
     */
    public Observable<Unit> approveKeys(List<Key> keys) {
        List<MemberOperation> operations = new LinkedList<>();
        for (Key key : keys) {
            operations.add(Util.toAddKeyOperation(key));
        }
        return client
                .updateMember(member.build(), operations)
                .map(new Function<MemberProtos.Member, Unit>() {
                    public Unit apply(MemberProtos.Member proto) {
                        member.clear().mergeFrom(proto);
                        return Unit.INSTANCE;
                    }
                });
    }

    /**
     * Removes a public key owned by this member.
     *
     * @param keyId key ID of the key to remove
     * @return observable that completes when the operation has finished
     */
    public Observable<Unit> removeKey(String keyId) {
        return removeKeys(Collections.singletonList(keyId));
    }

    /**
     * Removes public keys owned by this member.
     *
     * @param keyIds key IDs of the keys to remove
     * @return observable that completes when the operation has finished
     */
    public Observable<Unit> removeKeys(List<String> keyIds) {
        List<MemberOperation> operations = new LinkedList<>();
        for (String keyId : keyIds) {
            operations.add(MemberOperation
                    .newBuilder()
                    .setRemoveKey(MemberRemoveKeyOperation
                            .newBuilder()
                            .setKeyId(keyId))
                    .build());
        }
        return client
                .updateMember(member.build(), operations)
                .map(new Function<MemberProtos.Member, Unit>() {
                    public Unit apply(MemberProtos.Member proto) {
                        member.clear().mergeFrom(proto);
                        return Unit.INSTANCE;
                    }
                });
    }

    /**
     * Creates a subscriber to push notifications.
     *
     * @param handler specify the handler of the notifications
     * @param handlerInstructions map of instructions for the handler
     * @return subscriber Subscriber
     */
    public Observable<Subscriber> subscribeToNotifications(
            String handler,
            Map<String, String> handlerInstructions) {
        return client.subscribeToNotifications(handler, handlerInstructions);
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
    public Observable<Unit> unsubscribeFromNotifications(String subscriberId) {
        return client
                .unsubscribeFromNotifications(subscriberId)
                .map(new Function<Unit, Unit>() {
                    public Unit apply(Unit ignore) {
                        return Unit.INSTANCE;
                    }
                });
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
     * @param authorization an authorization to accounts, from the bank
     * @return list of linked accounts
     */
    public Observable<List<AccountAsync>> linkAccounts(
            BankAuthorization authorization) {
        return client
                .linkAccounts(authorization)
                .map(new Function<List<AccountProtos.Account>, List<AccountAsync>>() {
                    @Override
                    public List<AccountAsync> apply(List<AccountProtos.Account> accounts) {
                        List<AccountAsync> result = new LinkedList<>();
                        for (AccountProtos.Account account : accounts) {
                            result.add(new AccountAsync(MemberAsync.this, account, client));
                        }
                        return result;
                    }
                });
    }

    /**
     * Unlinks bank accounts previously linked via linkAccounts call.
     *
     * @param accountIds account ids to unlink
     * @return nothing
     */
    public Observable<Unit> unlinkAccounts(List<String> accountIds) {
        return client.unlinkAccounts(accountIds);
    }

    /**
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @return list of accounts
     */
    public Observable<List<AccountAsync>> getAccounts() {
        return client
                .getAccounts()
                .map(new Function<List<AccountProtos.Account>, List<AccountAsync>>() {
                    @Override
                    public List<AccountAsync> apply(List<AccountProtos.Account> accounts) {
                        List<AccountAsync> result = new LinkedList<>();
                        for (AccountProtos.Account account : accounts) {
                            result.add(new AccountAsync(MemberAsync.this, account, client));
                        }
                        return result;
                    }
                });
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
                .map(new Function<AccountProtos.Account, AccountAsync>() {
                    public AccountAsync apply(AccountProtos.Account account) {
                        return new AccountAsync(MemberAsync.this, account, client);
                    }
                });
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
     * Creates and uploads a blob.
     *
     * @param ownerId id of the owner of the blob
     * @param type MIME type of the file
     * @param name name
     * @param data file data
     * @return attachment
     */
    public Observable<Attachment> createBlob(
            String ownerId,
            final String type,
            final String name,
            byte[] data) {
        return createBlob(ownerId, type, name, data, AccessMode.DEFAULT);
    }

    /**
     * Creates and uploads a blob.
     *
     * @param ownerId id of the owner of the blob
     * @param type MIME type of the file
     * @param name name
     * @param data file data
     * @param accessMode Normal access or public
     * @return attachment
     */
    public Observable<Attachment> createBlob(
            String ownerId,
            final String type,
            final String name,
            byte[] data,
            AccessMode accessMode) {
        Payload payload = Payload
                .newBuilder()
                .setOwnerId(ownerId)
                .setType(type)
                .setName(name)
                .setData(ByteString.copyFrom(data))
                .setAccessMode(accessMode)
                .build();
        return client.createBlob(payload)
                .map(new Function<String, Attachment>() {
                    public Attachment apply(String id) {
                        return Attachment.newBuilder()
                                .setBlobId(id)
                                .setName(name)
                                .setType(type)
                                .build();
                    }
                });
    }

    /**
     * Retrieves a blob from the server.
     *
     * @param blobId id of the blob
     * @return Blob
     */
    public Observable<Blob> getBlob(String blobId) {
        return client.getBlob(blobId);
    }

    /**
     * Retrieves a blob that is attached to a transfer token.
     *
     * @param tokenId id of the token
     * @param blobId id of the blob
     * @return Blob
     */
    public Observable<Blob> getTokenBlob(String tokenId, String blobId) {
        return client.getTokenBlob(tokenId, blobId);
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
    public Observable<Unit> deleteAddress(String addressId) {
        return client.deleteAddress(addressId);
    }

    /**
     * Replaces auth'd member's public profile.
     *
     * @param profile profile to set
     * @return updated profile
     */
    public Observable<Profile> setProfile(Profile profile) {
        return client.setProfile(profile);
    }

    /**
     * Gets a member's public profile. Unlike setProfile, you can get another member's profile.
     *
     * @param memberId member ID of member whose profile we want
     * @return their profile
     */
    public Observable<Profile> getProfile(String memberId) {
        return client.getProfile(memberId);
    }

    /**
     * Replaces auth'd member's public profile picture.
     *
     * @param type MIME type of picture
     * @param data image data
     * @return observable that completes when the operation has finished
     */
    public Observable<Unit> setProfilePicture(final String type, byte[] data) {
        Payload payload = Payload.newBuilder()
                .setOwnerId(memberId())
                .setType(type)
                .setName("profile")
                .setData(ByteString.copyFrom(data))
                .setAccessMode(PUBLIC)
                .build();
        return client.setProfilePicture(payload);
    }

    /**
     * Gets a member's public profile picture. Unlike set, you can get another member's picture.
     *
     * @param memberId member ID of member whose profile we want
     * @param size desired size category (small, medium, large, original)
     * @return blob with picture; empty blob (no fields set) if has no picture
     */
    public Observable<Blob> getProfilePicture(String memberId, ProfilePictureSize size) {
        return client.getProfilePicture(memberId, size);
    }

    /**
     * Creates a new transfer token builder.
     *
     * @param amount transfer amount
     * @param currency currency code, e.g. "USD"
     * @return transfer token returned by the server
     */
    public TransferTokenBuilder createTransferToken(double amount, String currency) {
        return new TransferTokenBuilder(this, amount, currency);
    }

    /**
     * Creates a new transfer token from a token payload.
     *
     * @param payload transfer token payload
     * @return transfer token returned by the server
     */
    public Observable<Token> createTransferToken(TokenPayload payload) {
        return client.createTransferToken(payload);
    }

    /**
     * Creates an access token built from a given {@link AccessTokenBuilder}.
     *
     * @param accessTokenBuilder an {@link AccessTokenBuilder} to create access token from
     * @return the access token created
     */
    public Observable<Token> createAccessToken(AccessTokenBuilder accessTokenBuilder) {
        return client.createAccessToken(accessTokenBuilder.from(memberId()).build());
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
        return redeemToken(token, null, null, null, null, null);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param refId transfer reference id
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(Token token, String refId) {
        return redeemToken(token, null, null, null, null, refId);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param destination transfer instruction destination
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(Token token, TransferEndpoint destination) {
        return redeemToken(token, null, null, null, destination, null);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param destination transfer instruction destination
     * @param refId transfer reference id
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(
            Token token,
            TransferEndpoint destination,
            String refId) {
        return redeemToken(token, null, null, null, destination, refId);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param amount transfer amount
     * @param currency transfer currency code, e.g. "EUR"
     * @param description transfer description
     * @param destination the transfer instruction destination
     * @param refId transfer reference id
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable TransferEndpoint destination,
            @Nullable String refId) {
        TransferPayload.Builder payload = TransferPayload.newBuilder()
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
        if (refId != null) {
            payload.setRefId(refId);
        } else {
            logger.warn("refId is not set. A random ID will be used.");
            payload.setRefId(generateNonce());
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
     * @return bank authorization
     */
    public Observable<BankAuthorization> createTestBankAccount(
            double balance,
            String currency) {
        return client.createTestBankAccount(Money.newBuilder()
                .setCurrency(currency)
                .setValue(Double.toString(balance))
                .build());
    }

    @Override
    public int hashCode() {
        return member.getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MemberAsync)) {
            return false;
        }

        MemberAsync other = (MemberAsync) obj;
        return member.build().equals(other.member.build());
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
