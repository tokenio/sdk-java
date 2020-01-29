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

package io.token.rpc;

import static io.token.proto.ProtoJson.toJson;
import static io.token.proto.banklink.Banklink.AccountLinkingStatus.FAILURE_BANK_AUTHORIZATION_REQUIRED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.ENDORSED;
import static io.token.rpc.util.Converters.toCompletable;
import static io.token.util.Util.toObservable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.exceptions.BankAuthorizationRequiredException;
import io.token.exceptions.RequestException;
import io.token.exceptions.StepUpRequiredException;
import io.token.proto.PagedList;
import io.token.proto.banklink.Banklink.OauthBankAuthorization;
import io.token.proto.common.account.AccountProtos.Account;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos.BankInfo;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation.Authorization;
import io.token.proto.common.member.MemberProtos.MemberRecoveryRulesOperation;
import io.token.proto.common.member.MemberProtos.MemberUpdate;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.member.MemberProtos.ProfilePictureSize;
import io.token.proto.common.member.MemberProtos.RecoveryRule;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.security.SecurityProtos.CustomerTrackingMetadata;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenSignature.Action;
import io.token.proto.common.transaction.TransactionProtos.Balance;
import io.token.proto.common.transaction.TransactionProtos.StandingOrder;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination;
import io.token.proto.gateway.Gateway.ConfirmFundsRequest;
import io.token.proto.gateway.Gateway.ConfirmFundsResponse;
import io.token.proto.gateway.Gateway.CreateTestBankAccountRequest;
import io.token.proto.gateway.Gateway.CreateTestBankAccountResponse;
import io.token.proto.gateway.Gateway.DeleteMemberRequest;
import io.token.proto.gateway.Gateway.GetAccountRequest;
import io.token.proto.gateway.Gateway.GetAccountResponse;
import io.token.proto.gateway.Gateway.GetAccountsRequest;
import io.token.proto.gateway.Gateway.GetAccountsResponse;
import io.token.proto.gateway.Gateway.GetAliasesRequest;
import io.token.proto.gateway.Gateway.GetAliasesResponse;
import io.token.proto.gateway.Gateway.GetBalanceRequest;
import io.token.proto.gateway.Gateway.GetBalanceResponse;
import io.token.proto.gateway.Gateway.GetBalancesRequest;
import io.token.proto.gateway.Gateway.GetBankInfoRequest;
import io.token.proto.gateway.Gateway.GetBankInfoResponse;
import io.token.proto.gateway.Gateway.GetDefaultAgentRequest;
import io.token.proto.gateway.Gateway.GetDefaultAgentResponse;
import io.token.proto.gateway.Gateway.GetMemberRequest;
import io.token.proto.gateway.Gateway.GetMemberResponse;
import io.token.proto.gateway.Gateway.GetProfilePictureRequest;
import io.token.proto.gateway.Gateway.GetProfilePictureResponse;
import io.token.proto.gateway.Gateway.GetProfileRequest;
import io.token.proto.gateway.Gateway.GetProfileResponse;
import io.token.proto.gateway.Gateway.GetStandingOrderRequest;
import io.token.proto.gateway.Gateway.GetStandingOrdersRequest;
import io.token.proto.gateway.Gateway.GetTransactionRequest;
import io.token.proto.gateway.Gateway.GetTransactionsRequest;
import io.token.proto.gateway.Gateway.LinkAccountsOauthRequest;
import io.token.proto.gateway.Gateway.Page;
import io.token.proto.gateway.Gateway.ResolveTransferDestinationsRequest;
import io.token.proto.gateway.Gateway.ResolveTransferDestinationsResponse;
import io.token.proto.gateway.Gateway.RetryVerificationRequest;
import io.token.proto.gateway.Gateway.RetryVerificationResponse;
import io.token.proto.gateway.Gateway.UpdateMemberRequest;
import io.token.proto.gateway.Gateway.UpdateMemberResponse;
import io.token.proto.gateway.Gateway.VerifyAliasRequest;
import io.token.security.CryptoEngine;
import io.token.security.Signer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * An authenticated RPC client that is used to talk to Token gateway. The
 * class is a thin wrapper on top of gRPC generated client. Makes the API
 * easier to use.
 */
public class Client {
    protected final String memberId;
    protected final CryptoEngine crypto;
    protected final GatewayProvider gateway;
    protected boolean customerInitiated = false;
    protected CustomerTrackingMetadata customerTrackingMetadata = CustomerTrackingMetadata
            .getDefaultInstance();

