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

import static io.token.proto.common.address.AddressProtos.Address;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import io.token.proto.PagedList;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.bank.BankProtos.Bank;
import io.token.proto.common.bank.BankProtos.BankInfo;
import io.token.proto.common.blob.BlobProtos.Attachment;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.blob.BlobProtos.Blob.AccessMode;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.AddressRecord;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.member.MemberProtos.ProfilePictureSize;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.security.keystore.SecretKeyPair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import rx.functions.Func1;

/**
 * Represents a Member in the Token system. Each member has an active secret
 * and public key pair that is used to perform authentication.
 */
public final class Member {
    private final MemberAsync async;

    /**
     * Creates an instance with a {@link MemberAsync} all calls are delegated to.
     *
     * @param async real implementation that the calls are delegated to
     */
    public Member(MemberAsync async) {
        this.async = async;
    }

    /**
     * Gets a {@link MemberAsync} delegate.
     *
     * @return asynchronous version of the account API
     */
    public MemberAsync async() {
        return async;
    }

    /**
     * Gets member ID.
     *
     * @return a unique ID that identifies the member in the Token system
     */
    public String memberId() {
        return async.memberId();
    }

    /**
     * Gets user first username.
     *
     * @return first username owned by the user
     */
    public String firstUsername() {
        return async.firstUsername();
    }

    /**
     * Gets a list of all usernames owned by the member.
     *
     * @return list of usernames owned by the member
     */
    public List<String> usernames() {
        return async.usernames();
    }

    /**
     * Gets all public keys for this member.
     *
     * @return list of public keys that are approved for this member
     */
    public List<Key> keys() {
        return async.keys();
    }

    /**
     * Sets the On-Behalf-Of authentication value to be used
     * with this client.  The value must correspond to an existing
     * Access Token ID issued for the client member.
     *
     * @param accessTokenId the access token id
     */
    public void useAccessToken(String accessTokenId) {
        this.async.useAccessToken(accessTokenId);
    }

    /**
     * Clears the access token value used with this client.
     */
    public void clearAccessToken() {
        this.async.clearAccessToken();
    }

    /**
     * Adds a new username for the member.
     *
     * @param username username, e.g. 'john', must be unique
     */
    public void addUsername(String username) {
        async.addUsername(username).toBlocking().single();
    }

    /**
     * Adds new usernames for the member.
     *
     * @param usernames usernames, e.g. 'john', must be unique
     */
    public void addUsernames(List<String> usernames) {
        async.addUsernames(usernames).toBlocking().single();
    }

    /**
     * Removes an username for the member.
     *
     * @param username username, e.g. 'john'
     */
    public void removeUsername(String username) {
        async.removeUsername(username).toBlocking().single();
    }

    /**
     * Removes usernames for the member.
     *
     * @param usernames usernames, e.g. 'john'
     */
    public void removeUsernames(List<String> usernames) {
        async.removeUsernames(usernames).toBlocking().single();
    }

    /**
     * Approves a secret key owned by this member. The key is added to the list
     * of valid keys for the member.
     *
     * @param key key to add to the approved list
     * @param level key privilege level
     */
    public void approveKey(SecretKeyPair key, Level level) {
        async.approveKey(key, level).toBlocking().single();
    }

    /**
     * Approves a public key owned by this member. The key is added to the list
     * of valid keys for the member.
     *
     * @param key key to add to the approved list
     */
    public void approveKey(Key key) {
        async.approveKey(key).toBlocking().single();
    }

