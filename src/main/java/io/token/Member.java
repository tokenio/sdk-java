package io.token;

import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.*;
import io.token.proto.common.payment.PaymentProtos.*;
import io.token.rpc.Client;
import io.token.security.SecretKey;
import io.token.util.codec.ByteEncoding;
import rx.Observable;

import javax.annotation.Nullable;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;

/**
 * Represents a Member in the Token system. Each member has an active secret
 * and public key pair that is used to perform authentication.
 */
public final class Member {
    private final SecretKey key;
    private final Client client;
    private final MemberProtos.Member.Builder member;

    /**
     * @param member internal member representation, fetched from server
     * @param key secret/public key pair
     * @param client RPC client used to perform operations against the server
     */
    Member(MemberProtos.Member member, SecretKey key, Client client) {
        this.key = key;
        this.client = client;
        this.member = member.toBuilder();
    }

    /**
     * @return a unique ID that identifies the member in the Token system
     */
    public String getMemberId() {
        return member.getId();
    }

    /**
     * @return secret/public keys associated with this member instance
     */
    public SecretKey getKey() {
        return key;
    }

    /**
     * @return first alias owned by the user
     */
    public String getFirstAlias() {
        return member.getAliasesCount() == 0 ? null : member.getAliases(0);
    }

    /**
     * @return list of aliases owned by the member
     */
    public List<String> getAliases() {
        return member.getAliasesList();
    }

    /**
     * @return list of public keys that are approved for this member
     */
    public List<byte[]> getPublicKeys() {
        return member.getKeysBuilderList()
                .stream()
                .map(k -> ByteEncoding.parse(k.getPublicKey()))
                .collect(toList());
    }

    /**
     * Adds a new alias for the member.
     *
     * @param alias alias, e.g. 'john', must be unique
     */
    public void addAlias(String alias) {
        addAliasAsync(alias).toBlocking().single();
    }