    /**
     * Creates a client instance.
     *
     * @param memberId member id
     * @param crypto the crypto engine used to sign for authentication, request payloads, etc
     * @param gateway gateway gRPC stub
     */
    protected Client(String memberId, CryptoEngine crypto, GatewayProvider gateway) {
        this.memberId = memberId;
        this.crypto = crypto;
        this.gateway = gateway;
    }

    /**
     * Looks up member information for the current user. The user is defined by
     * the key used for authentication.
     *
     * @param memberId member id
     * @return an observable of member
     */
    public Observable<Member> getMember(String memberId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getMember(GetMemberRequest.newBuilder()
                        .setMemberId(memberId)
                        .build()))
                .map(GetMemberResponse::getMember);
    }

    /**
     * Updates member by applying the specified operations.
     *
     * @param member member to update
     * @param operations operations to apply
     * @param metadata metadata of operations
     * @return an observable of updated member
     */
    public Observable<Member> updateMember(
            Member member,
            List<MemberOperation> operations,
            List<MemberOperationMetadata> metadata) {
        if (operations.isEmpty()) {
            return Observable.just(member);
        }
        Signer signer = crypto.createSigner(PRIVILEGED);
        MemberUpdate update = MemberUpdate
                .newBuilder()
                .setMemberId(member.getId())
                .setPrevHash(member.getLastHash())
                .addAllOperations(operations)
                .build();

        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .updateMember(UpdateMemberRequest
                        .newBuilder()
                        .setUpdate(update)
                        .setUpdateSignature(Signature
                                .newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(update)))
                        .addAllMetadata(metadata)
                        .build()))
                .map(UpdateMemberResponse::getMember);
    }

    /**
     * Updates member by applying the specified operations that don't contain addAliasOperation.
     *
     * @param member member to update
     * @param operations operations to apply
     * @return an observable of updated member
     */
    public Observable<Member> updateMember(Member member, List<MemberOperation> operations) {
        return updateMember(member, operations, Collections.emptyList());
    }

    /**
     * Set Token as the recovery agent.
     *
     * @return a completable
     */
    public Completable useDefaultRecoveryRule() {
        final Signer signer = crypto.createSigner(PRIVILEGED);
        return getMember(memberId)
                .flatMap(member -> toObservable(gateway
                        .withAuthentication(authenticationContext())
                        .getDefaultAgent(GetDefaultAgentRequest.getDefaultInstance()))
                        .map(response -> {
                            RecoveryRule rule = RecoveryRule.newBuilder()
                                    .setPrimaryAgent(response.getMemberId())
                                    .build();
                            return MemberUpdate.newBuilder()
                                    .setPrevHash(member.getLastHash())
                                    .setMemberId(member.getId())
                                    .addOperations(MemberOperation.newBuilder()
                                            .setRecoveryRules(
                                                    MemberRecoveryRulesOperation
                                                            .newBuilder()
                                                            .setRecoveryRule(rule)))
                                    .build();
                        }))
                .flatMapCompletable(update -> toCompletable(gateway
                        .withAuthentication(authenticationContext())
                        .updateMember(UpdateMemberRequest.newBuilder()
                                .setUpdate(update)
                                .setUpdateSignature(Signature.newBuilder()
                                        .setKeyId(signer.getKeyId())
                                        .setMemberId(memberId)
                                        .setSignature(signer.sign(update)))
                                .build())));
    }

    /**
     * Gets a member's public profile.
     *
     * @param memberId member Id whose profile we want
     * @return their profile text
     */
    public Observable<Profile> getProfile(String memberId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getProfile(GetProfileRequest.newBuilder()
                        .setMemberId(memberId)
                        .build()))
                .map(GetProfileResponse::getProfile);
    }

    /**
     * Gets a member's public profile picture.
     *
     * @param memberId member Id whose profile we want
     * @param size size category we want (small, medium, large, original)
     * @return blob with picture; empty blob (no fields set) if has no picture
     */
    public Observable<Blob> getProfilePicture(String memberId, ProfilePictureSize size) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getProfilePicture(GetProfilePictureRequest.newBuilder()
                        .setMemberId(memberId)
                        .setSize(size)
                        .build()))
                .map(GetProfilePictureResponse::getBlob);
    }

    /**
     * Signs a token payload.
     *
     * @param payload token payload
     * @param keyLevel key level
     * @return token payload signature
     */
    public Signature signTokenPayload(TokenPayload payload, Key.Level keyLevel) {
        final Signer signer = crypto.createSigner(keyLevel);
        return Signature.newBuilder()
                .setKeyId(signer.getKeyId())
                .setMemberId(memberId)
                .setSignature(signer.sign(tokenAction(payload, ENDORSED)))
                .build();
    }

    /**
     * Looks up a linked funding account.
     *
     * @param accountId account id
     * @return account info
     */
    public Observable<Account> getAccount(String accountId) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf())
                .getAccount(GetAccountRequest
                        .newBuilder()
                        .setAccountId(accountId)
                        .build()))
                .map(GetAccountResponse::getAccount);
    }


    /**
     * Looks up all the linked funding accounts.
     *
     * @return list of linked accounts
     */
    public Observable<List<Account>> getAccounts() {
        return toObservable(gateway
                .withAuthentication(onBehalfOf())
                .getAccounts(GetAccountsRequest
                        .newBuilder()
                        .build()))
                .map(new Function<GetAccountsResponse, List<Account>>() {
                    public List<Account> apply(GetAccountsResponse response) {
                        return response.getAccountsList();
                    }
                });
    }

    /**
     * Look up account balance.
     *
     * @param accountId account id
     * @param keyLevel key level
     * @return account balance
     */
    public Observable<Balance> getBalance(String accountId, Key.Level keyLevel) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf(keyLevel))
                .getBalance(GetBalanceRequest.newBuilder()
                        .setAccountId(accountId)
                        .build()))
                .map(response -> {
                    switch (response.getStatus()) {
                        case SUCCESSFUL_REQUEST:
                            return response.getBalance();
                        case MORE_SIGNATURES_NEEDED:
                            throw new StepUpRequiredException("Balance step up required.");
                        default:
                            throw new RequestException(response.getStatus());
                    }
                });
    }

    /**
     * Look up balances for a list of accounts.
     *
     * @param accountIds list of account ids
     * @param keyLevel key level
     * @return list of balances
     */
    public Observable<List<Balance>> getBalances(List<String> accountIds, Key.Level keyLevel) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf(keyLevel))
                .getBalances(GetBalancesRequest
                        .newBuilder()
                        .addAllAccountId(accountIds)
                        .build()))
                .map(response -> {
                    List<Balance> balances = new ArrayList<>();
                    for (GetBalanceResponse getBalanceResponse : response.getResponseList()) {
                        switch (getBalanceResponse.getStatus()) {
                            case SUCCESSFUL_REQUEST:
                                balances.add(getBalanceResponse.getBalance());
                                break;
                            case MORE_SIGNATURES_NEEDED:
                                throw new StepUpRequiredException("Balance step up required.");
                            default:
                                throw new RequestException(getBalanceResponse.getStatus());
                        }
                    }
                    return balances;
                });
    }

    /**
     * Look up an existing transaction and return the response.
     *
     * @param accountId account id
     * @param transactionId transaction id
     * @param keyLevel key level
     * @return transaction
     */
    public Observable<Transaction> getTransaction(
            String accountId,
            String transactionId,
            Key.Level keyLevel) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf(keyLevel))
                .getTransaction(GetTransactionRequest
                        .newBuilder()
                        .setAccountId(accountId)
                        .setTransactionId(transactionId)
                        .build()))
                .map(response -> {
                    switch (response.getStatus()) {
                        case SUCCESSFUL_REQUEST:
                            return response.getTransaction();
                        case MORE_SIGNATURES_NEEDED:
                            throw new StepUpRequiredException("Transaction step up required.");
                        default:
                            throw new RequestException(response.getStatus());
                    }
                });
    }

    /**
     * Lookup transactions and return response.
     *
     * @param accountId account id
     * @param offset offset
     * @param limit limit
     * @param keyLevel key level
     * @param startDate inclusive lower bound of transaction booking date
     * @param endDate inclusive upper bound of transaction booking date
     * @return paged list of transactions
     */
    public Observable<PagedList<Transaction, String>> getTransactions(
            String accountId,
            @Nullable String offset,
            int limit,
            Key.Level keyLevel,
            @Nullable String startDate,
            @Nullable String endDate) {
        GetTransactionsRequest.Builder builder = GetTransactionsRequest
                .newBuilder()
                .setAccountId(accountId)
                .setPage(pageBuilder(offset, limit));
        if (startDate != null) {
            builder.setStartDate(startDate);
        }
        if (endDate != null) {
            builder.setEndDate(endDate);
        }
        return toObservable(gateway
                .withAuthentication(onBehalfOf(keyLevel))
                .getTransactions(builder.build()))
                .map(response -> {
                    switch (response.getStatus()) {
                        case SUCCESSFUL_REQUEST:
                            return PagedList.create(
                                    response.getTransactionsList(),
                                    response.getOffset());
                        case MORE_SIGNATURES_NEEDED:
                            throw new StepUpRequiredException("Transactions step up required.");
                        default:
                            throw new RequestException(response.getStatus());
                    }
                });
    }

    /**
     * Look up an existing standing order and return the response.
     *
     * @param accountId account ID
     * @param standingOrderId standing order ID
     * @param keyLevel key level
     * @return transaction
     */
    public Observable<StandingOrder> getStandingOrder(
            String accountId,
            String standingOrderId,
            Key.Level keyLevel) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf(keyLevel))
                .getStandingOrder(GetStandingOrderRequest
                        .newBuilder()
                        .setAccountId(accountId)
                        .setStandingOrderId(standingOrderId)
                        .build()))
                .map(response -> {
                    switch (response.getStatus()) {
                        case SUCCESSFUL_REQUEST:
                            return response.getStandingOrder();
                        case MORE_SIGNATURES_NEEDED:
                            throw new StepUpRequiredException("Standing order step up required.");
                        default:
                            throw new RequestException(response.getStatus());
                    }
                });
    }

    /**
     * Look up standing orders and return response.
     *
     * @param accountId account ID
     * @param offset offset
     * @param limit limit
     * @param keyLevel key level
     * @return paged list of standing orders
     */
    public Observable<PagedList<StandingOrder, String>> getStandingOrders(
            String accountId,
            @Nullable String offset,
            int limit,
            Key.Level keyLevel) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf(keyLevel))
                .getStandingOrders(GetStandingOrdersRequest
                        .newBuilder()
                        .setAccountId(accountId)
                        .setPage(pageBuilder(offset, limit))
                        .build()))
                .map(response -> {
                    switch (response.getStatus()) {
                        case SUCCESSFUL_REQUEST:
                            return PagedList.create(
                                    response.getStandingOrdersList(),
                                    response.getOffset());
                        case MORE_SIGNATURES_NEEDED:
                            throw new StepUpRequiredException("Standing order step up required.");
                        default:
                            throw new RequestException(response.getStatus());
                    }
                });
    }

    /**
     * Confirm that the given account has sufficient funds to cover the charge.
     *
     * @param accountId account ID
     * @param amount charge amount
     * @return true if the account has sufficient funds to cover the charge
     */
    public Observable<Boolean> confirmFunds(String accountId, Money amount) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf())
                .confirmFunds(ConfirmFundsRequest.newBuilder()
                        .setAccountId(accountId)
                        .setAmount(amount)
                        .build()))
                .map(ConfirmFundsResponse::getFundsAvailable);
    }

    /**
     * Returns linking information for the specified bank id.
     *
     * @param bankId the bank id
     * @return bank linking information
     */
    public Observable<BankInfo> getBankInfo(String bankId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getBankInfo(GetBankInfoRequest
                        .newBuilder()
                        .setBankId(bankId)
                        .build()))
                .map(GetBankInfoResponse::getInfo);
    }

    /**
     * Links a funding bank account to Token.
     *
     * @param authorization OAuth authorization for linking
     * @return list of linked accounts
     * @throws BankAuthorizationRequiredException if bank authorization payload
     *     is required to link accounts
     */
    public Observable<List<Account>> linkAccounts(OauthBankAuthorization authorization)
            throws BankAuthorizationRequiredException {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .linkAccountsOauth(LinkAccountsOauthRequest
                        .newBuilder()
                        .setAuthorization(authorization)
                        .build()))
                .map(response -> {
                    if (response.getStatus() == FAILURE_BANK_AUTHORIZATION_REQUIRED) {
                        throw new BankAuthorizationRequiredException();
                    }
                    return response.getAccountsList();
                });
    }

    /**
     * Creates a test bank account and links it.
     *
     * @param balance account balance to set
     * @return linked account
     */
    public Observable<Account> createAndLinkTestBankAccount(Money balance) {
        return createTestBankAuth(balance)
                .flatMap(authorization -> linkAccounts(authorization)
                        .map(accounts -> {
                            if (accounts.size() != 1) {
                                throw new RuntimeException(
                                        "Expected 1 account; found "
                                                + accounts.size());
                            }
                            return accounts.get(0);
                        }));
    }

    /**
     * Returns a list of aliases of the member.
     *
     * @return a list of aliases
     */
    public Observable<List<Alias>> getAliases() {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getAliases(GetAliasesRequest
                        .newBuilder()
                        .build()))
                .map(GetAliasesResponse::getAliasesList);
    }

    /**
     * Retry alias verification.
     *
     * @param alias the alias to be verified
     * @return the verification id
     */
    public Observable<String> retryVerification(Alias alias) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .retryVerification(RetryVerificationRequest.newBuilder()
                        .setAlias(alias)
                        .setMemberId(memberId)
                        .build()))
                .map(RetryVerificationResponse::getVerificationId);
    }

    /**
     * Authorizes recovery as a trusted agent.
     *
     * @param authorization the authorization
     * @return the signature
     */
    public Observable<Signature> authorizeRecovery(Authorization authorization) {
        Signer signer = crypto.createSigner(STANDARD);
        return Observable.just(Signature.newBuilder()
                .setMemberId(memberId)
                .setKeyId(signer.getKeyId())
                .setSignature(signer.sign(authorization))
                .build());
    }

    /**
     * Gets the member id of the default recovery agent.
     *
     * @return the member id
     */
    public Observable<String> getDefaultAgent() {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getDefaultAgent(GetDefaultAgentRequest.getDefaultInstance()))
                .map(GetDefaultAgentResponse::getMemberId);
    }

    /**
     * Verifies a given alias.
     *
     * @param verificationId the verification id
     * @param code the code
     * @return a completable
     */
    public Completable verifyAlias(String verificationId, String code) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .verifyAlias(VerifyAliasRequest.newBuilder()
                        .setVerificationId(verificationId)
                        .setCode(code)
                        .build()));
    }

    /**
     * Delete the member.
     *
     * @return completable
     */
    public Completable deleteMember() {
        return toCompletable(gateway
                .withAuthentication(authenticationContext(PRIVILEGED))
                .deleteMember(DeleteMemberRequest.getDefaultInstance()));
    }

    /**
     * Resolves transfer destinations for the given account ID.
     *
     * @param accountId account ID
     * @return transfer destinations
     */
    public Observable<List<TransferDestination>> resolveTransferDestinations(String accountId) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf())
                .resolveTransferDestinations(ResolveTransferDestinationsRequest.newBuilder()
                        .setAccountId(accountId)
                        .build()))
                .map(ResolveTransferDestinationsResponse::getTransferDestinationsList);
    }

    public CryptoEngine getCryptoEngine() {
        return crypto;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Client)) {
            return false;
        }

        Client other = (Client) obj;
        return memberId.equals(other.memberId);
    }

    @Override
    public int hashCode() {
        return memberId.hashCode();
    }

    protected AuthenticationContext authenticationContext() {
        return AuthenticationContext.create(
                null,
                false,
                LOW,
                customerTrackingMetadata);
    }

    protected AuthenticationContext authenticationContext(Key.Level level) {
        return AuthenticationContext.create(
                null,
                false,
                level,
                customerTrackingMetadata);
    }

    protected String getOnBehalfOf() {
        return null;
    }

    private AuthenticationContext onBehalfOf() {
        return AuthenticationContext.create(
                getOnBehalfOf(),
                customerInitiated,
                LOW,
                customerTrackingMetadata);
    }

    private AuthenticationContext onBehalfOf(Key.Level level) {
        return AuthenticationContext.create(
                getOnBehalfOf(),
                customerInitiated,
                level,
                customerTrackingMetadata);
    }

    protected Page.Builder pageBuilder(@Nullable String offset, int limit) {
        Page.Builder page = Page.newBuilder()
                .setLimit(limit);
        if (offset != null) {
            page.setOffset(offset);
        }

        return page;
    }

    protected String tokenAction(Token token, Action action) {
        return tokenAction(token.getPayload(), action);
    }

    private String tokenAction(TokenPayload tokenPayload, Action action) {
        return String.format(
                "%s.%s",
                toJson(tokenPayload),
                action.name().toLowerCase());
    }

    private Observable<OauthBankAuthorization> createTestBankAuth(Money balance) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createTestBankAccount(CreateTestBankAccountRequest.newBuilder()
                        .setBalance(balance)
                        .build()))
                .map(CreateTestBankAccountResponse::getAuthorization);
    }
}
