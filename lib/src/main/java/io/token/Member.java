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

import io.reactivex.functions.Function;
import io.token.proto.PagedList;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos.Bank;
import io.token.proto.common.bank.BankProtos.BankInfo;
import io.token.proto.common.blob.BlobProtos.Attachment;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.blob.BlobProtos.Blob.AccessMode;
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
     * Gets user first alias.
     *
     * @return first alias owned by the user
     */
    @Nullable
    public Alias firstAlias() {
        return async.firstAlias();
    }

    /**
     * Gets a list of all aliases owned by the member.
     *
     * @return list of aliases owned by the member
     */
    public List<Alias> aliases() {
        return async.aliases();
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
     * Adds a new alias for the member.
     *
     * @param alias alias, e.g. 'john', must be unique
     */
    public void addAlias(Alias alias) {
        async.addAlias(alias).blockingSingle();
    }

    /**
     * Adds new aliases for the member.
     *
     * @param aliases aliases, e.g. 'john', must be unique
     */
    public void addAliases(List<Alias> aliases) {
        async.addAliases(aliases).blockingSingle();
    }

    /**
     * Removes an alias for the member.
     *
     * @param alias alias, e.g. 'john'
     */
    public void removeAlias(Alias alias) {
        async.removeAlias(alias).blockingSingle();
    }

    /**
     * Removes aliases for the member.
     *
     * @param aliases aliases, e.g. 'john'
     */
    public void removeAliases(List<Alias> aliases) {
        async.removeAliases(aliases).blockingSingle();
    }

    /**
     * Approves a secret key owned by this member. The key is added to the list
     * of valid keys for the member.
     *
     * @param key key to add to the approved list
     * @param level key privilege level
     */
    public void approveKey(SecretKeyPair key, Level level) {
        async.approveKey(key, level).blockingSingle();
    }

    /**
     * Approves a public key owned by this member. The key is added to the list
     * of valid keys for the member.
     *
     * @param key key to add to the approved list
     */
    public void approveKey(Key key) {
        async.approveKey(key).blockingSingle();
    }

    /**
     * Approves public keys owned by this member. The keys are added to the list
     * of valid keys for the member.
     *
     * @param keys keys to add to the approved list
     */
    public void approveKeys(List<Key> keys) {
        async.approveKeys(keys).blockingSingle();
    }

    /**
     * Removes a public key owned by this member.
     *
     * @param keyId key ID of the key to remove
     */
    public void removeKey(String keyId) {
        async.removeKey(keyId).blockingSingle();
    }

    /**
     * Removes public keys owned by this member.
     *
     * @param keyIds key IDs of the keys to remove
     */
    public void removeKeys(List<String> keyIds) {
        async.removeKeys(keyIds).blockingSingle();
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
        return async.subscribeToNotifications(handler, handlerInstructions).blockingSingle();
    }

    /**
     * Gets a list of all subscribers.
     *
     * @return subscribers Subscribers
     */
    public List<Subscriber> getSubscribers() {
        return async.getSubscribers()
                .blockingSingle();
    }

    /**
     * Gets a subscriber by Id.
     *
     * @param subscriberId subscriberId
     * @return subscribers Subscribers
     */
    public Subscriber getSubscriber(String subscriberId) {
        return async.getSubscriber(subscriberId).blockingSingle();
    }

    /**
     * Removes a subscriber by Id.
     *
     * @param subscriberId subscriberId
     */
    public void unsubscribeFromNotifications(String subscriberId) {
        async.unsubscribeFromNotifications(subscriberId).blockingSingle();
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
        return async.getNotifications(offset, limit).blockingSingle();
    }

    /**
     * Gets a notification by id.
     *
     * @param notificationId Id of the notification
     * @return notification
     */
    public Notification getNotification(String notificationId) {
        return async.getNotification(notificationId).blockingSingle();
    }

    /**
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @param authorization an authorization to accounts, from the bank
     * @return list of linked accounts
     */
    public List<Account> linkAccounts(BankAuthorization authorization) {
        return async.linkAccounts(authorization)
                .map(new Function<List<AccountAsync>, List<Account>>() {
                    public List<Account> apply(List<AccountAsync> asyncList) {
                        List<Account> accounts = new LinkedList<>();
                        for (AccountAsync async : asyncList) {
                            accounts.add(async.sync());
                        }
                        return accounts;
                    }
                })
                .blockingSingle();
    }

    /**
     * Unlinks bank accounts previously linked via linkAccounts call.
     *
     * @param accountIds list of account ids to unlink
     */
    public void unlinkAccounts(List<String> accountIds) {
        async.unlinkAccounts(accountIds).blockingSingle();
    }

    /**
     * Looks up funding bank accounts linked to Token.
     *
     * @return list of linked accounts
     */
    public List<Account> getAccounts() {
        return async.getAccounts()
                .map(new Function<List<AccountAsync>, List<Account>>() {
                    public List<Account> apply(List<AccountAsync> asyncList) {
                        List<Account> accounts = new LinkedList<>();
                        for (AccountAsync async : asyncList) {
                            accounts.add(async.sync());
                        }
                        return accounts;
                    }
                })
                .blockingSingle();
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
                .map(new Function<AccountAsync, Account>() {
                    public Account apply(AccountAsync async) {
                        return async.sync();
                    }
                })
                .blockingSingle();
    }

    /**
     * Looks up an existing token transfer.
     *
     * @param transferId ID of the transfer record
     * @return transfer record
     */
    public Transfer getTransfer(String transferId) {
        return async.getTransfer(transferId).blockingSingle();
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
        return async.getTransfers(offset, limit, tokenId).blockingSingle();
    }

    /**
     * Creates a new member address record.
     *
     * @param name the name of the address
     * @param address the address
     * @return the address record created
     */
    public AddressRecord addAddress(String name, Address address) {
        return async.addAddress(name, address).blockingSingle();
    }

    /**
     * Looks up an address by id.
     *
     * @param addressId the address id
     * @return an address record
     */
    public AddressRecord getAddress(String addressId) {
        return async.getAddress(addressId).blockingSingle();
    }

    /**
     * Looks up member addresses.
     *
     * @return a list of addresses
     */
    public List<AddressRecord> getAddresses() {
        return async.getAddresses().blockingSingle();
    }

    /**
     * Deletes a member address by its id.
     *
     * @param addressId the id of the address
     */
    public void deleteAddress(String addressId) {
        async.deleteAddress(addressId).blockingSingle();
    }

    /**
     * Replaces the authenticated member's public profile.
     *
     * @param profile Profile to set
     * @return updated profile
     */
    public Profile setProfile(Profile profile) {
        return async.setProfile(profile).blockingSingle();
    }

    /**
     * Gets a member's public profile.
     *
     * @param memberId member ID of member whose profile we want
     * @return profile info
     */
    public Profile getProfile(String memberId) {
        return async.getProfile(memberId).blockingSingle();
    }

    /**
     * Replaces auth'd member's public profile picture.
     *
     * @param type MIME type of picture
     * @param data image data
     */
    public void setProfilePicture(final String type, byte[] data) {
        async.setProfilePicture(type, data).blockingSingle();
    }

    /**
     * Gets a member's public profile picture.
     *
     * @param memberId member ID of member whose profile we want
     * @param size Size category desired (small/medium/large/original)
     * @return blob with picture; empty blob (no fields set) if has no picture
     */
    public Blob getProfilePicture(String memberId, ProfilePictureSize size) {
        return async.getProfilePicture(memberId, size).blockingSingle();
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
        return async.createAccessToken(accessTokenBuilder).blockingSingle();
    }

    /**
     * Looks up an existing token.
     *
     * @param tokenId token id
     * @return transfer token returned by the server
     */
    public Token getToken(String tokenId) {
        return async.getToken(tokenId).blockingSingle();
    }

    /**
     * Looks up tokens owned by the member.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return transfer tokens owned by the member
     */
    public PagedList<Token, String> getTransferTokens(@Nullable String offset, int limit) {
        return async.getTransferTokens(offset, limit).blockingSingle();
    }

    /**
     * Looks up tokens owned by the member.
     *
     * @param offset optional offset offset to start at
     * @param limit max number of records to return
     * @return transfer tokens owned by the member
     */
    public PagedList<Token, String> getAccessTokens(@Nullable String offset, int limit) {
        return async.getAccessTokens(offset, limit).blockingSingle();
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
        return async.endorseToken(token, keyLevel).blockingSingle();
    }

    /**
     * Cancels the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to cancel
     * @return result of endorsed token
     */
    public TokenOperationResult cancelToken(Token token) {
        return async.cancelToken(token).blockingSingle();
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
                .blockingSingle();
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
                .blockingSingle();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @return transfer record
     */
    public Transfer redeemToken(Token token) {
        return async.redeemToken(token).blockingSingle();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param refId transfer reference id
     * @return transfer record
     */
    public Transfer redeemToken(Token token, String refId) {
        return async.redeemToken(token, refId).blockingSingle();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param destination transfer instruction destination
     * @return transfer record
     */
    public Transfer redeemToken(Token token, TransferEndpoint destination) {
        return async.redeemToken(token, destination).blockingSingle();
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
        return async.redeemToken(token, destination, refId).blockingSingle();
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
                .blockingSingle();
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
                .blockingSingle();
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
                .blockingSingle();
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
                .blockingSingle();
    }

    /**
     * Looks up an existing transaction for a given account.
     *
     * @param accountId the account id
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Transaction getTransaction(String accountId, String transactionId) {
        return async.getTransaction(accountId, transactionId).blockingSingle();
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
        return async.getTransactions(accountId, offset, limit).blockingSingle();
    }

    /**
     * Looks up account balance.
     *
     * @param accountId the account id
     * @return balance
     */
    public Money getBalance(String accountId) {
        return async.getBalance(accountId).blockingSingle();
    }

    /**
     * Returns a list of all token enabled banks.
     *
     * @return a list of banks
     */
    public List<Bank> getBanks() {
        return async.getBanks().blockingSingle();
    }

    /**
     * Returns linking information for the specified bank id.
     *
     * @param bankId the bank id
     * @return bank linking information
     */
    public BankInfo getBankInfo(String bankId) {
        return async.getBankInfo(bankId).blockingSingle();
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
        return async.createBlob(ownerId, type, name, data, accessMode).blockingSingle();
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
                .blockingSingle();
    }

    /**
     * Gets a blob from the server.
     *
     * @param blobId blob Id
     * @return Blob
     */
    public Blob getBlob(String blobId) {
        return async.getBlob(blobId).blockingSingle();
    }

    /**
     * Retrieves a blob that is attached to a token.
     *
     * @param tokenId id of the token
     * @param blobId id of the blob
     * @return Blob
     */
    public Blob getTokenBlob(String tokenId, String blobId) {
        return async.getTokenBlob(tokenId, blobId).blockingSingle();
    }

    /**
     * Creates a test bank account in a fake bank.
     *
     * @param balance account balance to set
     * @param currency currency code, i.e. "EUR"
     * @return bank authorization
     */
    public BankAuthorization createTestBankAccount(double balance, String currency) {
        return async.createTestBankAccount(balance, currency).blockingSingle();
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