    /**
     * Adds a new alias for the member.
     *
     * @param alias alias, e.g. 'john', must be unique
     */
    public Observable<Void> addAliasAsync(String alias) {
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
    public void removeAlias(String alias) {
        removeAliasAsync(alias).toBlocking().single();
    }

    /**
     * Removes an alias for the member.
     *
     * @param alias alias, e.g. 'john'
     */
    public Observable<Void> removeAliasAsync(String alias) {
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
    public void approveKey(byte[] publicKey, int level) {
        approveKeyAsync(publicKey, level).toBlocking().single();
    }

    /**
     * Approves a public key owned by this member. The key is added to the list
     * of valid keys for the member.
     *
     * @param publicKey public key to add to the approved list
     * @param level key security level
     */
    public Observable<Void> approveKeyAsync(byte[] publicKey, int level) {
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
    public void removeKey(String keyId) {
        removeKeyAsync(keyId).toBlocking().single();
    }

    /**
     * Removes a public key owned by this member.
     *
     * @param keyId key ID of the key to remove
     */
    public Observable<Void> removeKeyAsync(String keyId) {
        return client
                .removeKey(member.build(), keyId)
                .map(m -> {
                    member.clear().mergeFrom(m);
                    return null;
                });
    }

    /**
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @param bankId bank id
     * @param accountLinkPayload account link authorization payload generated
     *                           by the bank
     */
    public List<Account> linkAccounts(String bankId, byte[] accountLinkPayload) {
        return linkAccountsAsync(bankId, accountLinkPayload).toBlocking().single();
    }

    /**
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @param bankId bank id
     * @param accountLinkPayload account link authorization payload generated
     *                           by the bank
     */
    public Observable<List<Account>> linkAccountsAsync(String bankId, byte[] accountLinkPayload) {
        return client
                .linkAccounts(bankId, accountLinkPayload)
                .map(accounts -> accounts.stream()
                        .map(a -> new Account(this, a, client))
                        .collect(toList()));
    }

    /**
     * Looks up funding bank accounts linked to Token.
     *
     * @return list of accounts
     */
    public List<Account> lookupAccounts() {
        return lookupAccountAsync().toBlocking().single();
    }

    /**
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @return list of accounts
     */
    public Observable<List<Account>> lookupAccountAsync() {
        return client
                .lookupAccounts()
                .map(accounts -> accounts.stream()
                        .map(a -> new Account(this, a, client))
                        .collect(toList()));
    }

    /**
     * Looks up an existing token payment.
     *
     * @param paymentId ID of the payment record
     * @return payment record
     */
    public Payment lookupPayment(String paymentId) {
        return lookupPaymentAsync(paymentId).toBlocking().single();
    }

    /**
     * Looks up an existing token payment.
     *
     * @param paymentId ID of the payment record
     * @return payment record
     */
    public Observable<Payment> lookupPaymentAsync(String paymentId) {
        return client.lookupPayment(paymentId);
    }

    /**
     * Looks up existing token payments.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @param tokenId optional token id to restrict the search
     * @return payment record
     */
    public List<Payment> lookupPayments(int offset, int limit, @Nullable String tokenId) {
        return lookupPaymentsAsync(offset, limit, tokenId).toBlocking().single();
    }

    /**
     * Looks up existing token payments.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @param tokenId optional token id to restrict the search
     * @return payment record
     */
    public Observable<List<Payment>> lookupPaymentsAsync(int offset, int limit, @Nullable String tokenId) {
        return client.lookupPayments(offset, limit, tokenId);
    }

    /**
     * Creates a new member address
     *
     * @param name the name of the address
     * @param address the address json
     * @return an address record created
     */
    public Observable<Address> createAddressAsync(String name, String address) {
        return client.createAddress(name, address);
    }

    /**
     * Creates a new member address
     *
     * @param name the name of the address
     * @param address the address json
     * @return the address record created
     */
    public Address createAddress(String name, String address) {
        return createAddressAsync(name, address).toBlocking().single();
    }

    /**
     * Looks up an address by id
     *
     * @param addressId the address id
     * @return an address record
     */
    public Observable<Address> getAddressAsync(String addressId) {
        return client.getAddress(addressId);
    }

    /**
     * Looks up an address by id
     *
     * @param addressId the address id
     * @return an address record
     */
    public Address getAddress(String addressId) {
        return getAddressAsync(addressId).toBlocking().single();
    }

    /**
     * Looks up member addresses
     *
     * @return a list of addresses
     */
    public Observable<List<Address>> getAddressesAsync() {
        return client.getAddresses();
    }

    /**
     * Looks up member addresses
     *
     * @return a list of addresses
     */
    public List<Address> getAddresses() {
        return getAddressesAsync().toBlocking().single();
    }

    /**
     * Deletes a member address by its id
     *
     * @param addressId the id of the address
     */
    public Observable<Void> deleteAddressAsync(String addressId) {
        return client.deleteAddress(addressId);
    }

    /**
     * Deletes a member address by its id
     *
     * @param addressId the id of the address
     */
    public void deleteAddress(String addressId) {
        deleteAddressAsync(addressId).toBlocking().single();
    }

    /**
     * Sets member preferences
     *
     * @param preferences member json preferences
     */
    public Observable<Void> setPreferencesAsync(String preferences) {
        return client.setPreferences(preferences);
    }

    /**
     * Sets member preferences
     *
     * @param preferences member json preferences
     */
    public void setPreferences(String preferences) {
        setPreferencesAsync(preferences).toBlocking().single();
    }

    /**
     * Looks up member preferences
     *
     * @return member preferences
     */
    public Observable<String> getPreferencesAsync() {
        return client.getPreferences();
    }

    /**
     * Looks up member preferences
     *
     * @return member preferences
     */
    public String getPreferences() {
        return getPreferencesAsync().toBlocking().single();
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
