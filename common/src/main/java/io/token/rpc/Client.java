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
import static io.token.proto.common.transaction.TransactionProtos.RequestStatus.SUCCESSFUL_REQUEST;
import static io.token.rpc.util.Converters.toCompletable;
import static io.token.util.Util.toObservable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.exceptions.BankAuthorizationRequiredException;
import io.token.exceptions.StepUpRequiredException;
import io.token.proto.PagedList;
import io.token.proto.banklink.Banklink;
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
import io.token.proto.common.money.MoneyProtos;
import io.token.proto.common.notification.NotificationProtos;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.SecurityMetadata;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenSignature.Action;
import io.token.proto.common.transaction.TransactionProtos.Balance;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.proto.gateway.Gateway;
import io.token.proto.gateway.Gateway.CreateBlobRequest;
import io.token.proto.gateway.Gateway.CreateBlobResponse;
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
import io.token.proto.gateway.Gateway.GetBalancesResponse;
import io.token.proto.gateway.Gateway.GetBankInfoRequest;
import io.token.proto.gateway.Gateway.GetBankInfoResponse;
import io.token.proto.gateway.Gateway.GetBlobRequest;
import io.token.proto.gateway.Gateway.GetBlobResponse;
import io.token.proto.gateway.Gateway.GetDefaultAgentRequest;
import io.token.proto.gateway.Gateway.GetDefaultAgentResponse;
import io.token.proto.gateway.Gateway.GetMemberRequest;
import io.token.proto.gateway.Gateway.GetMemberResponse;
import io.token.proto.gateway.Gateway.GetProfilePictureRequest;
import io.token.proto.gateway.Gateway.GetProfilePictureResponse;
import io.token.proto.gateway.Gateway.GetProfileRequest;
import io.token.proto.gateway.Gateway.GetProfileResponse;
import io.token.proto.gateway.Gateway.GetTransactionRequest;
import io.token.proto.gateway.Gateway.GetTransactionResponse;
import io.token.proto.gateway.Gateway.GetTransactionsRequest;
import io.token.proto.gateway.Gateway.GetTransactionsResponse;
import io.token.proto.gateway.Gateway.LinkAccountsOauthRequest;
import io.token.proto.gateway.Gateway.Page;
import io.token.proto.gateway.Gateway.ResolveTransferDestinationsRequest;
import io.token.proto.gateway.Gateway.ResolveTransferDestinationsResponse;
import io.token.proto.gateway.Gateway.RetryVerificationRequest;
import io.token.proto.gateway.Gateway.RetryVerificationResponse;
import io.token.proto.gateway.Gateway.SetProfilePictureRequest;
import io.token.proto.gateway.Gateway.SetProfileRequest;
import io.token.proto.gateway.Gateway.SetProfileResponse;
import io.token.proto.gateway.Gateway.TriggerStepUpNotificationRequest;
import io.token.proto.gateway.Gateway.TriggerStepUpNotificationResponse;
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
    protected SecurityMetadata trackingMetadata = SecurityMetadata.getDefaultInstance();

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
                .map(new Function<GetMemberResponse, Member>() {
                    public Member apply(GetMemberResponse response) {
                        return response.getMember();
                    }
                });
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
                .map(new Function<UpdateMemberResponse, Member>() {
                    public Member apply(UpdateMemberResponse response) {
                        return response.getMember();
                    }
                });
    }

    /**
     * Updates member by applying the specified operations that don't contain addAliasOperation.
     *
     * @param member member to update
     * @param operations operations to apply
     * @return an observable of updated member
     */
    public Observable<Member> updateMember(Member member, List<MemberOperation> operations) {
        return updateMember(member, operations, Collections.<MemberOperationMetadata>emptyList());
    }

    /**
     * Set Token as the recovery agent.
     *
     * @return a completable
     */
    public Completable useDefaultRecoveryRule() {
        final Signer signer = crypto.createSigner(PRIVILEGED);
        return getMember(memberId)
                .flatMap(new Function<Member, Observable<MemberUpdate>>() {
                    public Observable<MemberUpdate> apply(final Member member) {
                        return toObservable(gateway
                                .withAuthentication(authenticationContext())
                                .getDefaultAgent(GetDefaultAgentRequest.getDefaultInstance()))
                                .map(new Function<GetDefaultAgentResponse, MemberUpdate>() {
                                    public MemberUpdate apply(GetDefaultAgentResponse response) {
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
                                    }
                                });
                    }
                })
                .flatMapCompletable(new Function<MemberUpdate, Completable>() {
                    public Completable apply(MemberUpdate update) {
                        return toCompletable(gateway
                                .withAuthentication(authenticationContext())
                                .updateMember(UpdateMemberRequest.newBuilder()
                                        .setUpdate(update)
                                        .setUpdateSignature(Signature.newBuilder()
                                                .setKeyId(signer.getKeyId())
                                                .setMemberId(memberId)
                                                .setSignature(signer.sign(update)))
                                        .build()));
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
        return toObservable(gateway
                .withAuthentication(onBehalfOf())
                .getAccount(GetAccountRequest
                        .newBuilder()
                        .setAccountId(accountId)
                        .build()))
                .map(new Function<GetAccountResponse, Account>() {
                    public Account apply(GetAccountResponse response) {
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
                .map(new Function<GetBalanceResponse, Balance>() {
                    public Balance apply(GetBalanceResponse response) {
                        if (response.getStatus() == SUCCESSFUL_REQUEST) {
                            return response.getBalance();
                        } else {
                            throw new StepUpRequiredException("Balance step up required.");
                        }
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
                .map(new Function<GetBalancesResponse, List<Balance>>() {
                    public List<Balance> apply(GetBalancesResponse response) {
                        List<Balance> balances = new ArrayList<>();
                        for (GetBalanceResponse getBalanceResponse : response.getResponseList()) {
                            if (getBalanceResponse.getStatus() == SUCCESSFUL_REQUEST) {
                                balances.add(getBalanceResponse.getBalance());
                            }
                        }
                        return balances;
                    }
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
                .map(new Function<GetTransactionResponse, Transaction>() {
                    public Transaction apply(GetTransactionResponse response) {
                        if (response.getStatus() == SUCCESSFUL_REQUEST) {
                            return response.getTransaction();
                        } else {
                            throw new StepUpRequiredException("Transaction step up required.");
                        }
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
     * @return paged list of transactions
     */
    public Observable<PagedList<Transaction, String>> getTransactions(
            String accountId,
            @Nullable String offset,
            int limit,
            Key.Level keyLevel) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf(keyLevel))
                .getTransactions(GetTransactionsRequest
                        .newBuilder()
                        .setAccountId(accountId)
                        .setPage(pageBuilder(offset, limit))
                        .build()))
                .map(new Function<GetTransactionsResponse, PagedList<Transaction, String>>() {
                    public PagedList<Transaction, String> apply(GetTransactionsResponse response) {
                        if (response.getStatus() == SUCCESSFUL_REQUEST) {
                            return PagedList.create(
                                    response.getTransactionsList(),
                                    response.getOffset());
                        } else {
                            throw new StepUpRequiredException("Transactions step up required.");
                        }
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
                .withAuthentication(authenticationContext())
                .getBankInfo(GetBankInfoRequest
                        .newBuilder()
                        .setBankId(bankId)
                        .build()))
                .map(new Function<GetBankInfoResponse, BankInfo>() {
                    public BankInfo apply(GetBankInfoResponse response) {
                        return response.getInfo();
                    }
                });
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
                .map(new Function<Gateway.LinkAccountsOauthResponse, List<Account>>() {
                    public List<Account> apply(Gateway.LinkAccountsOauthResponse response) {
                        if (response.getStatus() == FAILURE_BANK_AUTHORIZATION_REQUIRED) {
                            throw new BankAuthorizationRequiredException();
                        }
                        return response.getAccountsList();
                    }
                });
    }

    /**
     * Creates a test bank account and links it.
     *
     * @param balance account balance to set
     * @return linked account
     */
    public Observable<Account> createAndLinkTestBankAccount(MoneyProtos.Money balance) {
        return createTestBankAccount(balance)
                .flatMap(new Function<OauthBankAuthorization, Observable<Account>>() {
                    @Override
                    public Observable<Account> apply(OauthBankAuthorization authorization) {
                        return linkAccounts(authorization)
                                .map(new Function<List<Account>, Account>() {
                                    @Override
                                    public Account apply(List<Account> accounts) {
                                        if (accounts.size() != 1) {
                                            throw new RuntimeException(
                                                    "Expected 1 account; found "
                                                            + accounts.size());
                                        }
                                        return accounts.get(0);
                                    }
                                });
                    }
                });
    }

    /**
     * Creates a test bank account and returns the authorization for it.
     *
     * @param balance account balance to set
     * @return OAuth bank authorization
     */
    public Observable<OauthBankAuthorization> createTestBankAccount(MoneyProtos.Money balance) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createTestBankAccount(CreateTestBankAccountRequest.newBuilder()
                        .setBalance(balance)
                        .build()))
                .map(new Function<CreateTestBankAccountResponse, OauthBankAuthorization>() {
                    public OauthBankAuthorization apply(CreateTestBankAccountResponse response) {
                        return response.getAuthorization();
                    }
                });
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
                .map(new Function<GetAliasesResponse, List<Alias>>() {
                    public List<Alias> apply(GetAliasesResponse response) {
                        return response.getAliasesList();
                    }
                });
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
                .map(new Function<RetryVerificationResponse, String>() {
                    public String apply(RetryVerificationResponse response) {
                        return response.getVerificationId();
                    }
                });
    }

    /**
     * Authorizes recovery as a trusted agent.
     *
     * @param authorization the authorization
     * @return the signature
     */
    public Observable<Signature> authorizeRecovery(Authorization authorization) {
        Signer signer = crypto.createSigner(PRIVILEGED);
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
                .map(new Function<GetDefaultAgentResponse, String>() {
                    public String apply(GetDefaultAgentResponse response) {
                        return response.getMemberId();
                    }
                });
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
     * @return transfer endpoints
     */
    public Observable<List<TransferEndpoint>> resolveTransferDestinations(String accountId) {
        return toObservable(gateway
                .withAuthentication(onBehalfOf())
                .resolveTransferDestinations(ResolveTransferDestinationsRequest.newBuilder()
                        .setAccountId(accountId)
                        .build()))
                .map(new Function<ResolveTransferDestinationsResponse, List<TransferEndpoint>>() {
                    @Override
                    public List<TransferEndpoint> apply(
                            ResolveTransferDestinationsResponse response) {
                        return response.getDestinationsList();
                    }
                });
    }

    public CryptoEngine getCryptoEngine() {
        return crypto;
    }

    /**
     * Sets tracking metadata included in all requests.
     *
     * @param trackingMetadata tracking metadata
     */
    public void setTrackingMetadata(SecurityMetadata trackingMetadata) {
        this.trackingMetadata = trackingMetadata;
    }

    /**
     * Clears tracking metadata.
     */
    public void clearTrackingMetadata() {
        this.trackingMetadata = SecurityMetadata.getDefaultInstance();
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
        return AuthenticationContext.create(null, false, LOW, trackingMetadata);
    }

    protected AuthenticationContext authenticationContext(Key.Level level) {
        return AuthenticationContext.create(null, false, level, trackingMetadata);
    }

    protected String getOnBehalfOf() {
        return null;
    }

    private AuthenticationContext onBehalfOf() {
        return AuthenticationContext.create(
                getOnBehalfOf(),
                customerInitiated,
                LOW,
                trackingMetadata);
    }

    private AuthenticationContext onBehalfOf(Key.Level level) {
        return AuthenticationContext.create(
                getOnBehalfOf(),
                customerInitiated,
                level,
                trackingMetadata);
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
}
