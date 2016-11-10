package io.token.rpc;

import io.token.proto.PagedList;
import io.token.proto.common.account.AccountProtos.Account;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.*;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.subscriber.SubscriberProtos.Platform;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transfer.TransferProtos.TransferPayload;
import io.token.proto.gateway.Gateway.*;
import io.token.proto.gateway.Gateway.ReplaceTokenRequest.CancelToken;
import io.token.proto.gateway.Gateway.ReplaceTokenRequest.CreateToken;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.security.SecretKey;
import io.token.util.codec.ByteEncoding;
import rx.Observable;

import javax.annotation.Nullable;
import java.util.List;

import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.CANCELLED;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.ENDORSED;
import static io.token.rpc.util.Converters.toObservable;
import static io.token.security.Crypto.sign;

/**
 * An authenticated RPC client that is used to talk to Token gateway. The
 * class is a thin wrapper on top of gRPC generated client. Makes the API
 * easier to use.
 */
public final class Client {
    private final String memberId;
    private final SecretKey key;
    private final GatewayServiceFutureStub gateway;
    private String onBehalfOf;

    /**
     * @param memberId member id
     * @param key secret key that is used to sign payload for certain requests.
     * This is generally the same key that is used for
     * authentication.
     * @param gateway gateway gRPC stub
     */
    public Client(String memberId, SecretKey key, GatewayServiceFutureStub gateway) {
        this.memberId = memberId;
        this.key = key;
        this.gateway = gateway;
    }

    /**
     * Sets the On-Behalf-Of authentication value to be used
     * with this client.  The value must correspond to an existing
     * Access Token ID issued for the client member.
     *
     * @param accessTokenId the access token id to be used
     */
    public void useAccessToken(String accessTokenId) {
        this.onBehalfOf = accessTokenId;
    }

