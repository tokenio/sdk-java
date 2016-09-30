package io.token.rpc;

import com.google.protobuf.ByteString;
import io.token.proto.common.account.AccountProtos.*;
import io.token.proto.common.device.DeviceProtos;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.*;
import io.token.proto.common.member.MemberProtos.MemberAddKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberAliasOperation;
import io.token.proto.common.member.MemberProtos.MemberUpdate;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.payment.PaymentProtos.Payment;
import io.token.proto.common.payment.PaymentProtos.PaymentPayload;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.token.TokenProtos.*;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.gateway.Gateway.*;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.security.SecretKey;
import io.token.util.codec.ByteEncoding;
import rx.Observable;

import javax.annotation.Nullable;
import java.util.List;

import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.*;
import static io.token.rpc.util.Converters.toObservable;
import static io.token.security.Crypto.sign;

/**
 * An authenticated RPC client that is used to talk to Token gateway. The
 * class is a thin wrapper on top of gRPC generated client. Makes the API
 * easier to use.
 */
public final class Client {
    private final SecretKey key;
    private final GatewayServiceFutureStub gateway;

    /**
     * @param key secret key that is used to sign payload for certain requests.
     *            This is generally the same key that is used for
     *            authentication.
     * @param gateway gateway gRPC stub
     */
    public Client(SecretKey key, GatewayServiceFutureStub gateway) {
        this.key = key;
        this.gateway = gateway;
    }

    /**
     * Looks up member information for the current user. The user is defined by
     * the key used for authentication.
     *
     * @return member information
     */
    public Observable<Member> getMember() {
        return toObservable(gateway.getMember(GetMemberRequest.getDefaultInstance()))
                .map(GetMemberResponse::getMember);
    }

    /**
     * Adds a public key to the list of the approved keys.
     *
     * @param member member to add the key to
     * @param level key level
     * @param publicKey public key to add to the approved list
     * @return member information
     */
    public Observable<Member> addKey(Member member, int level, byte[] publicKey) {
        return updateMember(MemberUpdate.newBuilder()
                .setMemberId(member.getId())
                .setPrevHash(member.getLastHash())
                .setAddKey(MemberAddKeyOperation.newBuilder()
                        .setPublicKey(ByteEncoding.serialize(publicKey))
                        .setLevel(level))
                .build());
    }

    /**
     * Removes a public key from the list of the approved keys.
     *
     * @param member member to remove the key for
     * @param keyId key ID of the key to remove
     * @return member information
     */
    public Observable<Member> removeKey(Member member, String keyId) {
        return updateMember(MemberUpdate.newBuilder()
                .setMemberId(member.getId())
                .setPrevHash(member.getLastHash())
                .setRemoveKey(MemberProtos.MemberRemoveKeyOperation.newBuilder()
                        .setKeyId(keyId))
                .build());
    }

    /**
     * Adds an alias for a given user.
     *
     * @param member member to add the key to
     * @param alias new unique alias to add
     * @return member information
     */
    public Observable<Member> addAlias(Member member, String alias) {
        return updateMember(MemberUpdate.newBuilder()
                .setMemberId(member.getId())
                .setPrevHash(member.getLastHash())
                .setAddAlias(MemberAliasOperation.newBuilder()
                        .setAlias(alias))
                .build());
    }

    /**
     * Removes an existing alias for a given user.
     *
     * @param member member to add the key to
     * @param alias new unique alias to add
     * @return member information
     */
    public Observable<Member> removeAlias(Member member, String alias) {
        return updateMember(MemberUpdate.newBuilder()
                .setMemberId(member.getId())
                .setPrevHash(member.getLastHash())
                .setRemoveAlias(MemberAliasOperation.newBuilder()
                        .setAlias(alias))
                .build());
    }

    /**
     * Subscribes a device to receive push notifications
     *
     * @param provider notification provider (e.g. Token)
     * @param notificationUri uri of the device (e.g. iOS push token)
     * @param platform platform of the device
     * @param tags tags for the device
     * @return nothing
     */
    public Observable<Void> subscribeDevice(String provider, String notificationUri,
                                            DeviceProtos.Platform platform, List<String> tags) {
        return toObservable(gateway.subscribeDevice(SubscribeDeviceRequest.newBuilder()
                .setProvider(provider)
                .setNotificationUri(notificationUri)
                .setPlatform(platform)
                .addAllTags(tags)
                .build()))
                .map(empty -> null);
    }

     /**
     * Unsubscribes a device from push notifications
     *
     * @param provider notification provider (e.g. Token)
     * @param notificationUri uri of the device (e.g. iOS push token)
     * @return nothing
     */
    public Observable<Void> unsubscribeDevice(String provider, String notificationUri) {
        return toObservable(gateway.unsubscribeDevice(UnsubscribeDeviceRequest.newBuilder()
                .setProvider(provider)
                .setNotificationUri(notificationUri)
                .build()))
                .map(empty -> null);
    }

