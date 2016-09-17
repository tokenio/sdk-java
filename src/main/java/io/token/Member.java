package io.token;

import io.token.proto.common.member.MemberProtos.Address;
import io.token.proto.common.payment.PaymentProtos.Payment;
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
    public String getMemberId() {
        return async.getMemberId();
    }

    /**
     * @return secret/public keys associated with this member instance
     */
    public SecretKey getKey() {
        return async.getKey();
    }

    /**
     * @return first alias owned by the user
     */
    public String getFirstAlias() {
        return async.getFirstAlias();
    }

    /**
     * @return list of aliases owned by the member
     */
    public List<String> getAliases() {
        return async.getAliases();
    }

    /**
     * @return list of public keys that are approved for this member
     */
    public List<byte[]> getPublicKeys() {
        return async.getPublicKeys();
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
    public void approveKey(byte[] publicKey, int level) {
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
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @param bankId bank id
     * @param accountLinkPayload account link authorization payload generated
     *                           by the bank
     */
    public List<Account> linkAccounts(String bankId, byte[] accountLinkPayload) {
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
    public List<Account> lookupAccounts() {
        return async.lookupAccount()
                .map(l -> l.stream()
                        .map(AccountAsync::sync)
                        .collect(toList()))
                .toBlocking()
                .single();
    }

    /**
     * Looks up an existing token payment.
     *
     * @param paymentId ID of the payment record
     * @return payment record
     */
    public Payment lookupPayment(String paymentId) {
        return async.lookupPayment(paymentId).toBlocking().single();
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
        return async.lookupPayments(offset, limit, tokenId).toBlocking().single();
    }

    /**
     * Creates a new member address
     *
     * @param name the name of the address
     * @param address the address json
     * @return the address record created
     */
    public Address createAddress(String name, String address) {
        return async.createAddress(name, address).toBlocking().single();
    }

    /**
     * Looks up an address by id
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
     * Deletes a member address by its id
     *
     * @param addressId the id of the address
     */
    public void deleteAddress(String addressId) {
        async.deleteAddress(addressId).toBlocking().single();
    }

    /**
     * Sets member preferences
     *
     * @param preferences member json preferences
     */
    public void setPreferences(String preferences) {
        async.setPreferences(preferences).toBlocking().single();
    }

    /**
     * Looks up member preferences
     *
     * @return member preferences
     */
    public String getPreferences() {
        return async.getPreferences().toBlocking().single();
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