    /**
     * Clears the On-Behalf-Of value used with this client.
     */
    public void clearAccessToken() {
        this.onBehalfOf = null;
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
    public Observable<Member> addKey(
            Member member,
            Level level,
            byte[] publicKey) {
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
    public Observable<Member> removeKey(
            Member member,
            String keyId) {
        return updateMember(MemberUpdate.newBuilder()
                .setMemberId(member.getId())
                .setPrevHash(member.getLastHash())
                .setRemoveKey(MemberProtos.MemberRemoveKeyOperation.newBuilder()
                        .setKeyId(keyId))
                .build());
    }

    /**
     * Adds an username for a given user.
     *
     * @param member member to add the key to
     * @param username new unique username to add
     * @return member information
     */
    public Observable<Member> addUsername(
            Member member,
            String username) {
        return updateMember(MemberUpdate.newBuilder()
                .setMemberId(member.getId())
                .setPrevHash(member.getLastHash())
                .setAddUsername(MemberUsernameOperation.newBuilder()
                        .setUsername(username))
                .build());
    }

    /**
     * Removes an existing username for a given user.
     *
     * @param member member to add the key to
     * @param username new unique username to add
     * @return member information
     */
    public Observable<Member> removeUsername(
            Member member,
            String username) {
        return updateMember(MemberUpdate.newBuilder()
                .setMemberId(member.getId())
                .setPrevHash(member.getLastHash())
                .setRemoveUsername(MemberUsernameOperation.newBuilder()
                        .setUsername(username))
                .build());
    }

    /**
     * Creates a subscriber to receive push notifications
     *
     * @param target notification target (e.g. iOS push token)
     * @param platform platform of the device
     * @return subscriber subscriber
     */
    public Observable<Subscriber> subscribeToNotifications(
            String target,
            Platform platform) {
        return toObservable(gateway.subscribeToNotifications(
                SubscribeToNotificationsRequest.newBuilder()
                        .setTarget(target)
                        .setPlatform(platform)
                        .build()))
                .map(SubscribeToNotificationsResponse::getSubscriber);
    }

    /**
     * Gets all subscribers for the member
     *
     * @return subscribers Subscribers
     */
    public Observable<List<Subscriber>> getSubscribers() {
        return toObservable(gateway.getSubscribers(
                GetSubscribersRequest.newBuilder()
                        .build()))
                .map(GetSubscribersResponse::getSubscribersList);
    }

    /**
     * Gets a subscriber by Id
     *
     * @return subscriber Subscriber
     */
    public Observable<Subscriber> getSubscriber(String subscriberId) {
        return toObservable(gateway.getSubscriber(
                GetSubscriberRequest.newBuilder()
                        .setSubscriberId(subscriberId)
                        .build()))
                .map(GetSubscriberResponse::getSubscriber);
    }

    /**
     * Removes a subscriber, to stop receiving notifications
     *
     * @param subscriberId id of the subscriber
     * @return nothing
     */
    public Observable<Void> unsubscribeFromNotifications(
            String subscriberId) {
        return toObservable(gateway.unsubscribeFromNotifications(
                UnsubscribeFromNotificationsRequest.newBuilder()
                        .setSubscriberId(subscriberId)
                        .build()))
                .map(empty -> null);
    }

    /**
     * Links a funding bank account to Token.
     *
     * @param bankId bank id
     * @param accountLinkPayloads a list of account payloads to be linked
     * @return list of linked accounts
     */
    public Observable<List<Account>> linkAccounts(
            String bankId,
            List<String> accountLinkPayloads) {
        return toObservable(gateway
                .linkAccounts(LinkAccountsRequest.newBuilder()
                        .setBankId(bankId)
                        .addAllAccountLinkPayloads(accountLinkPayloads)
                        .build())
        ).map(LinkAccountsResponse::getAccountsList);
    }

    /**
     * Looks up a linked funding account.
     *
     * @param accountId account id
     * @return account info
     */
    public Observable<Account> getAccount(String accountId) {
        setAuthenticationContext();
        return toObservable(gateway.getAccount(GetAccountRequest.newBuilder()
                .setAccountId(accountId)
                .build())
        ).map(GetAccountResponse::getAccount);
    }

    /**
     * Looks up all the linked funding accounts.
     *
     * @return list of linked accounts
     */
    public Observable<List<Account>> getAccounts() {
        setAuthenticationContext();
        return toObservable(gateway.getAccounts(GetAccountsRequest.newBuilder()
                .build())
        ).map(GetAccountsResponse::getAccountsList);
    }

    /**
     * Creates a new token.
     *
     * @param payload transfer token payload
     * @return transfer token returned by the server
     */
    public Observable<Token> createToken(TokenPayload payload) {
        return toObservable(gateway.createToken(CreateTokenRequest.newBuilder()
                .setPayload(payload)
                .build())
        ).map(CreateTokenResponse::getToken);
    }

    /**
     * Looks up a existing token.
     *
     * @param tokenId token id
     * @return token returned by the server
     */
    public Observable<Token> getToken(String tokenId) {
        return toObservable(gateway.getToken(GetTokenRequest.newBuilder()
                .setTokenId(tokenId)
                .build())
        ).map(GetTokenResponse::getToken);
    }

    /**
     * Looks up a list of existing token.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return token returned by the server
     */
    public Observable<PagedList<Token, String>> getTokens(
            GetTokensRequest.Type type,
            @Nullable String offset,
            int limit) {
        return toObservable(gateway.getTokens(GetTokensRequest.newBuilder()
                .setType(type)
                .setPage(pageBuilder(offset, limit))
                .build())
        ).map(res -> PagedList.create(res.getTokensList(), res.getOffset()));
    }

    /**
     * Endorses a token.
     *
     * @param token token to endorse
     * @return result of the endorse operation, returned by the server
     */
    public Observable<TokenOperationResult> endorseToken(Token token) {
        return toObservable(gateway.endorseToken(EndorseTokenRequest.newBuilder()
                .setTokenId(token.getId())
                .setSignature(Signature.newBuilder()
                        .setMemberId(memberId)
                        .setKeyId(key.getId())
                        .setSignature(sign(key, token, ENDORSED)))
                .build())
        ).map(EndorseTokenResponse::getResult);
    }

    /**
     * Cancels a token.
     *
     * @param token token to cancel
     * @return result of the cancel operation, returned by the server
     */
    public Observable<TokenOperationResult> cancelToken(Token token) {
        return toObservable(gateway.cancelToken(CancelTokenRequest.newBuilder()
                .setTokenId(token.getId())
                .setSignature(Signature.newBuilder()
                        .setMemberId(memberId)
                        .setKeyId(key.getId())
                        .setSignature(sign(key, token, CANCELLED)))
                .build())
        ).map(CancelTokenResponse::getResult);
    }

    /**
     * Cancels the existing token, creates a replacement and optionally endorses it.
     * Supported only for access tokens.
     *
     * @param tokenToCancel old token to cancel
     * @param tokenToCreate new token to create
     * @param endorseNewToken endorses a new token if {@code true}
     * @return result of the replacement operation, returned by the server
     */
    public Observable<TokenOperationResult> replaceToken(
            Token tokenToCancel,
            TokenPayload tokenToCreate,
            boolean endorseNewToken) {

        CreateToken.Builder createToken = CreateToken.newBuilder().setPayload(tokenToCreate);
        if (endorseNewToken) {
            createToken.setPayloadSignature(Signature.newBuilder()
                    .setMemberId(memberId)
                    .setKeyId(key.getId())
                    .setSignature(sign(key, tokenToCreate, ENDORSED)));
        }

        return toObservable(gateway.replaceToken(ReplaceTokenRequest.newBuilder()
                .setCancelToken(CancelToken.newBuilder()
                        .setTokenId(tokenToCancel.getId())
                        .setSignature(Signature.newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(key.getId())
                                .setSignature(sign(key, tokenToCancel, CANCELLED))))
                .setCreateToken(createToken)
                .build())
        ).map(ReplaceTokenResponse::getResult);
    }

    /**
     * Looks up account balance.
     *
     * @param accountId account id
     * @return account balance
     */
    public Observable<Money> getBalance(String accountId) {
        setAuthenticationContext();
        return toObservable(gateway.getBalance(GetBalanceRequest.newBuilder()
                .setAccountId(accountId)
                .build())
        ).map(GetBalanceResponse::getCurrent);
    }

    /**
     * Redeems a transfer token.
     *
     * @param transfer transfer parameters, such as amount, currency, etc
     * @return transfer record
     */
    public Observable<Transfer> createTransfer(TransferPayload transfer) {
        return toObservable(gateway.createTransfer(CreateTransferRequest.newBuilder()
                .setPayload(transfer)
                .setPayloadSignature(Signature.newBuilder()
                        .setMemberId(memberId)
                        .setKeyId(key.getId())
                        .setSignature(sign(key, transfer)))
                .build())
        ).map(CreateTransferResponse::getTransfer);
    }

    /**
     * Looks up an existing transfer.
     *
     * @param transferId transfer id
     * @return transfer record
     */
    public Observable<Transfer> getTransfer(String transferId) {
        return toObservable(gateway.getTransfer(GetTransferRequest.newBuilder()
                .setTransferId(transferId)
                .build())
        ).map(GetTransferResponse::getTransfer);
    }

    /**
     * Looks up a list of existing transfers.
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
        GetTransfersRequest.Builder request = GetTransfersRequest.newBuilder()
                .setPage(pageBuilder(offset, limit));

        if (tokenId != null) {
            request.setTokenId(tokenId);
        }

        return toObservable(gateway.getTransfers(request.build()))
                .map(res -> PagedList.create(res.getTransfersList(), res.getOffset()));
    }

    /**
     * Looks up an existing transaction. Doesn't have to be a transaction for a token transfer.
     *
     * @param accountId ID of the account
     * @param transactionId ID of the transaction
     * @return transaction record
     */
    public Observable<Transaction> getTransaction(
            String accountId,
            String transactionId) {
        setAuthenticationContext();
        return toObservable(gateway.getTransaction(GetTransactionRequest.newBuilder()
                .setAccountId(accountId)
                .setTransactionId(transactionId)
                .build())
        ).map(GetTransactionResponse::getTransaction);
    }

    /**
     * Looks up existing transactions. This is a full list of transactions with token transfers
     * being a subset.
     *
     * @param accountId ID of the account
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return transaction record
     */
    public Observable<PagedList<Transaction, String>> getTransactions(
            String accountId,
            @Nullable String offset,
            int limit) {
        setAuthenticationContext();
        return toObservable(gateway.getTransactions(GetTransactionsRequest.newBuilder()
                .setAccountId(accountId)
                .setPage(pageBuilder(offset, limit))
                .build())
        ).map(res -> PagedList.create(res.getTransactionsList(), res.getOffset()));
    }

    /**
     * Adds a new member address.
     *
     * @param name the name of the address
     * @param address the address json
     * @return an address record created
     */
    public Observable<AddressRecord> addAddress(String name, Address address) {
        return toObservable(gateway.addAddress(AddAddressRequest.newBuilder()
                .setName(name)
                .setAddress(address)
                .setAddressSignature(Signature.newBuilder()
                        .setMemberId(memberId)
                        .setKeyId(key.getId())
                        .setSignature(sign(key, address))
                        .build())
                .build())
        ).map(AddAddressResponse::getAddress);
    }

    /**
     * Looks up an address by id
     *
     * @param addressId the address id
     * @return an address record
     */
    public Observable<AddressRecord> getAddress(String addressId) {
        setAuthenticationContext();
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
    public Observable<List<AddressRecord>> getAddresses() {
        setAuthenticationContext();
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

    private Observable<Member> updateMember(MemberUpdate update) {
        return toObservable(gateway.updateMember(UpdateMemberRequest.newBuilder()
                .setUpdate(update)
                .setUpdateSignature(Signature.newBuilder()
                        .setMemberId(memberId)
                        .setKeyId(key.getId())
                        .setSignature(sign(key, update)))
                .build())
        ).map(UpdateMemberResponse::getMember);
    }

    private void setAuthenticationContext() {
        if (onBehalfOf != null) {
            AuthenticationContext.setOnBehalfOf(onBehalfOf);
        }
    }

    private Page.Builder pageBuilder(@Nullable String offset, int limit) {
        Page.Builder page = Page.newBuilder()
                .setLimit(limit);
        if (offset != null) {
            page.setOffset(offset);
        }

        return page;
    }
}
