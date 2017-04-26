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

package io.token.rpc;

import static io.token.proto.ProtoJson.toJson;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.CANCELLED;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.ENDORSED;
import static io.token.util.Util.toObservable;

import io.token.proto.PagedList;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.account.AccountProtos.Account;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.bank.BankProtos.Bank;
import io.token.proto.common.bank.BankProtos.BankInfo;
import io.token.proto.common.member.MemberProtos.AddressRecord;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberUpdate;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenSignature.Action;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transfer.TransferProtos.TransferPayload;
import io.token.proto.gateway.Gateway.AddAddressRequest;
import io.token.proto.gateway.Gateway.AddAddressResponse;
import io.token.proto.gateway.Gateway.CancelTokenRequest;
import io.token.proto.gateway.Gateway.CancelTokenResponse;
import io.token.proto.gateway.Gateway.CreateTestBankAccountRequest;
import io.token.proto.gateway.Gateway.CreateTestBankAccountResponse;
import io.token.proto.gateway.Gateway.CreateTokenRequest;
import io.token.proto.gateway.Gateway.CreateTokenResponse;
import io.token.proto.gateway.Gateway.CreateTransferRequest;
import io.token.proto.gateway.Gateway.CreateTransferResponse;
import io.token.proto.gateway.Gateway.DeleteAddressRequest;
import io.token.proto.gateway.Gateway.DeleteAddressResponse;
import io.token.proto.gateway.Gateway.EndorseTokenRequest;
import io.token.proto.gateway.Gateway.EndorseTokenResponse;
import io.token.proto.gateway.Gateway.GetAccountRequest;
import io.token.proto.gateway.Gateway.GetAccountResponse;
import io.token.proto.gateway.Gateway.GetAccountsRequest;
import io.token.proto.gateway.Gateway.GetAccountsResponse;
import io.token.proto.gateway.Gateway.GetAddressRequest;
import io.token.proto.gateway.Gateway.GetAddressResponse;
import io.token.proto.gateway.Gateway.GetAddressesRequest;
import io.token.proto.gateway.Gateway.GetAddressesResponse;
import io.token.proto.gateway.Gateway.GetBalanceRequest;
import io.token.proto.gateway.Gateway.GetBalanceResponse;
import io.token.proto.gateway.Gateway.GetBankInfoRequest;
import io.token.proto.gateway.Gateway.GetBankInfoResponse;
import io.token.proto.gateway.Gateway.GetBanksRequest;
import io.token.proto.gateway.Gateway.GetBanksResponse;
import io.token.proto.gateway.Gateway.GetMemberRequest;
import io.token.proto.gateway.Gateway.GetMemberResponse;
import io.token.proto.gateway.Gateway.GetNotificationRequest;
import io.token.proto.gateway.Gateway.GetNotificationResponse;
import io.token.proto.gateway.Gateway.GetNotificationsRequest;
import io.token.proto.gateway.Gateway.GetNotificationsResponse;
import io.token.proto.gateway.Gateway.GetSubscriberRequest;
import io.token.proto.gateway.Gateway.GetSubscriberResponse;
import io.token.proto.gateway.Gateway.GetSubscribersRequest;
import io.token.proto.gateway.Gateway.GetSubscribersResponse;
import io.token.proto.gateway.Gateway.GetTokenRequest;
import io.token.proto.gateway.Gateway.GetTokenResponse;
import io.token.proto.gateway.Gateway.GetTokensRequest;
import io.token.proto.gateway.Gateway.GetTokensResponse;
import io.token.proto.gateway.Gateway.GetTransactionRequest;
import io.token.proto.gateway.Gateway.GetTransactionResponse;
import io.token.proto.gateway.Gateway.GetTransactionsRequest;
import io.token.proto.gateway.Gateway.GetTransactionsResponse;
import io.token.proto.gateway.Gateway.GetTransferRequest;
import io.token.proto.gateway.Gateway.GetTransferResponse;
import io.token.proto.gateway.Gateway.GetTransfersRequest;
import io.token.proto.gateway.Gateway.GetTransfersResponse;
import io.token.proto.gateway.Gateway.LinkAccountsRequest;
import io.token.proto.gateway.Gateway.LinkAccountsResponse;
import io.token.proto.gateway.Gateway.Page;
import io.token.proto.gateway.Gateway.ReplaceTokenRequest;
import io.token.proto.gateway.Gateway.ReplaceTokenRequest.CancelToken;
import io.token.proto.gateway.Gateway.ReplaceTokenRequest.CreateToken;
import io.token.proto.gateway.Gateway.ReplaceTokenResponse;
import io.token.proto.gateway.Gateway.SubscribeToNotificationsRequest;
import io.token.proto.gateway.Gateway.SubscribeToNotificationsResponse;
import io.token.proto.gateway.Gateway.UnlinkAccountsRequest;
import io.token.proto.gateway.Gateway.UnlinkAccountsResponse;
import io.token.proto.gateway.Gateway.UnsubscribeFromNotificationsRequest;
import io.token.proto.gateway.Gateway.UnsubscribeFromNotificationsResponse;
import io.token.proto.gateway.Gateway.UpdateMemberRequest;
import io.token.proto.gateway.Gateway.UpdateMemberResponse;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.security.CryptoEngine;
import io.token.security.Signer;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import rx.Observable;
import rx.functions.Func1;

