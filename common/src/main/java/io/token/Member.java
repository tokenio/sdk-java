/**
 * Copyright (c) 2019 Token, Inc.
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

import static io.reactivex.Completable.fromObservable;
import static io.token.proto.common.blob.BlobProtos.Blob.AccessMode.PUBLIC;
import static io.token.util.Util.TOKEN_REALM;
import static io.token.util.Util.hashAlias;
import static io.token.util.Util.normalizeAlias;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

import com.google.protobuf.ByteString;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.token.TokenClient.TokenCluster;
import io.token.exceptions.InvalidRealmException;
import io.token.exceptions.NoAliasesFoundException;
import io.token.proto.PagedList;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos.BankInfo;
import io.token.proto.common.blob.BlobProtos.Attachment;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.blob.BlobProtos.Blob.AccessMode;
import io.token.proto.common.blob.BlobProtos.Blob.Payload;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.Member.Builder;
import io.token.proto.common.member.MemberProtos.MemberAliasOperation;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation.Authorization;
import io.token.proto.common.member.MemberProtos.MemberRecoveryRulesOperation;
import io.token.proto.common.member.MemberProtos.MemberRemoveKeyOperation;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.member.MemberProtos.ProfilePictureSize;
import io.token.proto.common.member.MemberProtos.RecoveryRule;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.SecurityMetadata;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.transaction.TransactionProtos.Balance;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.rpc.Client;
import io.token.security.keystore.SecretKeyPair;
import io.token.util.Util;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Represents a Member in the Token system. Each member has an active secret
 * and public key pair that is used to perform authentication.
 */
public class Member<AccT extends Account> {
    protected final Client client;
    protected final Builder member;
    protected final TokenCluster cluster;