    /**
     * Links a funding bank account to Token.
     *
     * @param bankId bank id
     * @param accountLinkPayload account link authorization payload generated
     *                           by the bank
     * @return list of linked accounts
     */
    public Observable<List<Account>> linkAccounts(String bankId, byte[] accountLinkPayload) {
        return toObservable(gateway.linkAccount(LinkAccountRequest.newBuilder()
                .setBankId(bankId)
                .setAccountLinkPayload(ByteString.copyFrom(accountLinkPayload))
                .build())
        ).map(LinkAccountResponse::getAccountsList);
    }

    /**
     * Looks up a linked funding account.
     *
     * @param accountId account id
     * @return account info
     */
    public Observable<Account> lookupAccount(String accountId) {
        return toObservable(gateway.lookupAccount(LookupAccountRequest.newBuilder()
                .setAccountId(accountId)
                .build())
        ).map(LookupAccountResponse::getAccount);
    }

    /**
     * Looks up all the linked funding accounts.
     *
     * @return list of linked accounts
     */
    public Observable<List<Account>> lookupAccounts() {
        return toObservable(gateway.lookupAccounts(LookupAccountsRequest.newBuilder()
                .build())
        ).map(LookupAccountsResponse::getAccountsList);
    }

    /**
     * Sets account name.
     *
     * @param accountId account id
     * @param accountName new name to use
     * @return updated account info
     */
    public Observable<Account> setAccountName(String accountId, String accountName) {
        return toObservable(gateway.setAccountName(SetAccountNameRequest.newBuilder()
                .setAccountId(accountId)
                .setName(accountName)
                .build())
        ).map(SetAccountNameResponse::getAccount);
    }

    /**
     * Creates a new payment token.
     *
     * @param token payment token
     * @return payment token returned by the server
     */
    public Observable<Token> createToken(PaymentToken token) {
        return toObservable(gateway.createPaymentToken(CreatePaymentTokenRequest.newBuilder()
                .setToken(token)
                .build())
        ).map(CreatePaymentTokenResponse::getToken);
    }

    /**
     * Creates a new information token.
     *
     * @param token information token
     * @return the token returned by the server
     */
    public Observable<Token> createToken(InformationToken token) {
        return toObservable(gateway.createInformationToken(CreateInformationTokenRequest.newBuilder()
                .setToken(token)
                .build())
        ).map(CreateInformationTokenResponse::getToken);
    }

    /**
     * Looks up a existing token.
     *
     * @param tokenId token id
     * @return token returned by the server
     */
    public Observable<Token> lookupToken(String tokenId) {
        return toObservable(gateway.lookupToken(LookupTokenRequest.newBuilder()
                .setTokenId(tokenId)
                .build())
        ).map(LookupTokenResponse::getToken);
    }

    /**
     * Looks up a list of existing token.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return token returned by the server
     */
    public Observable<List<Token>> lookupTokens(int offset, int limit) {
        return toObservable(gateway.lookupTokens(LookupTokensRequest.newBuilder()
                .setOffset(offset)
                .setLimit(limit)
                .build())
        ).map(LookupTokensResponse::getTokensList);
    }

    /**
     * Endorses a token.
     *
     * @param token token to endorse
     * @return endorsed token returned by the server
     */
    public Observable<Token> endorseToken(Token token) {
        return toObservable(gateway.endorseToken(EndorseTokenRequest.newBuilder()
                .setTokenId(token.getId())
                .setSignature(Signature.newBuilder()
                        .setKeyId(key.getId())
                        .setSignature(sign(key, token, ENDORSED)))
                .build())
        ).map(EndorseTokenResponse::getToken);
    }

    /**
     * Declines a token.
     *
     * @param token token to decline
     * @return declined token returned by the server
     */
    public Observable<Token> declineToken(Token token) {
        return toObservable(gateway.declineToken(DeclineTokenRequest.newBuilder()
                .setTokenId(token.getId())
                .setSignature(Signature.newBuilder()
                        .setKeyId(key.getId())
                        .setSignature(sign(key, token, DECLINED)))
                .build())
        ).map(DeclineTokenResponse::getToken);
    }

    /**
     * Revokes a token.
     *
     * @param token token to revoke
     * @return revoked token returned by the server
     */
    public Observable<Token> revokeToken(Token token) {
        return toObservable(gateway.revokeToken(RevokeTokenRequest.newBuilder()
                .setTokenId(token.getId())
                .setSignature(Signature.newBuilder()
                        .setKeyId(key.getId())
                        .setSignature(sign(key, token, REVOKED)))
                .build())
        ).map(RevokeTokenResponse::getToken);
    }

    /**
     * Redeems a payment token.
     *
     * @param payment payment parameters, such as amount, currency, etc
     * @return payment record
     */
    public Observable<Payment> redeemToken(PaymentPayload payment) {
        return toObservable(gateway.redeemPaymentToken(RedeemPaymentTokenRequest.newBuilder()
                .setPayload(payment)
                .setSignature(Signature.newBuilder()
                        .setKeyId(key.getId())
                        .setSignature(sign(key, payment)))
                .build())
        ).map(RedeemPaymentTokenResponse::getPayment);
    }