/**
 * An authenticated RPC client that is used to talk to Token gateway. The
 * class is a thin wrapper on top of gRPC generated client. Makes the API
 * easier to use.
 */
public final class Client {
    private final String memberId;
    private final CryptoEngine crypto;
    private final GatewayServiceFutureStub gateway;
    private String onBehalfOf;

    /**
     * This is generally the same key that is used for authentication.
     *
     * @param memberId member id
     * @param crypto the crypto engine used to sign for authentication, request payloads, etc
     * @param gateway gateway gRPC stub
     */
    public Client(String memberId, CryptoEngine crypto, GatewayServiceFutureStub gateway) {
        this.memberId = memberId;
        this.crypto = crypto;
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
                .map(new Func1<GetMemberResponse, Member>() {
                    public Member call(GetMemberResponse response) {
                        return response.getMember();
                    }
                });
    }

    /**
     * Updates member by applying the specified operations.
     *
     * @param member member to update
     * @param operations operations to apply
     * @return member information
     */
    public Observable<Member> updateMember(Member member, List<MemberOperation> operations) {
        Signer signer = crypto.createSigner(Key.Level.PRIVILEGED);

        MemberUpdate update = MemberUpdate
                .newBuilder()
                .setMemberId(member.getId())
                .setPrevHash(member.getLastHash())
                .addAllOperations(operations)
                .build();

        return toObservable(gateway
                .updateMember(UpdateMemberRequest
                        .newBuilder()
                        .setUpdate(update)
                        .setUpdateSignature(Signature
                                .newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(update)))
                        .build()))
                .map(new Func1<UpdateMemberResponse, Member>() {
                    public Member call(UpdateMemberResponse response) {
                        return response.getMember();
                    }
                });
    }

    /**
     * Creates a subscriber to receive push notifications.
     *
     * @param handler specify the handler of the notifications
     * @param handlerInstructions map of instructions for the handler
     * @return notification subscriber
     */
    public Observable<Subscriber> subscribeToNotifications(
            String handler,
            Map<String, String> handlerInstructions) {
        SubscribeToNotificationsRequest request = SubscribeToNotificationsRequest
                .newBuilder()
                .setHandler(handler)
                .putAllHandlerInstructions(handlerInstructions)
                .build();
        return toObservable(gateway
                .subscribeToNotifications(request))
                .map(new Func1<SubscribeToNotificationsResponse, Subscriber>() {
                    public Subscriber call(SubscribeToNotificationsResponse response) {
                        return response.getSubscriber();
                    }
                });
    }

    /**
     * Gets all subscribers for the member.
     *
     * @return list of notification subscribers
     */
    public Observable<List<Subscriber>> getSubscribers() {
        return toObservable(gateway
                .getSubscribers(GetSubscribersRequest
                        .newBuilder()
                        .build()))
                .map(new Func1<GetSubscribersResponse, List<Subscriber>>() {
                    @Override
                    public List<Subscriber> call(GetSubscribersResponse response) {
                        return response.getSubscribersList();
                    }
                });
    }

    /**
     * Gets a subscriber by Id.
     *
     * @param subscriberId subscriber id
     * @return notification subscriber
     */
    public Observable<Subscriber> getSubscriber(String subscriberId) {
        return toObservable(gateway
                .getSubscriber(GetSubscriberRequest
                        .newBuilder()
                        .setSubscriberId(subscriberId)
                        .build()))
                .map(new Func1<GetSubscriberResponse, Subscriber>() {
                    public Subscriber call(GetSubscriberResponse response) {
                        return response.getSubscriber();
                    }
                });
    }

    /**
     * Removes a subscriber, to stop receiving notifications.
     *
     * @param subscriberId id of the subscriber
     * @return nothing
     */
    public Observable<Void> unsubscribeFromNotifications(
            String subscriberId) {
        return toObservable(gateway
                .unsubscribeFromNotifications(UnsubscribeFromNotificationsRequest
                        .newBuilder()
                        .setSubscriberId(subscriberId)
                        .build()))
                .map(new Func1<UnsubscribeFromNotificationsResponse, Void>() {
                    public Void call(UnsubscribeFromNotificationsResponse response) {
                        return null;
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
        return toObservable(gateway.getNotifications(
                GetNotificationsRequest
                        .newBuilder()
                        .setPage(pageBuilder(offset, limit))
                        .build()))
                .map(new Func1<GetNotificationsResponse, PagedList<Notification, String>>() {
                    public PagedList<Notification, String> call(GetNotificationsResponse response) {
                        return PagedList.create(
                                response.getNotificationsList(),
                                response.getOffset());
                    }
                });
    }

    /**
     * Gets a notification.
     *
     * @param notificationId id of the notification
     * @return notification
     */
    public Observable<Notification> getNotification(String notificationId) {
        return toObservable(gateway
                .getNotification(GetNotificationRequest
                        .newBuilder()
                        .setNotificationId(notificationId)
                        .build()))
                .map(new Func1<GetNotificationResponse, Notification>() {
                    public Notification call(GetNotificationResponse response) {
                        return response.getNotification();
                    }
                });
    }

    /**
     * Links a funding bank account to Token.
     *
     * @param authorization an authorization to accounts, from the bank
     * @return list of linked accounts
     */
    public Observable<List<Account>> linkAccounts(
            BankAuthorization authorization) {
        return toObservable(gateway
                .linkAccounts(LinkAccountsRequest
                        .newBuilder()
                        .setBankAuthorization(authorization)
                        .build()))
                .map(new Func1<LinkAccountsResponse, List<Account>>() {
                    public List<Account> call(LinkAccountsResponse response) {
                        return response.getAccountsList();
                    }
                });
    }

    /**
     * Unlinks token accounts.
     *
     * @param accountIds account ids to unlink
     * @return nothing
     */
    public Observable<Void> unlinkAccounts(List<String> accountIds) {
        return toObservable(gateway.unlinkAccounts(
                UnlinkAccountsRequest.newBuilder()
                        .addAllAccountIds(accountIds)
                        .build()))
                .map(new Func1<UnlinkAccountsResponse, Void>() {
                    public Void call(UnlinkAccountsResponse response) {
                        return null;
                    }
                });
    }

    /**
     * Looks up a linked funding account.
     *
     * @param accountId account id
     * @return account info
     */
    public Observable<Account> getAccount(String accountId) {
        setAuthenticationContext();
        return toObservable(gateway
                .getAccount(GetAccountRequest
                        .newBuilder()
                        .setAccountId(accountId)
                        .build()))
                .map(new Func1<GetAccountResponse, Account>() {
                    public Account call(GetAccountResponse response) {
                        return response.getAccount();
                    }
                });
    }

    /**
     * Looks up all the linked funding accounts.
     *
     * @return list of linked accounts
     */
    public Observable<List<Account>> getAccounts() {
        setAuthenticationContext();
        return toObservable(gateway
                .getAccounts(GetAccountsRequest
                        .newBuilder()
                        .build()))
                .map(new Func1<GetAccountsResponse, List<Account>>() {
                    public List<Account> call(GetAccountsResponse response) {
                        return response.getAccountsList();
                    }
                });
    }

    /**
     * Creates a new token.
     *
     * @param payload transfer token payload
     * @return transfer token returned by the server
     */
    public Observable<Token> createToken(TokenPayload payload) {
        return toObservable(gateway
                .createToken(CreateTokenRequest
                        .newBuilder()
                        .setPayload(payload)
                        .build()))
                .map(new Func1<CreateTokenResponse, Token>() {
                    public Token call(CreateTokenResponse response) {
                        return response.getToken();
                    }
                });
    }

    /**
     * Looks up a existing token.
     *
     * @param tokenId token id
     * @return token returned by the server
     */
    public Observable<Token> getToken(String tokenId) {
        return toObservable(gateway
                .getToken(GetTokenRequest
                        .newBuilder()
                        .setTokenId(tokenId)
                        .build()))
                .map(new Func1<GetTokenResponse, Token>() {
                    public Token call(GetTokenResponse response) {
                        return response.getToken();
                    }
                });
    }

    /**
     * Looks up a list of existing token.
     *
     * @param type token type
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return token returned by the server
     */
    public Observable<PagedList<Token, String>> getTokens(
            GetTokensRequest.Type type,
            @Nullable String offset,
            int limit) {
        return toObservable(gateway
                .getTokens(GetTokensRequest
                        .newBuilder()
                        .setType(type)
                        .setPage(pageBuilder(offset, limit))
                        .build()))
                .map(new Func1<GetTokensResponse, PagedList<Token, String>>() {
                    public PagedList<Token, String> call(GetTokensResponse response) {
                        return PagedList.create(response.getTokensList(), response.getOffset());
                    }
                });
    }

    /**
     * Endorses a token.
     *
     * @param token token to endorse
     * @param keyLevel key level to be used to endorse the token
     * @return result of the endorse operation, returned by the server
     */
    public Observable<TokenOperationResult> endorseToken(Token token, Key.Level keyLevel) {
        Signer signer = crypto.createSigner(keyLevel);
        return toObservable(gateway
                .endorseToken(EndorseTokenRequest
                        .newBuilder()
                        .setTokenId(token.getId())
                        .setSignature(Signature
                                .newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(tokenAction(token, ENDORSED))))
                        .build()))
                .map(new Func1<EndorseTokenResponse, TokenOperationResult>() {
                    @Override
                    public TokenOperationResult call(EndorseTokenResponse response) {
                        return response.getResult();
                    }
                });
    }

    /**
     * Cancels a token.
     *
     * @param token token to cancel
     * @return result of the cancel operation, returned by the server
     */
    public Observable<TokenOperationResult> cancelToken(Token token) {
        Signer signer = crypto.createSigner(Key.Level.LOW);
        return toObservable(gateway
                .cancelToken(CancelTokenRequest
                        .newBuilder()
                        .setTokenId(token.getId())
                        .setSignature(Signature
                                .newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(tokenAction(token, CANCELLED))))
                        .build()))
                .map(new Func1<CancelTokenResponse, TokenOperationResult>() {
                    public TokenOperationResult call(CancelTokenResponse response) {
                        return response.getResult();
                    }
                });
    }

    /**
     * Cancels the existing token and creates a replacement for it.
     * Supported only for access tokens.
     *
     * @param tokenToCancel old token to cancel
     * @param tokenToCreate new token to create
     * @return result of the replacement operation, returned by the server
     */
    public Observable<TokenOperationResult> replace(
            Token tokenToCancel,
            TokenPayload tokenToCreate) {
        return cancelAndReplace(tokenToCancel, CreateToken.newBuilder().setPayload(tokenToCreate));
    }

    /**
     * Cancels the existing token, creates a replacement and endorses it.
     * Supported only for access tokens.
     *
     * @param tokenToCancel old token to cancel
     * @param tokenToCreate new token to create
     * @return result of the replacement operation, returned by the server
     */
    public Observable<TokenOperationResult> replaceAndEndorseToken(
            Token tokenToCancel,
            TokenPayload tokenToCreate) {
        Signer signer = crypto.createSigner(Key.Level.STANDARD);
        CreateToken.Builder createToken = CreateToken.newBuilder().setPayload(tokenToCreate);
        createToken.setPayloadSignature(Signature.newBuilder()
                .setMemberId(memberId)
                .setKeyId(signer.getKeyId())
                .setSignature(signer.sign(tokenAction(tokenToCreate, ENDORSED))));
        return cancelAndReplace(tokenToCancel, createToken);
    }

    /**
     * Looks up account balance.
     *
     * @param accountId account id
     * @return account balance
     */
    public Observable<Money> getBalance(String accountId) {
        setAuthenticationContext();
        return toObservable(gateway
                .getBalance(GetBalanceRequest
                        .newBuilder()
                        .setAccountId(accountId)
                        .build()))
                .map(new Func1<GetBalanceResponse, Money>() {
                    public Money call(GetBalanceResponse response) {
                        return response.getCurrent();
                    }
                });
    }

    /**
     * Redeems a transfer token.
     *
     * @param transfer transfer parameters, such as amount, currency, etc
     * @return transfer record
     */
    public Observable<Transfer> createTransfer(TransferPayload transfer) {
        Signer signer = crypto.createSigner(Key.Level.LOW);
        return toObservable(gateway
                .createTransfer(CreateTransferRequest
                        .newBuilder()
                        .setPayload(transfer)
                        .setPayloadSignature(Signature
                                .newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(transfer)))
                        .build()))
                .map(new Func1<CreateTransferResponse, Transfer>() {
                    public Transfer call(CreateTransferResponse response) {
                        return response.getTransfer();
                    }
                });
    }

    /**
     * Looks up an existing transfer.
     *
     * @param transferId transfer id
     * @return transfer record
     */
    public Observable<Transfer> getTransfer(String transferId) {
        return toObservable(gateway
                .getTransfer(GetTransferRequest
                        .newBuilder()
                        .setTransferId(transferId)
                        .build()))
                .map(new Func1<GetTransferResponse, Transfer>() {
                    public Transfer call(GetTransferResponse response) {
                        return response.getTransfer();
                    }
                });
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
        GetTransfersRequest.Builder request = GetTransfersRequest
                .newBuilder()
                .setPage(pageBuilder(offset, limit));

        if (tokenId != null) {
            request.setTokenId(tokenId);
        }

        return toObservable(gateway.getTransfers(request.build()))
                .map(new Func1<GetTransfersResponse, PagedList<Transfer, String>>() {
                    public PagedList<Transfer, String> call(GetTransfersResponse response) {
                        return PagedList.create(response.getTransfersList(), response.getOffset());
                    }
                });
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
        return toObservable(gateway
                .getTransaction(GetTransactionRequest
                        .newBuilder()
                        .setAccountId(accountId)
                        .setTransactionId(transactionId)
                        .build()))
                .map(new Func1<GetTransactionResponse, Transaction>() {
                    public Transaction call(GetTransactionResponse response) {
                        return response.getTransaction();
                    }
                });
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
        return toObservable(gateway
                .getTransactions(GetTransactionsRequest
                        .newBuilder()
                        .setAccountId(accountId)
                        .setPage(pageBuilder(offset, limit))
                        .build()))
                .map(new Func1<GetTransactionsResponse, PagedList<Transaction, String>>() {
                    public PagedList<Transaction, String> call(GetTransactionsResponse response) {
                        return PagedList.create(
                                response.getTransactionsList(),
                                response.getOffset());
                    }
                });
    }

    /**
     * Adds a new member address.
     *
     * @param name the name of the address
     * @param address the address json
     * @return an address record created
     */
    public Observable<AddressRecord> addAddress(String name, Address address) {
        Signer signer = crypto.createSigner(Key.Level.LOW);
        return toObservable(gateway
                .addAddress(AddAddressRequest
                        .newBuilder()
                        .setName(name)
                        .setAddress(address)
                        .setAddressSignature(Signature
                                .newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(address))
                                .build())
                        .build()))
                .map(new Func1<AddAddressResponse, AddressRecord>() {
                    public AddressRecord call(AddAddressResponse response) {
                        return response.getAddress();
                    }
                });
    }

    /**
     * Looks up an address by id.
     *
     * @param addressId the address id
     * @return an address record
     */
    public Observable<AddressRecord> getAddress(String addressId) {
        setAuthenticationContext();
        return toObservable(gateway
                .getAddress(GetAddressRequest
                        .newBuilder()
                        .setAddressId(addressId)
                        .build()))
                .map(new Func1<GetAddressResponse, AddressRecord>() {
                    public AddressRecord call(GetAddressResponse response) {
                        return response.getAddress();
                    }
                });
    }

    /**
     * Looks up member addresses.
     *
     * @return a list of addresses
     */
    public Observable<List<AddressRecord>> getAddresses() {
        setAuthenticationContext();
        return toObservable(gateway
                .getAddresses(GetAddressesRequest
                        .newBuilder()
                        .build()))
                .map(new Func1<GetAddressesResponse, List<AddressRecord>>() {
                    public List<AddressRecord> call(GetAddressesResponse response) {
                        return response.getAddressesList();
                    }
                });
    }

    /**
     * Deletes a member address by its id.
     *
     * @param addressId the id of the address
     * @return observable that completes when request
     */
    public Observable<Void> deleteAddress(String addressId) {
        return toObservable(gateway
                .deleteAddress(DeleteAddressRequest
                        .newBuilder()
                        .setAddressId(addressId)
                        .build())
        ).map(new Func1<DeleteAddressResponse, Void>() {
            public Void call(DeleteAddressResponse response) {
                return null;
            }
        });
    }

    /**
     * Returns a list of all token enabled banks.
     *
     * @return a list of banks
     */
    public Observable<List<Bank>> getBanks() {
        return toObservable(gateway
                .getBanks(GetBanksRequest
                        .newBuilder()
                        .build()))
                .map(new Func1<GetBanksResponse, List<Bank>>() {
                    public List<Bank> call(GetBanksResponse response) {
                        return response.getBanksList();
                    }
                });
    }

    /**
     * Returns linking information for the specified bank id.
     *
     * @param bankId the bank id
     * @return bank linking information
     */
    public Observable<BankInfo> getBankInfo(String bankId) {
        return toObservable(gateway
                .getBankInfo(GetBankInfoRequest
                        .newBuilder()
                        .setBankId(bankId)
                        .build()))
                .map(new Func1<GetBankInfoResponse, BankInfo>() {
                    public BankInfo call(GetBankInfoResponse response) {
                        return response.getInfo();
                    }
                });
    }

    /**
     * Creates a test bank account and generates bank authorization.
     *
     * @param balance account balance to set
     * @return bank authorization
     */
    public Observable<BankAuthorization> createTestBankAccount(Money balance) {
        return toObservable(gateway
                .createTestBankAccount(CreateTestBankAccountRequest
                        .newBuilder()
                        .setBalance(balance)
                        .build()))
                .map(new Func1<CreateTestBankAccountResponse, BankAuthorization>() {
                    public BankAuthorization call(CreateTestBankAccountResponse response) {
                        return response.getBankAuthorization();
                    }
                });
    }

    private Observable<TokenOperationResult> cancelAndReplace(
            Token tokenToCancel,
            CreateToken.Builder createToken) {
        Signer signer = crypto.createSigner(Key.Level.LOW);
        return toObservable(gateway
                .replaceToken(ReplaceTokenRequest
                        .newBuilder()
                        .setCancelToken(CancelToken
                                .newBuilder()
                                .setTokenId(tokenToCancel.getId())
                                .setSignature(Signature
                                        .newBuilder()
                                        .setMemberId(memberId)
                                        .setKeyId(signer.getKeyId())
                                        .setSignature(signer
                                                .sign(tokenAction(tokenToCancel, CANCELLED)))))
                        .setCreateToken(createToken)
                        .build()))
                .map(new Func1<ReplaceTokenResponse, TokenOperationResult>() {
                    public TokenOperationResult call(ReplaceTokenResponse response) {
                        return response.getResult();
                    }
                });
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

    private String tokenAction(Token token, Action action) {
        return tokenAction(token.getPayload(), action);
    }

    private String tokenAction(TokenPayload tokenPayload, Action action) {
        return String.format(
                "%s.%s",
                toJson(tokenPayload),
                action.name().toLowerCase());
    }
}