    /**
     * Creates an instance of {@link Member}.
     *
     * @param member internal member representation, fetched from server
     * @param client RPC client used to perform operations against the server
     * @param cluster Token cluster, e.g. sandbox, production
     */
    protected Member(
            MemberProtos.Member member,
            Client client,
            TokenCluster cluster) {
        this.client = client;
        this.member = member.toBuilder();
        this.cluster = cluster;
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
     * Gets the last hash.
     *
     * @return an observable of the last hash
     */
    public Observable<String> lastHash() {
        return client
                .getMember(member.getId())
                .map(new Function<MemberProtos.Member, String>() {
                    public String apply(MemberProtos.Member member) {
                        return member.getLastHash();
                    }
                });
    }

    /**
     * Gets the last hash.
     *
     * @return the last hash
     */
    public String lastHashBlocking() {
        return lastHash().blockingSingle();
    }

    /**
     * Gets the first alias owner by the user.
     *
     * @return first alias owned by the user, or throws exception if no aliases are found
     */
    public Observable<Alias> firstAlias() {
        return client.getAliases()
                .map(new Function<List<Alias>, Alias>() {
                    public Alias apply(List<Alias> aliases) {
                        if (aliases.isEmpty()) {
                            throw new NoAliasesFoundException(memberId());
                        } else {
                            return aliases.get(0);
                        }
                    }
                });
    }

    /**
     * Gets user first alias.
     *
     * @return first alias owned by the user, or throws exception if not aliases are found
     */
    public Alias firstAliasBlocking() {
        return firstAlias().blockingSingle();
    }

    /**
     * Gets all aliases owned by the member.
     *
     * @return list of aliases owned by the member
     */
    public Observable<List<Alias>> aliases() {
        return client.getAliases();
    }

    /**
     * Gets a list of all aliases owned by the member.
     *
     * @return list of aliases owned by the member
     */
    public List<Alias> aliasesBlocking() {
        return aliases().blockingSingle();
    }

    /**
     * Gets all public keys for this member.
     *
     * @return list of public keys that are approved for this member
     */
    public Observable<List<Key>> getKeys() {
        return client.getMember(memberId())
                .map(new Function<MemberProtos.Member, List<Key>>() {
                    @Override
                    public List<Key> apply(MemberProtos.Member updated) {
                        return member.clear().mergeFrom(updated).getKeysList();
                    }
                });
    }

    /**
     * Gets all public keys for this member.
     *
     * @return list of public keys that are approved for this member
     */
    public List<Key> getKeysBlocking() {
        return getKeys().blockingSingle();
    }

    /**
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @return list of accounts
     */
    public Observable<List<AccT>> getAccounts() {
        return client
                .getAccounts()
                .map(new Function<List<AccountProtos.Account>, List<AccT>>() {
                    @Override
                    public List<AccT> apply(List<AccountProtos.Account> accounts) {
                        List<AccT> result = new LinkedList<>();
                        for (AccountProtos.Account account : accounts) {
                            result.add((AccT) new Account(Member.this, account, client));
                        }
                        return result;
                    }
                });
    }

    /**
     * Looks up funding bank accounts linked to Token.
     *
     * @return list of linked accounts
     */
    public List<AccT> getAccountsBlocking() {
        return getAccounts().blockingSingle();
    }

    /**
     * Looks up a funding bank account linked to Token.
     *
     * @param accountId account id
     * @return looked up account
     */
    public Observable<AccT> getAccount(String accountId) {
        return client
                .getAccount(accountId)
                .map(new Function<AccountProtos.Account, AccT>() {
                    public AccT apply(AccountProtos.Account account) {
                        return (AccT) new Account(Member.this, account, client);
                    }
                });
    }

    /**
     * Looks up a funding bank account linked to Token.
     *
     * @param accountId account id
     * @return looked up account
     */
    public Account getAccountBlocking(String accountId) {
        return getAccount(accountId).blockingSingle();
    }

    /**
     * Adds a new alias for the member.
     *
     * @param alias alias, e.g. 'john', must be unique
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable addAlias(Alias alias) {
        return addAliases(singletonList(alias));
    }

    /**
     * Adds a new alias for the member.
     *
     * @param alias alias, e.g. 'john', must be unique
     */
    public void addAliasBlocking(Alias alias) {
        addAlias(alias).blockingAwait();
    }

    /**
     * Adds new aliases for the member.
     *
     * @param aliasList aliases, e.g. 'john', must be unique
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable addAliases(final List<Alias> aliasList) {
        final List<MemberOperation> operations = new LinkedList<>();
        final List<MemberOperationMetadata> metadata = new LinkedList<>();
        for (Alias alias : aliasList) {
            String partnerId = member.getPartnerId();
            if (!partnerId.isEmpty() && !partnerId.equals(TOKEN_REALM)) {
                // Realm must equal member's partner ID if affiliated
                if (!alias.getRealm().isEmpty() && !alias.getRealm().equals(partnerId)) {
                    throw new InvalidRealmException(alias.getRealm(), partnerId);
                }
                alias = alias.toBuilder()
                        .setRealm(partnerId)
                        .build();
            }

            operations.add(Util.toAddAliasOperation(normalizeAlias(alias)));
            metadata.add(Util.toAddAliasOperationMetadata(normalizeAlias(alias)));
        }
        return fromObservable(client
                .getMember(memberId())
                .flatMap(new Function<MemberProtos.Member, Observable<Boolean>>() {
                    public Observable<Boolean> apply(MemberProtos.Member latest) {
                        return client
                                .updateMember(
                                        member.setLastHash(latest.getLastHash()).build(),
                                        operations,
                                        metadata)
                                .map(new Function<MemberProtos.Member, Boolean>() {
                                    public Boolean apply(MemberProtos.Member proto) {
                                        member.clear().mergeFrom(proto);
                                        return true;
                                    }
                                });
                    }
                }));
    }

    /**
     * Adds new aliases for the member.
     *
     * @param aliases aliases, e.g. 'john', must be unique
     */
    public void addAliasesBlocking(List<Alias> aliases) {
        addAliases(aliases).blockingAwait();
    }

    /**
     * Retries alias verification.
     *
     * @param alias the alias to be verified
     * @return the verification id
     */
    public Observable<String> retryVerification(Alias alias) {
        return client.retryVerification(alias);
    }

    /**
     * Retries alias verification.
     *
     * @param alias the alias to be verified
     * @return the verification id
     */
    public String retryVerificationBlocking(Alias alias) {
        return retryVerification(alias).blockingSingle();
    }

    /**
     * Adds the recovery rule.
     *
     * @param recoveryRule the recovery rule
     * @return an observable of updated member
     */
    public Observable<MemberProtos.Member> addRecoveryRule(final RecoveryRule recoveryRule) {
        return client
                .getMember(memberId())
                .flatMap(new Function<MemberProtos.Member, Observable<MemberProtos.Member>>() {
                    @Override
                    public Observable<MemberProtos.Member> apply(MemberProtos.Member member) {
                        return client.updateMember(
                                member,
                                singletonList(MemberOperation.newBuilder()
                                        .setRecoveryRules(MemberRecoveryRulesOperation.newBuilder()
                                                .setRecoveryRule(recoveryRule)).build()));
                    }
                });
    }

    /**
     * Adds the recovery rule.
     *
     * @param recoveryRule the recovery rule
     * @return updated member
     */
    public MemberProtos.Member addRecoveryRuleBlocking(RecoveryRule recoveryRule) {
        return addRecoveryRule(recoveryRule).blockingSingle();
    }

    /**
     * Set Token as the recovery agent.
     *
     * @return a completable
     */
    public Completable useDefaultRecoveryRule() {
        return client.useDefaultRecoveryRule();
    }

    /**
     * Set Token as the recovery agent.
     */
    public void useDefaultRecoveryRuleBlocking() {
        useDefaultRecoveryRule().blockingAwait();
    }

    /**
     * Authorizes recovery as a trusted agent.
     *
     * @param authorization the authorization
     * @return the signature
     */
    public Observable<Signature> authorizeRecovery(Authorization authorization) {
        return client.authorizeRecovery(authorization);
    }

    /**
     * Authorizes recovery as a trusted agent.
     *
     * @param authorization the authorization
     * @return the signature
     */
    public Signature authorizeRecoveryBlocking(Authorization authorization) {
        return authorizeRecovery(authorization).blockingSingle();
    }

    /**
     * Gets the member id of the default recovery agent.
     *
     * @return the member id
     */
    public Observable<String> getDefaultAgent() {
        return client.getDefaultAgent();
    }

    /**
     * Gets the member id of the default recovery agent.
     *
     * @return the member id
     */
    public String getDefaultAgentBlocking() {
        return getDefaultAgent().blockingSingle();
    }

    /**
     * Verifies a given alias.
     *
     * @param verificationId the verification id
     * @param code the code
     * @return a completable
     */
    public Completable verifyAlias(String verificationId, String code) {
        return client.verifyAlias(verificationId, code);
    }

    /**
     * Verifies a given alias.
     *
     * @param verificationId the verification id
     * @param code the code
     */
    public void verifyAliasBlocking(String verificationId, String code) {
        verifyAlias(verificationId, code).blockingAwait();
    }

    /**
     * Removes an alias for the member.
     *
     * @param alias alias, e.g. 'john'
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable removeAlias(Alias alias) {
        return removeAliases(singletonList(alias));
    }

    /**
     * Removes an alias for the member.
     *
     * @param alias alias, e.g. 'john'
     */
    public void removeAliasBlocking(Alias alias) {
        removeAlias(alias).blockingAwait();
    }

    /**
     * Removes aliases for the member.
     *
     * @param aliasList aliases, e.g. 'john'
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable removeAliases(final List<Alias> aliasList) {
        final List<MemberOperation> operations = new LinkedList<>();
        for (Alias alias : aliasList) {
            operations.add(MemberOperation
                    .newBuilder()
                    .setRemoveAlias(MemberAliasOperation
                            .newBuilder()
                            .setAliasHash(hashAlias(alias)))
                    .build());
        }
        return fromObservable(client
                .getMember(memberId())
                .flatMap(new Function<MemberProtos.Member, Observable<Boolean>>() {
                    public Observable<Boolean> apply(MemberProtos.Member latest) {
                        return client
                                .updateMember(
                                        member.setLastHash(latest.getLastHash()).build(),
                                        operations)
                                .map(new Function<MemberProtos.Member, Boolean>() {
                                    public Boolean apply(MemberProtos.Member proto) {
                                        member.clear().mergeFrom(proto);
                                        return true;
                                    }
                                });
                    }
                }));
    }

    /**
     * Removes aliases for the member.
     *
     * @param aliases aliases, e.g. 'john'
     */
    public void removeAliasesBlocking(List<Alias> aliases) {
        removeAliases(aliases).blockingAwait();
    }

    /**
     * Approves a key owned by this member. The key is added to the list
     * of valid keys for the member.
     *
     * @param key key to add to the approved list
     * @param level key privilege level
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable approveKey(SecretKeyPair key, Key.Level level) {
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
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable approveKey(Key key) {
        return approveKeys(singletonList(key));
    }

    /**
     * Approves a secret key owned by this member. The key is added to the list
     * of valid keys for the member.
     *
     * @param key key to add to the approved list
     * @param level key privilege level
     */
    public void approveKeyBlocking(SecretKeyPair key, Key.Level level) {
        approveKey(key, level).blockingAwait();
    }

    /**
     * Approves a public key owned by this member. The key is added to the list
     * of valid keys for the member.
     *
     * @param key key to add to the approved list
     */
    public void approveKeyBlocking(Key key) {
        approveKey(key).blockingAwait();
    }

    /**
     * Approves public keys owned by this member. The keys are added to the list
     * of valid keys for the member.
     *
     * @param keys keys to add to the approved list
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable approveKeys(List<Key> keys) {
        final List<MemberOperation> operations = new LinkedList<>();
        for (Key key : keys) {
            operations.add(Util.toAddKeyOperation(key));
        }
        return fromObservable(updateKeys(operations));
    }

    /**
     * Approves public keys owned by this member. The keys are added to the list
     * of valid keys for the member.
     *
     * @param keys keys to add to the approved list
     */
    public void approveKeysBlocking(List<Key> keys) {
        approveKeys(keys).blockingAwait();
    }

    /**
     * Removes a public key owned by this member.
     *
     * @param keyId key ID of the key to remove
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable removeKey(String keyId) {
        return removeKeys(singletonList(keyId));
    }

    /**
     * Removes a public key owned by this member.
     *
     * @param keyId key ID of the key to remove
     */
    public void removeKeyBlocking(String keyId) {
        removeKey(keyId).blockingAwait();
    }

    /**
     * Removes public keys owned by this member.
     *
     * @param keyIds key IDs of the keys to remove
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable removeKeys(List<String> keyIds) {
        final List<MemberOperation> operations = new LinkedList<>();
        for (String keyId : keyIds) {
            operations.add(MemberOperation
                    .newBuilder()
                    .setRemoveKey(MemberRemoveKeyOperation
                            .newBuilder()
                            .setKeyId(keyId))
                    .build());
        }
        return fromObservable(updateKeys(operations));
    }

    /**
     * Removes public keys owned by this member.
     *
     * @param keyIds key IDs of the keys to remove
     */
    public void removeKeysBlocking(List<String> keyIds) {
        removeKeys(keyIds).blockingAwait();
    }

    /**
     * Removes all public keys that do not have a corresponding private key stored on
     * the current device from the member.
     *
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable removeNonStoredKeys() {
        final List<Key> storedKeys = client.getCryptoEngine().getPublicKeys();
        return fromObservable(client.getMember(memberId())
                .flatMap(new Function<MemberProtos.Member, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(MemberProtos.Member member) {
                        List<String> toRemoveIds = new LinkedList<>();
                        for (Key key : member.getKeysList()) {
                            if (!storedKeys.contains(key)) {
                                toRemoveIds.add(key.getId());
                            }
                        }
                        return Member.this.removeKeys(toRemoveIds).toObservable();
                    }
                }));
    }

    /**
     * Removes all public keys that do not have a corresponding private key stored on
     * the current device from the member.
     */
    public void removeNonStoredKeysBlocking() {
        removeNonStoredKeys().blockingAwait();
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
     * Replaces the authenticated member's public profile.
     *
     * @param profile Profile to set
     * @return updated profile
     */
    public Profile setProfileBlocking(Profile profile) {
        return setProfile(profile).blockingSingle();
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
     * Gets a member's public profile.
     *
     * @param memberId member ID of member whose profile we want
     * @return profile info
     */
    public Profile getProfileBlocking(String memberId) {
        return getProfile(memberId).blockingSingle();
    }

    /**
     * Replaces auth'd member's public profile picture.
     *
     * @param type MIME type of picture
     * @param data image data
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable setProfilePicture(final String type, byte[] data) {
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
     * Replaces auth'd member's public profile picture.
     *
     * @param type MIME type of picture
     * @param data image data
     */
    public void setProfilePictureBlocking(final String type, byte[] data) {
        setProfilePicture(type, data).blockingAwait();
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
     * Gets a member's public profile picture.
     *
     * @param memberId member ID of member whose profile we want
     * @param size Size category desired (small/medium/large/original)
     * @return blob with picture; empty blob (no fields set) if has no picture
     */
    public Blob getProfilePictureBlocking(String memberId, ProfilePictureSize size) {
        return getProfilePicture(memberId, size).blockingSingle();
    }

    /**
     * Looks up an existing transaction for a given account.
     *
     * @param accountId the account id
     * @param transactionId ID of the transaction
     * @param keyLevel key level
     * @return transaction record
     */
    public Observable<Transaction> getTransaction(
            String accountId,
            String transactionId,
            Key.Level keyLevel) {
        return client.getTransaction(accountId, transactionId, keyLevel);
    }

    /**
     * Looks up an existing transaction for a given account.
     *
     * @param accountId the account id
     * @param transactionId ID of the transaction
     * @param keyLevel key level
     * @return transaction
     */
    public Transaction getTransactionBlocking(
            String accountId,
            String transactionId,
            Key.Level keyLevel) {
        return getTransaction(accountId, transactionId, keyLevel).blockingSingle();
    }

    /**
     * Looks up transactions for a given account.
     *
     * @param accountId the account id
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @param keyLevel key level
     * @return a paged list of transaction records
     */
    public Observable<PagedList<Transaction, String>> getTransactions(
            String accountId,
            @Nullable String offset,
            int limit,
            Key.Level keyLevel) {
        return client.getTransactions(accountId, offset, limit, keyLevel);
    }

    /**
     * Looks up transactions for a given account.
     *
     * @param accountId the account id
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @param keyLevel key level
     * @return paged list of transactions
     */
    public PagedList<Transaction, String> getTransactionsBlocking(
            String accountId,
            @Nullable String offset,
            int limit,
            Key.Level keyLevel) {
        return getTransactions(accountId, offset, limit, keyLevel).blockingSingle();
    }

    /**
     * Looks up account balance.
     *
     * @param accountId the account id
     * @param keyLevel key level
     * @return balance
     */
    public Observable<Balance> getBalance(String accountId, Key.Level keyLevel) {
        return client.getBalance(accountId, keyLevel);
    }

    /**
     * Looks up account balance.
     *
     * @param accountId account id
     * @param keyLevel key level
     * @return balance
     */
    public Balance getBalanceBlocking(String accountId, Key.Level keyLevel) {
        return getBalance(accountId, keyLevel).blockingSingle();
    }

    /**
     * Looks up balances for a list of accounts.
     *
     * @param accountIds list of account ids
     * @param keyLevel key level
     * @return list of balances
     */
    public Observable<List<Balance>> getBalances(
            List<String> accountIds, Key.Level keyLevel) {
        return client.getBalances(accountIds, keyLevel);
    }

    /**
     * Looks up balances for a list of accounts.
     *
     * @param accountIds list of account ids
     * @param keyLevel key level
     * @return list of balances
     */
    public List<Balance> getBalancesBlocking(
            List<String> accountIds,
            Key.Level keyLevel) {
        return getBalances(accountIds, keyLevel).blockingSingle();
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
     * Returns linking information for the specified bank id.
     *
     * @param bankId the bank id
     * @return bank linking information
     */
    public BankInfo getBankInfoBlocking(String bankId) {
        return getBankInfo(bankId).blockingSingle();
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
     * Creates and uploads a blob.
     *
     * @param ownerId id of the owner of the blob
     * @param type MIME type of the file
     * @param name name
     * @param data file data
     * @param accessMode Default or public
     * @return blob Id
     */
    public Attachment createBlobBlocking(
            String ownerId,
            String type,
            String name,
            byte[] data,
            AccessMode accessMode) {
        return createBlob(ownerId, type, name, data, accessMode).blockingSingle();
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
    public Attachment createBlobBlocking(
            String ownerId,
            String type,
            String name,
            byte[] data) {
        return createBlob(ownerId, type, name, data, AccessMode.DEFAULT)
                .blockingSingle();
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
     * Gets a blob from the server.
     *
     * @param blobId blob Id
     * @return Blob
     */
    public Blob getBlobBlocking(String blobId) {
        return getBlob(blobId).blockingSingle();
    }


    /**
     * Delete the member.
     *
     * @return completable
     */
    public Completable deleteMember() {
        return client.deleteMember();
    }


    /**
     * Delete the member.
     */
    public void deleteMemberBlocking()  {
        deleteMember().blockingAwait();
    }

    /**
     * Resolves transfer destinations for the given account ID.
     *
     * @param accountId account ID
     * @return transfer endpoints
     */
    public Observable<List<TransferEndpoint>> resolveTransferDestinations(String accountId) {
        return client.resolveTransferDestinations(accountId);
    }

    /**
     * Resolves transfer destinations for the given account ID.
     *
     * @param accountId account ID
     * @return transfer endpoints
     */
    public List<TransferEndpoint> resolveTransferDestinationsBlocking(String accountId) {
        return resolveTransferDestinations(accountId).blockingSingle();
    }

    /**
     * Get the Token cluster, e.g. sandbox, production.
     *
     * @return Token cluster
     */
    public TokenCluster getTokenCluster() {
        return cluster;
    }

    /**
     * Sets security metadata included in all requests.
     *
     * @param securityMetadata security metadata
     */
    public void setTrackingMetadata(SecurityMetadata securityMetadata) {
        client.setTrackingMetadata(securityMetadata);
    }

    /**
     * Clears security metadata.
     */
    public void clearTrackingMetadata() {
        client.clearTrackingMetadata();
    }

    /**
     * Trigger a step up notification for balance requests.
     *
     * @param accountIds list of account ids
     * @return notification status
     */
    public Observable<NotifyStatus> triggerBalanceStepUpNotification(List<String> accountIds) {
        return client.triggerBalanceStepUpNotification(accountIds);
    }

    /**
     * Trigger a step up notification for balance requests.
     *
     * @param accountIds list of account ids
     * @return notification status
     */
    public NotifyStatus triggerBalanceStepUpNotificationBlocking(List<String> accountIds) {
        return triggerBalanceStepUpNotification(accountIds).blockingSingle();
    }

    @Override
    public int hashCode() {
        return client.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Member)) {
            return false;
        }

        Member other = (Member) obj;
        return member.build().equals(other.member.build())
                && client.equals(other.client);
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }

    private Observable<Builder> updateKeys(final List<MemberOperation> operations) {
        return client
                .getMember(memberId())
                .flatMap(new Function<MemberProtos.Member, Observable<Builder>>() {
                    public Observable<Builder> apply(MemberProtos.Member latest) {
                        return client
                                .updateMember(
                                        member.setLastHash(latest.getLastHash()).build(),
                                        operations)
                                .map(new Function<MemberProtos.Member, Builder>() {
                                    public Builder apply(MemberProtos.Member proto) {
                                        return member.clear().mergeFrom(proto);
                                    }
                                });
                    }
                });
    }
}