    /**
     * Looks up account balance.
     *
     * @param accountId account id
     * @return account balance
     */
    public Observable<Money> lookupBalance(String accountId) {
        return toObservable(gateway.lookupBalance(LookupBalanceRequest.newBuilder()
                .setAccountId(accountId)
                .build())
        ).map(LookupBalanceResponse::getCurrent);
    }

    /**
     * Looks up an existing payment.
     *
     * @param paymentId payment id
     * @return payment record
     */
    public Observable<Payment> lookupPayment(String paymentId) {
        return toObservable(gateway.lookupPayment(LookupPaymentRequest.newBuilder()
                .setPaymentId(paymentId)
                .build())
        ).map(LookupPaymentResponse::getPayment);
    }

    /**
     * Looks up a list of existing payments.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @param tokenId optional token id to restrict the search
     * @return payment record
     */
    public Observable<List<Payment>> lookupPayments(int offset, int limit, @Nullable String tokenId) {
        LookupPaymentsRequest.Builder request = LookupPaymentsRequest.newBuilder()
                .setOffset(offset)
                .setLimit(limit);

        if (tokenId != null) {
            request.setTokenId(tokenId);
        }

        return toObservable(gateway.lookupPayments(request.build()))
                .map(LookupPaymentsResponse::getPaymentsList);
    }

    /**
     * Looks up an existing transaction. Doesn't have to be a transaction for a token payment.
     *
     * @param accountId ID of the account
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Observable<Transaction> lookupTransaction(String accountId, String transactionId) {
        return toObservable(gateway.lookupTransaction(LookupTransactionRequest.newBuilder()
                .setAccountId(accountId)
                .setTransactionId(transactionId)
                .build())
        ).map(LookupTransactionResponse::getTransaction);
    }

    /**
     * Looks up existing transactions. This is a full list of transactions with token payments
     * being a subset.
     *
     * @param accountId ID of the account
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return transaction record
     */
    public Observable<List<Transaction>> lookupTransactions(String accountId, int offset, int limit) {
        return toObservable(gateway.lookupTransactions(LookupTransactionsRequest.newBuilder()
                .setAccountId(accountId)
                .setOffset(offset)
                .setLimit(limit)
                .build())
        ).map(LookupTransactionsResponse::getTransactionsList);
    }

    /**
     * Creates a new member address
     *
     * @param name the name of the address
     * @param address the address json
     * @return an address record created
     */
    public Observable<Address> createAddress(String name, String address) {
        return toObservable(gateway.createAddress(CreateAddressRequest.newBuilder()
                .setName(name)
                .setData(address)
                .setSignature(Signature.newBuilder()
                        .setKeyId(key.getId())
                        .setSignature(sign(key, address))
                        .build())
                .build())
        ).map(CreateAddressResponse::getAddress);
    }

    /**
     * Looks up an address by id
     *
     * @param addressId the address id
     * @return an address record
     */
    public Observable<Address> getAddress(String addressId) {
        return toObservable(gateway.getAddress(GetAddressRequest.newBuilder()
                .setAddressId(addressId)
                .build())
        ).map(GetAddressResponse::getAddress);
    }

    /**
     * Looks up member addresses
     *
     * @return a list of addresses
     */
    public Observable<List<Address>> getAddresses() {
        return toObservable(gateway.getAddresses(GetAddressesRequest.newBuilder()
                .build())
        ).map(GetAddressesResponse::getAddressesList);
    }

    /**
     * Deletes a member address by its id
     *
     * @param addressId the id of the address
     */
    public Observable<Void> deleteAddress(String addressId) {
        return toObservable(gateway.deleteAddress(DeleteAddressRequest.newBuilder()
                .setAddressId(addressId)
                .build())
        ).map(empty -> null);
    }

    /**
     * Sets member preferences
     *
     * @param preferences member json preferences
     */
    public Observable<Void> setPreferences(String preferences) {
        return toObservable(gateway.setPreference(SetPreferenceRequest.newBuilder()
                .setPreference(preferences)
                .build())
        ).map(empty -> null);
    }

    /**
     * Looks up member preferences
     *
     * @return member preferences
     */
    public Observable<String> getPreferences() {
        return toObservable(gateway.getPreference(GetPreferenceRequest.newBuilder()
                .build())
        ).map(GetPreferenceResponse::getPreference);
    }

    private Observable<Member> updateMember(MemberUpdate update) {
        return toObservable(gateway.updateMember(UpdateMemberRequest.newBuilder()
                .setUpdate(update)
                .setSignature(Signature.newBuilder()
                        .setKeyId(key.getId())
                        .setSignature(sign(key, update)))
                .build())
        ).map(UpdateMemberResponse::getMember);
    }
}