    /**
     * Approves public keys owned by this member. The keys are added to the list
     * of valid keys for the member.
     *
     * @param keys keys to add to the approved list
     */
    public void approveKeys(List<Key> keys) {
        async.approveKeys(keys).toBlocking().single();
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
     * Removes public keys owned by this member.
     *
     * @param keyIds key IDs of the keys to remove
     */
    public void removeKeys(List<String> keyIds) {
        async.removeKeys(keyIds).toBlocking().single();
    }

    /**
     * Subscribes a device to receive push notifications.
     *
     * @param handler specify the handler of the notifications
     * @return subscriber Subscriber
     */
    public Subscriber subscribeToNotifications(
            String handler) {
        return subscribeToNotifications(handler, new HashMap<String, String>());
    }

    /**
     * Subscribes a device to receive push notifications.
     *
     * @param handler specify the handler of the notifications
     * @param handlerInstructions map of instructions for the handler
     * @return subscriber Subscriber
     */
    public Subscriber subscribeToNotifications(
            String handler,
            Map<String, String> handlerInstructions) {
        return async.subscribeToNotifications(handler, handlerInstructions).toBlocking().single();
    }

    /**
     * Gets a list of all subscribers.
     *
     * @return subscribers Subscribers
     */
    public List<Subscriber> getSubscribers() {
        return async.getSubscribers()
                .toBlocking()
                .single();
    }

    /**
     * Gets a subscriber by Id.
     *
     * @param subscriberId subscriberId
     * @return subscribers Subscribers
     */
    public Subscriber getSubscriber(String subscriberId) {
        return async.getSubscriber(subscriberId).toBlocking().single();
    }

    /**
     * Removes a subscriber by Id.
     *
     * @param subscriberId subscriberId
     */
    public void unsubscribeFromNotifications(String subscriberId) {
        async.unsubscribeFromNotifications(subscriberId).toBlocking().single();
    }


    /**
     * Gets a list of the member's notifications.
     *
     * @param offset offset to start
     * @param limit how many notifications to get
     * @return list of notifications
     */
    public PagedList<Notification, String> getNotifications(
            @Nullable String offset,
            int limit) {
        return async.getNotifications(offset, limit).toBlocking().single();
    }

    /**
     * Gets a notification by id.
     *
     * @param notificationId Id of the notification
     * @return notification
     */
    public Notification getNotification(String notificationId) {
        return async.getNotification(notificationId).toBlocking().single();
    }

    /**
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @param authorization an authorization to accounts, from the bank
     * @return list of linked accounts
     */
    public List<Account> linkAccounts(BankAuthorization authorization) {
        return async.linkAccounts(authorization)
                .map(new Func1<List<AccountAsync>, List<Account>>() {
                    public List<Account> call(List<AccountAsync> asyncList) {
                        List<Account> accounts = new LinkedList<>();
                        for (AccountAsync async : asyncList) {
                            accounts.add(async.sync());
                        }
                        return accounts;
                    }
                })
                .toBlocking()
                .single();
    }

    /**
     * Unlinks bank accounts previously linked via linkAccounts call.
     *
     * @param accountIds list of account ids to unlink
     */
    public void unlinkAccounts(List<String> accountIds) {
        async.unlinkAccounts(accountIds).toBlocking().single();
    }

    /**
     * Looks up funding bank accounts linked to Token.
     *
     * @return list of linked accounts
     */
    public List<Account> getAccounts() {
        return async.getAccount()
                .map(new Func1<List<AccountAsync>, List<Account>>() {
                    public List<Account> call(List<AccountAsync> asyncList) {
                        List<Account> accounts = new LinkedList<>();
                        for (AccountAsync async : asyncList) {
                            accounts.add(async.sync());
                        }
                        return accounts;
                    }
                })
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
        return async
                .getAccount(accountId)
                .map(new Func1<AccountAsync, Account>() {
                    public Account call(AccountAsync async) {
                        return async.sync();
                    }
                })
                .toBlocking()
                .single();
    }

    /**
     * Looks up an existing token transfer.
     *
     * @param transferId ID of the transfer record
     * @return transfer record
     */
    public Transfer getTransfer(String transferId) {
        return async.getTransfer(transferId).toBlocking().single();
    }

    /**
     * Looks up existing token transfers.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @param tokenId optional token id to restrict the search
     * @return transfer record
     */
    public PagedList<Transfer, String> getTransfers(
            @Nullable String offset,
            int limit,
            @Nullable String tokenId) {
        return async.getTransfers(offset, limit, tokenId).toBlocking().single();
    }

    /**
     * Creates a new member address record.
     *
     * @param name the name of the address
     * @param address the address
     * @return the address record created
     */
    public AddressRecord addAddress(String name, Address address) {
        return async.addAddress(name, address).toBlocking().single();
    }

    /**
     * Looks up an address by id.
     *
     * @param addressId the address id
     * @return an address record
     */
    public AddressRecord getAddress(String addressId) {
        return async.getAddress(addressId).toBlocking().single();
    }

    /**
     * Looks up member addresses.
     *
     * @return a list of addresses
     */
    public List<AddressRecord> getAddresses() {
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
     * Replaces the authenticated member's public profile.
     *
     * @param profile Profile to set
     * @return updated profile
     */
    public Profile setProfile(Profile profile) {
        return async.setProfile(profile).toBlocking().single();
    }

    /**
     * Gets a member's public profile.
     *
     * @param memberId member ID of member whose profile we want
     * @return profile info
     */
    public Profile getProfile(String memberId) {
        return async.getProfile(memberId).toBlocking().single();
    }

    /**
     * Replaces auth'd member's public profile picture.
     *
     * @param type MIME type of picture
     * @param data image data
     */
    public void setProfilePicture(final String type, byte[] data) {
        async.setProfilePicture(type, data).toBlocking().single();
    }

    /**
     * Gets a member's public profile picture.
     *
     * @param memberId member ID of member whose profile we want
     * @param size Size category desired (small/medium/large/original)
     * @return profile info
     */
    public Blob getProfilePicture(String memberId, ProfilePictureSize size) {
        return async.getProfilePicture(memberId, size).toBlocking().single();
    }

    /**
     * Creates a new transfer token builder.
     *
     * @param amount transfer amount
     * @param currency currency code, e.g. "USD"
     * @return transfer token returned by the server
     */
    public TransferTokenBuilder createTransferToken(double amount, String currency) {
        return new TransferTokenBuilder(async, amount, currency);
    }

    /**
     * Creates an access token built from a given {@link AccessTokenBuilder}.
     *
     * @param accessTokenBuilder an {@link AccessTokenBuilder} to create access token from
     * @return the access token created
     */
    public Token createAccessToken(AccessTokenBuilder accessTokenBuilder) {
        return async.createAccessToken(accessTokenBuilder).toBlocking().single();
    }

    /**
     * Looks up an existing token.
     *
     * @param tokenId token id
     * @return transfer token returned by the server
     */
    public Token getToken(String tokenId) {
        return async.getToken(tokenId).toBlocking().single();
    }

    /**
     * Looks up tokens owned by the member.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return transfer tokens owned by the member
     */
    public PagedList<Token, String> getTransferTokens(@Nullable String offset, int limit) {
        return async.getTransferTokens(offset, limit).toBlocking().single();
    }

    /**
     * Looks up tokens owned by the member.
     *
     * @param offset optional offset offset to start at
     * @param limit max number of records to return
     * @return transfer tokens owned by the member
     */
    public PagedList<Token, String> getAccessTokens(@Nullable String offset, int limit) {
        return async.getAccessTokens(offset, limit).toBlocking().single();
    }

    /**
     * Endorses the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to endorse
     * @param keyLevel key level to be used to endorse the token
     * @return result of endorse token
     */
    public TokenOperationResult endorseToken(Token token, Key.Level keyLevel) {
        return async.endorseToken(token, keyLevel).toBlocking().single();
    }

    /**
     * Cancels the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to cancel
     * @return result of endorsed token
     */
    public TokenOperationResult cancelToken(Token token) {
        return async.cancelToken(token).toBlocking().single();
    }

    /**
     * Cancels the existing access token, creates a replacement and optionally endorses it.
     *
     * @param tokenToCancel old token to cancel
     * @param tokenToCreate an {@link AccessTokenBuilder} to create new token from
     * @return result of the replacement operation
     */
    public TokenOperationResult replaceAccessToken(
            Token tokenToCancel,
            AccessTokenBuilder tokenToCreate) {
        return async
                .replaceAccessToken(tokenToCancel, tokenToCreate)
                .toBlocking()
                .single();
    }

    /**
     * Cancels the existing access token, creates a replacement and optionally endorses it.
     *
     * @param tokenToCancel old token to cancel
     * @param tokenToCreate an {@link AccessTokenBuilder} to create new token from
     * @return result of the replacement operation
     */
    public TokenOperationResult replaceAndEndorseAccessToken(
            Token tokenToCancel,
            AccessTokenBuilder tokenToCreate) {
        return async
                .replaceAndEndorseAccessToken(tokenToCancel, tokenToCreate)
                .toBlocking()
                .single();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @return transfer record
     */
    public Transfer redeemToken(Token token) {
        return async.redeemToken(token).toBlocking().single();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param refId transfer reference id
     * @return transfer record
     */
    public Transfer redeemToken(Token token, String refId) {
        return async.redeemToken(token, refId).toBlocking().single();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param destination transfer instruction destination
     * @return transfer record
     */
    public Transfer redeemToken(Token token, TransferEndpoint destination) {
        return async.redeemToken(token, destination).toBlocking().single();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param destination transfer instruction destination
     * @param refId transfer reference id
     * @return transfer record
     */
    public Transfer redeemToken(Token token, TransferEndpoint destination, String refId) {
        return async.redeemToken(token, destination, refId).toBlocking().single();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param amount transfer amount
     * @param currency transfer currency code, e.g. "EUR"
     * @param description transfer description
     * @return transfer record
     */
    public Transfer redeemToken(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description) {
        return async
                .redeemToken(token, amount, currency, description, null, null)
                .toBlocking()
                .single();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param amount transfer amount
     * @param currency transfer currency code, e.g. "EUR"
     * @param destination transfer instruction destination
     * @return transfer record
     */
    public Transfer redeemToken(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable TransferEndpoint destination) {
        return async
                .redeemToken(token, amount, currency, null, destination, null)
                .toBlocking()
                .single();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param amount transfer amount
     * @param currency transfer currency code, e.g. "EUR"
     * @param description transfer description
     * @param destination transfer instruction destination
     * @return transfer record
     */
    public Transfer redeemToken(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable TransferEndpoint destination) {
        return async
                .redeemToken(token, amount, currency, description, destination, null)
                .toBlocking()
                .single();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param amount transfer amount
     * @param currency transfer currency code, e.g. "EUR"
     * @param description transfer description
     * @param destination transfer instruction destination
     * @param refId transfer reference id
     * @return transfer record
     */
    public Transfer redeemToken(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable TransferEndpoint destination,
            @Nullable String refId) {
        return async
                .redeemToken(token, amount, currency, description, destination, refId)
                .toBlocking()
                .single();
    }

    /**
     * Looks up an existing transaction for a given account.
     *
     * @param accountId the account id
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Transaction getTransaction(String accountId, String transactionId) {
        return async.getTransaction(accountId, transactionId).toBlocking().single();
    }

    /**
     * Looks up transactions for a given account.
     *
     * @param accountId the account id
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return a list of transaction record
     */
    public PagedList<Transaction, String> getTransactions(
            String accountId,
            @Nullable String offset,
            int limit) {
        return async.getTransactions(accountId, offset, limit).toBlocking().single();
    }

    /**
     * Looks up account balance.
     *
     * @param accountId the account id
     * @return balance
     */
    public Money getBalance(String accountId) {
        return async.getBalance(accountId).toBlocking().single();
    }

    /**
     * Returns a list of all token enabled banks.
     *
     * @return a list of banks
     */
    public List<Bank> getBanks() {
        return async.getBanks().toBlocking().single();
    }

    /**
     * Returns linking information for the specified bank id.
     *
     * @param bankId the bank id
     * @return bank linking information
     */
    public BankInfo getBankInfo(String bankId) {
        return async.getBankInfo(bankId).toBlocking().single();
    }

    /**
     * Creates and uploads a blob.
     *
     * @param ownerId id of the owner of the blob
     * @param type MIME type of the file
     * @param name name
     * @param data file data
     * @param accessMode Default or public
     * @return blob Id
     */
    public Attachment createBlob(
            String ownerId,
            String type,
            String name,
            byte[] data,
            AccessMode accessMode) {
        return async.createBlob(ownerId, type, name, data, accessMode).toBlocking().single();
    }

    /**
     * Creates and uploads a blob.
     *
     * @param ownerId id of the owner of the blob
     * @param type MIME type of the file
     * @param name name
     * @param data file data
     * @return blob Id
     */
    public Attachment createBlob(
            String ownerId,
            String type,
            String name,
            byte[] data) {
        return async
                .createBlob(ownerId, type, name, data, AccessMode.DEFAULT)
                .toBlocking()
                .single();
    }

    /**
     * Gets a blob from the server.
     *
     * @param blobId blob Id
     * @return Blob
     */
    public Blob getBlob(String blobId) {
        return async.getBlob(blobId).toBlocking().single();
    }

    /**
     * Retrieves a blob that is attached to a token.
     *
     * @param tokenId id of the token
     * @param blobId id of the blob
     * @return Blob
     */
    public Blob getTokenBlob(String tokenId, String blobId) {
        return async.getTokenBlob(tokenId, blobId).toBlocking().single();
    }

    /**
     * Creates a test bank account in a fake bank.
     *
     * @param balance account balance to set
     * @param currency currency code, i.e. "EUR"
     * @return bank authorization
     */
    public BankAuthorization createTestBankAccount(double balance, String currency) {
        return async.createTestBankAccount(balance, currency).toBlocking().single();
    }

    @Override
    public int hashCode() {
        return async.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Member) {
            return async.equals(((Member) obj).async);
        } else {
            return async.equals(obj);
        }
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
