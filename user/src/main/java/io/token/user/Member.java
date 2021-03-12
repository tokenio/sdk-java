/**
 * Copyright (c) 2021 Token, Inc.
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

package io.token.user;

import static io.reactivex.Completable.fromObservable;
import static io.token.proto.common.blob.BlobProtos.Blob.AccessMode.PUBLIC;
import static io.token.user.util.Util.findFirstCapturingGroup;
import static io.token.user.util.Util.generateNonce;
import static io.token.user.util.Util.getWebAppUrl;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.Function;
import io.token.TokenClient.TokenCluster;
import io.token.exceptions.BankAuthorizationRequiredException;
import io.token.proto.MoneyUtil;
import io.token.proto.PagedList;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.banklink.Banklink.OauthBankAuthorization;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.blob.BlobProtos.Blob.Payload;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.member.MemberProtos.ReceiptContact;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.notification.NotificationProtos.Notification.Status;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.submission.SubmissionProtos.StandingOrderSubmission;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.BulkTransferBody;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenRequest;
import io.token.proto.common.transaction.TransactionProtos.Balance;
import io.token.proto.common.transfer.TransferProtos.BulkTransfer;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transfer.TransferProtos.TransferPayload;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.proto.gateway.Gateway.GetTokensRequest;
import io.token.user.browser.Browser;
import io.token.user.browser.BrowserFactory;
import io.token.user.rpc.Client;

import java.math.BigDecimal;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Member in the Token system. Each member has an active secret
 * and public key pair that is used to perform authentication.
 */
public class Member extends io.token.Member {
    private static final Logger logger = LoggerFactory.getLogger(Member.class);

    private final Client client;
    private final BrowserFactory browserFactory;

    /**
     * Creates an instance of {@link Member}.
     *
     * @param memberId member ID
     * @param partnerId member ID of partner
     * @param client RPC client used to perform operations against the server
     * @param cluster Token cluster, e.g. sandbox, production
     * @param browserFactory browser factory for displaying UI for linking
     */
    Member(
            String memberId,
            @Nullable String partnerId,
            @Nullable String realmId,
            Client client,
            TokenCluster cluster,
            BrowserFactory browserFactory) {
        super(memberId, partnerId, realmId, client, cluster);
        this.client = client;
        this.browserFactory = browserFactory;
    }

    /**
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @return list of accounts
     */
    public Observable<List<Account>> getAccounts() {
        return super.getAccountsImpl()
                .map(accs -> {
                    List<Account> accounts = Lists.newArrayList();
                    for (io.token.Account acc : accs) {
                        accounts.add(new Account(acc, client, Member.this));
                    }
                    return accounts;
                });
    }

    /**
     * Looks up funding bank accounts linked to Token.
     *
     * @return list of linked accounts
     */
    public List<Account> getAccountsBlocking() {
        return getAccounts().blockingSingle();
    }

    /**
     * Looks up a funding bank account linked to Token.
     *
     * @param accountId account id
     * @return looked up account
     */
    public Observable<Account> getAccount(String accountId) {
        return getAccountImpl(accountId)
                .map(acc -> new Account(acc, client, Member.this));
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
     * Set the default bank account for this member.
     *
     * @param accountId ID of default account to set
     * @return completable
     */
    public Completable setDefaultAccount(String accountId) {
        return client.setDefaultAccount(accountId);
    }

    /**
     * Set the default bank account for this member.
     *
     * @param accountId ID of default account to set
     */
    public void setDefaultAccountBlocking(String accountId) {
        client.setDefaultAccount(accountId).blockingAwait();
    }

    /**
     * Get the default bank account for this member.
     *
     * @return observable account
     */
    public Observable<Account> getDefaultAccount() {
        return client
                .getDefaultAccount()
                .map(account -> new Account(Member.this, account, client));
    }

    /**
     * Gets the default bank account.
     *
     * @return the default bank account
     */
    public Account getDefaultAccountBlocking() {
        return getDefaultAccount().blockingSingle();
    }

    /**
     * Looks up an existing token transfer.
     *
     * @param transferId ID of the transfer record
     * @return transfer record
     */
    public Observable<Transfer> getTransfer(String transferId) {
        return client.getTransfer(transferId);
    }

    /**
     * Looks up an existing token transfer.
     *
     * @param transferId ID of the transfer record
     * @return transfer record
     */
    public Transfer getTransferBlocking(String transferId) {
        return getTransfer(transferId).blockingSingle();
    }

    /**
     * Looks up an existing bulk transfer.
     *
     * @param bulkTransferId bulk transfer ID
     * @return bulk transfer record
     */
    public Observable<BulkTransfer> getBulkTransfer(String bulkTransferId) {
        return client.getBulkTransfer(bulkTransferId);
    }

    /**
     * Looks up an existing bulk transfer.
     *
     * @param bulkTransferId bulk transfer ID
     * @return bulk transfer record
     */
    public BulkTransfer getBulkTransferBlocking(String bulkTransferId) {
        return getBulkTransfer(bulkTransferId).blockingSingle();
    }

    /**
     * Looks up an existing Token standing order submission.
     *
     * @param submissionId ID of the standing orde submission
     * @return standing order submission
     */
    public Observable<StandingOrderSubmission> getStandingOrderSubmission(String submissionId) {
        return client.getStandingOrderSubmission(submissionId);
    }

    /**
     * Looks up an existing Token standing order submission.
     *
     * @param submissionId ID of the standing orde submission
     * @return standing order submission
     */
    public StandingOrderSubmission getStandingOrderSubmissionBlocking(String submissionId) {
        return getStandingOrderSubmission(submissionId).blockingSingle();
    }

    /**
     * Looks up existing token transfers.
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
        return client.getTransfers(offset, limit, tokenId);
    }

    /**
     * Looks up existing token transfers.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @param tokenId optional token id to restrict the search
     * @return transfer record
     */
    public PagedList<Transfer, String> getTransfersBlocking(
            @Nullable String offset,
            int limit,
            @Nullable String tokenId) {
        return getTransfers(offset, limit, tokenId).blockingSingle();
    }

    /**
     * Looks up existing Token standing order submissions.
     *
     * @param offset optional offset to start at
     * @param limit max number of submissions to return
     * @return standing order submissions
     */
    public Observable<PagedList<StandingOrderSubmission, String>> getStandingOrderSubmissions(
            @Nullable String offset,
            int limit) {
        return client.getStandingOrderSubmissions(offset, limit);
    }

    /**
     * Looks up existing Token standing order submissions.
     *
     * @param offset optional offset to start at
     * @param limit max number of submissions to return
     * @return standing order submissions
     */
    public PagedList<StandingOrderSubmission, String> getStandingOrderSubmissionsBlocking(
            @Nullable String offset,
            int limit) {
        return getStandingOrderSubmissions(offset, limit).blockingSingle();
    }

    /**
     * Prepares a transfer token, returning the resolved token payload and policy.
     *
     * @param transferTokenBuilder transfer token builder
     * @return resolved token payload and policy
     */
    public Observable<PrepareTokenResult> prepareTransferToken(
            TransferTokenBuilder transferTokenBuilder) {
        transferTokenBuilder.from(memberId());
        return client.prepareToken(transferTokenBuilder.buildPayload());
    }

    /**
     * Prepares a transfer token, returning the resolved token payload and policy.
     *
     * @param transferTokenBuilder transfer token builder
     * @return resolved token payload and policy
     */
    public PrepareTokenResult prepareTransferTokenBlocking(
            TransferTokenBuilder transferTokenBuilder) {
        return prepareTransferToken(transferTokenBuilder).blockingSingle();
    }

    /**
     * Prepares a bulk transfer token, returning the resolved token payload
     * and policy.
     *
     * @param builder bulk transfer token builder
     * @return resolved token payload and policy
     */
    public Observable<PrepareTokenResult> prepareBulkTransferToken(
            BulkTransferTokenBuilder builder) {
        return client.prepareToken(builder.buildPayload());
    }

    /**
     * Prepares a bulk transfer token, returning the resolved token payload
     * and policy.
     *
     * @param builder bulk transfer token builder
     * @return resolved token payload and policy
     */
    public PrepareTokenResult prepareBulkTransferTokenBlocking(
            BulkTransferTokenBuilder builder) {
        return prepareBulkTransferToken(builder).blockingSingle();
    }

    /**
     * Prepares a standing order token, returning the resolved token payload
     * and policy.
     *
     * @param builder standing order token builder
     * @return resolved token payload and policy
     */
    public Observable<PrepareTokenResult> prepareStandingOrderToken(
            StandingOrderTokenBuilder builder) {
        return client.prepareToken(builder.buildPayload());
    }

    /**
     * Prepares a standing order token, returning the resolved token payload
     * and policy.
     *
     * @param builder standing order token builder
     * @return resolved token payload and policy
     */
    public PrepareTokenResult prepareStandingOrderTokenBlocking(
            StandingOrderTokenBuilder builder) {
        return prepareStandingOrderToken(builder).blockingSingle();
    }

    /**
     * Creates a token directly from a resolved token payload and list of token signatures.
     *
     * @param payload token payload
     * @param signatures list of signatures
     * @return token returned by the server
     */
    public Observable<Token> createToken(TokenPayload payload, List<Signature> signatures) {
        return createToken(payload, signatures, null);
    }

    /**
     * Creates a token directly from a resolved token payload and list of token signatures.
     *
     * @param payload token payload
     * @param signatures list of signatures
     * @param tokenRequestId token request ID
     * @return token returned by server
     */
    public Observable<Token> createToken(
            TokenPayload payload,
            List<Signature> signatures,
            @Nullable String tokenRequestId) {
        return client.createToken(payload, tokenRequestId, signatures);
    }

    /**
     * Creates a token with the member's own signature.
     *
     * @param payload token payload
     * @param keyLevel key level
     * @return token returned by the server
     */
    public Observable<Token> createToken(TokenPayload payload, Key.Level keyLevel) {
        return createToken(payload, null, keyLevel);
    }

    /**
     * Creates a token with the member's own signature.
     *
     * @param payload token payload
     * @param tokenRequestId token request ID
     * @param keyLevel key level
     * @return token returned by the server
     */
    public Observable<Token> createToken(
            TokenPayload payload,
            @Nullable String tokenRequestId,
            Key.Level keyLevel) {
        return client.createToken(
                payload,
                tokenRequestId,
                Collections.singletonList(signTokenPayload(payload, keyLevel)));
    }

    /**
     * Creates a token directly from a resolved token payload and list of token signatures.
     *
     * @param payload token payload
     * @param signatures list of signatures
     * @return token returned by the server
     */
    public Token createTokenBlocking(TokenPayload payload, List<Signature> signatures) {
        return createToken(payload, signatures).blockingSingle();
    }

    /**
     * Creates a token directly from a resolved token payload and list of token signatures.
     *
     * @param payload token payload
     * @param signatures list of signatures
     * @param tokenRequestId token request ID
     * @return token returned by server
     */
    public Token createTokenBlocking(
            TokenPayload payload,
            List<Signature> signatures,
            @Nullable String tokenRequestId) {
        return createToken(payload, signatures, tokenRequestId).blockingSingle();
    }

    /**
     * Creates a token with the member's own signature.
     *
     * @param payload token payload
     * @param keyLevel key level
     * @return token returned by the server
     */
    public Token createTokenBlocking(TokenPayload payload, Key.Level keyLevel) {
        return createToken(payload, keyLevel).blockingSingle();
    }

    /**
     * Creates a token with the member's own signature.
     *
     * @param payload token payload
     * @param tokenRequestId token request ID
     * @param keyLevel key level
     * @return token returned by the server
     */
    public Token createTokenBlocking(
            TokenPayload payload,
            @Nullable String tokenRequestId,
            Key.Level keyLevel) {
        return createToken(payload, tokenRequestId, keyLevel).blockingSingle();
    }

    /**
     * Creates a new transfer token builder.
     *
     * @param amount transfer amount
     * @param currency currency code, e.g. "USD"
     * @return transfer token builder
     */
    public TransferTokenBuilder createTransferTokenBuilder(double amount, String currency) {
        return new TransferTokenBuilder(this, amount, currency);
    }

    /**
     * Creates a new transfer token builder from a token request.
     *
     * @param tokenRequest token request
     * @return transfer token builder
     */
    public TransferTokenBuilder createTransferTokenBuilder(TokenRequest tokenRequest) {
        return new TransferTokenBuilder(this, tokenRequest);
    }

    /**
     * Creates a new transfer token builder from a token payload.
     *
     * @param tokenPayload token payload
     * @return transfer token builder
     */
    public TransferTokenBuilder createTransferTokenBuilder(TokenPayload tokenPayload) {
        return new TransferTokenBuilder(this, tokenPayload);
    }

    /**
     * Creates a new bulk transfer token builder.
     *
     * @param transfers list of transfers
     * @param totalAmount total amount irrespective of currency. Used for redundancy check.
     * @param source source account for all transfer
     * @return bulk transfer token builder
     */
    public BulkTransferTokenBuilder createBulkTransferTokenBuilder(
            List<BulkTransferBody.Transfer> transfers,
            double totalAmount,
            TransferEndpoint source) {
        return new BulkTransferTokenBuilder(this, transfers, totalAmount, source);
    }

    /**
     * Creates a new bulk transfer token builder from a token request.
     *
     * @param tokenRequest token request
     * @return bulk transfer token builder
     */
    public BulkTransferTokenBuilder createBulkTransferTokenBuilder(TokenRequest tokenRequest) {
        return new BulkTransferTokenBuilder(tokenRequest);
    }

    /**
     * Creates a new standing order token builder. Defines a standing order
     * for a fixed time span.
     *
     * @param amount individual transfer amount
     * @param currency currency code, e.g. "USD"
     * @param frequency ISO 20022 code for the frequency of the standing order:
     *                  DAIL, WEEK, TOWK, MNTH, TOMN, QUTR, SEMI, YEAR
     * @param startDate start date of the standing order: ISO 8601 YYYY-MM-DD
     * @param endDate end date of the standing order: ISO 8601 YYYY-MM-DD
     * @return standing order token builder
     */
    public StandingOrderTokenBuilder createStandingOrderTokenBuilder(
            double amount,
            String currency,
            String frequency,
            String startDate,
            String endDate) {
        return new StandingOrderTokenBuilder(
                this,
                amount,
                currency,
                frequency,
                startDate,
                endDate);
    }

    /**
     * Creates a new indefinite standing order token builder.
     *
     * @param amount individual transfer amount
     * @param currency currency code, e.g. "USD"
     * @param frequency ISO 20022 code for the frequency of the standing order:
     *                  DAIL, WEEK, TOWK, MNTH, TOMN, QUTR, SEMI, YEAR
     * @param startDate start date of the standing order: ISO 8601 YYYY-MM-DD
     * @return standing order token builder
     */
    public StandingOrderTokenBuilder createStandingOrderTokenBuilder(
            double amount,
            String currency,
            String frequency,
            String startDate) {
        return new StandingOrderTokenBuilder(
                this,
                amount,
                currency,
                frequency,
                startDate,
                null);
    }

    /**
     * Creates a new standing order token builder from a token request.
     *
     * @param tokenRequest token request
     * @return transfer token builder
     */
    public StandingOrderTokenBuilder createStandingOrderTokenBuilder(TokenRequest tokenRequest) {
        return new StandingOrderTokenBuilder(tokenRequest);
    }

    /**
     * Creates an access token built from a given {@link AccessTokenBuilder}.
     *
     * @param accessTokenBuilder an {@link AccessTokenBuilder} to create access token from
     * @return the access token created
     */
    public Observable<Token> createAccessToken(AccessTokenBuilder accessTokenBuilder) {
        return client.createAccessToken(
                accessTokenBuilder.from(memberId()).build(),
                accessTokenBuilder.getTokenRequestId());
    }

    /**
     * Creates an access token built from a given {@link AccessTokenBuilder}.
     *
     * @param accessTokenBuilder an {@link AccessTokenBuilder} to create access token from
     * @return the access token created
     */
    public Token createAccessTokenBlocking(AccessTokenBuilder accessTokenBuilder) {
        return createAccessToken(accessTokenBuilder).blockingSingle();
    }

    /**
     * Endorses the token by signing it. The signature is persisted along
     * with the token.
     *
     * <p>If the key's level is too low, the result's status is MORE_SIGNATURES_NEEDED
     * and the system pushes a notification to the member prompting them to use a
     * higher-privilege key.
     *
     * @param token token to endorse
     * @param keyLevel key level to be used to endorse the token
     * @return result of endorse token
     */
    public Observable<TokenOperationResult> endorseToken(Token token, Key.Level keyLevel) {
        return client.endorseToken(token, keyLevel);
    }

    /**
     * Endorses the token by signing it. The signature is persisted along
     * with the token.
     *
     * <p>If the key's level is too low, the result's status is MORE_SIGNATURES_NEEDED
     * and the system pushes a notification to the member prompting them to use a
     * higher-privilege key.
     *
     * @param token token to endorse
     * @param keyLevel key level to be used to endorse the token
     * @return result of endorse token
     */
    public TokenOperationResult endorseTokenBlocking(Token token, Key.Level keyLevel) {
        return endorseToken(token, keyLevel).blockingSingle();
    }

    /**
     * Cancels the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to cancel
     * @return result of cancel token
     */
    public Observable<TokenOperationResult> cancelToken(Token token) {
        return client.cancelToken(token);
    }

    /**
     * Cancels the token by signing it. The signature is persisted along
     * with the token.
     *
     * @param token token to cancel
     * @return result of endorsed token
     */
    public TokenOperationResult cancelTokenBlocking(Token token) {
        return cancelToken(token).blockingSingle();
    }

    /**
     * Cancels the existing access token and creates a replacement for it.
     *
     * @param tokenToCancel old token to cancel
     * @param tokenToCreate an {@link AccessTokenBuilder} to create new token from
     * @return result of the replacement operation
     */
    public Observable<TokenOperationResult> replaceAccessToken(
            Token tokenToCancel,
            AccessTokenBuilder tokenToCreate) {
        return client.replace(
                tokenToCancel,
                tokenToCreate.from(memberId()).build());
    }

    /**
     * Cancels the existing access token, creates a replacement and optionally endorses it.
     *
     * @param tokenToCancel old token to cancel
     * @param tokenToCreate an {@link AccessTokenBuilder} to create new token from
     * @return result of the replacement operation
     */
    public TokenOperationResult replaceAccessTokenBlocking(
            Token tokenToCancel,
            AccessTokenBuilder tokenToCreate) {
        return replaceAccessToken(tokenToCancel, tokenToCreate)
                .blockingSingle();
    }

    /**
     * Replaces the member's receipt contact.
     *
     * @param contact receipt contact to set
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable setReceiptContact(ReceiptContact contact) {
        return client.setReceiptContact(contact);
    }

    /**
     * Replaces the member's receipt contact.
     *
     * @param receiptContact receipt contact to set
     */
    public void setReceiptContactBlocking(ReceiptContact receiptContact) {
        setReceiptContact(receiptContact).blockingAwait();
    }

    /**
     * Gets the member's receipt email address.
     *
     * @return receipt contact
     */
    public Observable<ReceiptContact> getReceiptContact() {
        return client.getReceiptContact();
    }

    /**
     * Gets the member's receipt contact.
     *
     * @return receipt contact
     */
    public ReceiptContact getReceiptContactBlocking() {
        return getReceiptContact().blockingSingle();
    }

    /**
     * Looks up a existing access token where the calling member is the grantor and given member is
     * the grantee.
     *
     * @param toMemberId beneficiary of the active access token
     * @return access token returned by the server
     */
    public Observable<Token> getActiveAccessToken(String toMemberId) {
        return client.getActiveAccessToken(toMemberId);
    }

    /**
     * Looks up a existing access token where the calling member is the grantor and given member is
     * the grantee.
     *
     * @param toMemberId beneficiary of the active access token
     * @return access token returned by the server
     */
    public Token getActiveAccessTokenBlocking(String toMemberId) {
        return getActiveAccessToken(toMemberId).blockingSingle();
    }

    /**
     * Looks up transfer tokens owned by the member.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return transfer tokens owned by the member
     */
    public Observable<PagedList<Token, String>> getTransferTokens(
            @Nullable String offset,
            int limit) {
        return client.getTokens(GetTokensRequest.Type.TRANSFER, offset, limit);
    }

    /**
     * Looks up tokens owned by the member.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return transfer tokens owned by the member
     */
    public PagedList<Token, String> getTransferTokensBlocking(@Nullable String offset, int limit) {
        return getTransferTokens(offset, limit).blockingSingle();
    }

    /**
     * Looks up access tokens owned by the member.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return access tokens owned by the member
     */
    public Observable<PagedList<Token, String>> getAccessTokens(
            @Nullable String offset,
            int limit) {
        return client.getTokens(GetTokensRequest.Type.ACCESS, offset, limit);
    }

    /**
     * Looks up tokens owned by the member.
     *
     * @param offset optional offset offset to start at
     * @param limit max number of records to return
     * @return access tokens owned by the member
     */
    public PagedList<Token, String> getAccessTokensBlocking(@Nullable String offset, int limit) {
        return getAccessTokens(offset, limit).blockingSingle();
    }

    /**
     * Looks up a existing token.
     *
     * @param tokenId token id
     * @return token returned by the server
     */
    public Observable<Token> getToken(String tokenId) {
        return client.getToken(tokenId);
    }

    /**
     * Looks up an existing token.
     *
     * @param tokenId token id
     * @return token returned by the server
     */
    public Token getTokenBlocking(String tokenId) {
        return getToken(tokenId).blockingSingle();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(Token token) {
        return redeemTokenInternal(token, null, null, null, null, null);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param refId transfer reference id
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(Token token, String refId) {
        return redeemTokenInternal(token, null, null, null, null, refId);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param destination transfer instruction destination
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(Token token, TransferDestination destination) {
        return redeemToken(token, null, null, null, destination, null);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param destination transfer instruction destination
     * @param refId transfer reference id
     * @return transfer record
     * @deprecated Use TransferDestination instead of TransferEndpoint.
     */
    public Observable<Transfer> redeemToken(
            Token token,
            TransferDestination destination,
            String refId) {
        return redeemToken(token, null, null, null, destination, refId);
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
    public Observable<Transfer> redeemToken(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description) {
        return redeemTokenInternal(token, amount, currency, description, null, null);
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
    public Observable<Transfer> redeemToken(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable TransferDestination destination) {
        return redeemToken(token, amount, currency, null, destination, null);
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
    public Observable<Transfer> redeemToken(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable TransferDestination destination) {
        return redeemToken(token, amount, currency, description, destination, null);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param amount transfer amount
     * @param currency transfer currency code, e.g. "EUR"
     * @param description transfer description
     * @param refId transfer reference id
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable String refId) {
        return redeemTokenInternal(token, amount, currency, description, null, refId);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param amount transfer amount
     * @param currency transfer currency code, e.g. "EUR"
     * @param description transfer description
     * @param destination the transfer instruction destination
     * @param refId transfer reference id
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable TransferDestination destination,
            @Nullable String refId) {
        if (!token.getPayload().hasTransfer()) {
            throw new IllegalArgumentException("Expected transfer token; found "
                    + token.getPayload().getBodyCase());
        }
        TransferPayload.Builder payload = TransferPayload.newBuilder()
                .setTokenId(token.getId())
                .setDescription(token
                        .getPayload()
                        .getDescription());

        if (destination != null) {
            payload.addTransferDestinations(destination);
        }
        if (amount != null) {
            payload.getAmountBuilder().setValue(Double.toString(amount));
        }
        if (currency != null) {
            payload.getAmountBuilder().setCurrency(currency);
        }
        if (description != null) {
            payload.setDescription(description);
        }
        if (refId != null) {
            payload.setRefId(refId);
        } else if (amount == null || BigDecimal.valueOf(amount)
                .equals(MoneyUtil.parseAmount(token.getPayload()
                        .getTransfer()
                        .getLifetimeAmount()))) {
            payload.setRefId(token.getPayload().getRefId());
        } else {
            logger.warn("refId is not set. A random ID will be used.");
            payload.setRefId(generateNonce());
        }

        return client.createTransfer(payload.build());
    }

    // Remove when deprecated TransferEndpoint methods are removed.
    private Observable<Transfer> redeemTokenInternal(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable TransferEndpoint destination,
            @Nullable String refId) {
        TransferPayload.Builder payload = TransferPayload.newBuilder()
                .setTokenId(token.getId())
                .setDescription(token
                        .getPayload()
                        .getDescription());

        if (destination != null) {
            payload.addDestinations(destination);
        }
        if (amount != null) {
            payload.getAmountBuilder().setValue(Double.toString(amount));
        }
        if (currency != null) {
            payload.getAmountBuilder().setCurrency(currency);
        }
        if (description != null) {
            payload.setDescription(description);
        }
        if (refId != null) {
            payload.setRefId(refId);
        } else if (amount == null || BigDecimal.valueOf(amount)
                .equals(MoneyUtil.parseAmount(token.getPayload()
                        .getTransfer()
                        .getLifetimeAmount()))) {
            payload.setRefId(token.getPayload().getRefId());
        } else {
            logger.warn("refId is not set. A random ID will be used.");
            payload.setRefId(generateNonce());
        }

        return client.createTransfer(payload.build());
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @return transfer record
     */
    public Transfer redeemTokenBlocking(Token token) {
        return redeemToken(token).blockingSingle();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param refId transfer reference id
     * @return transfer record
     */
    public Transfer redeemTokenBlocking(Token token, String refId) {
        return redeemToken(token, refId).blockingSingle();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param destination transfer instruction destination
     * @return transfer record
     */
    public Transfer redeemTokenBlocking(Token token, TransferDestination destination) {
        return redeemToken(token, destination).blockingSingle();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param destination transfer instruction destination
     * @param refId transfer reference id
     * @return transfer record
     */
    public Transfer redeemTokenBlocking(
            Token token,
            TransferDestination destination,
            String refId) {
        return redeemToken(token, destination, refId).blockingSingle();
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
    public Transfer redeemTokenBlocking(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description) {
        return redeemToken(token, amount, currency, description)
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
    public Transfer redeemTokenBlocking(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable TransferDestination destination) {
        return redeemToken(token, amount, currency, destination)
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
    public Transfer redeemTokenBlocking(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable TransferDestination destination) {
        return redeemToken(token, amount, currency, description, destination)
                .blockingSingle();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param amount transfer amount
     * @param currency transfer currency code, e.g. "EUR"
     * @param description transfer description
     * @param refId transfer reference id
     * @return transfer record
     */
    public Transfer redeemTokenBlocking(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable String refId) {
        return redeemToken(token, amount, currency, description, refId).blockingSingle();
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
    public Transfer redeemTokenBlocking(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable TransferDestination destination,
            @Nullable String refId) {
        return redeemToken(token, amount, currency, description, destination, refId)
                .blockingSingle();
    }

    /**
     * Redeems a bulk transfer token.
     *
     * @param tokenId ID of token to redeem
     * @return bulk transfer record
     */
    public Observable<BulkTransfer> redeemBulkTransferToken(String tokenId) {
        return client.createBulkTransfer(tokenId);
    }

    /**
     * Redeems a bulk transfer token.
     *
     * @param tokenId ID of token to redeem
     * @return bulk transfer record
     */
    public BulkTransfer redeemBulkTransferTokenBlocking(String tokenId) {
        return redeemBulkTransferToken(tokenId).blockingSingle();
    }

    /**
     * Redeems a standing order token.
     *
     * @param tokenId ID of token to redeem
     * @return standing order submission
     */
    public Observable<StandingOrderSubmission> redeemStandingOrderToken(String tokenId) {
        return client.createStandingOrder(tokenId);
    }

    /**
     * Redeems a standing order token.
     *
     * @param tokenId ID of token to redeem
     * @return standing order submission
     */
    public StandingOrderSubmission redeemStandingOrderTokenBlocking(String tokenId) {
        return redeemStandingOrderToken(tokenId).blockingSingle();
    }

    /**
     * Links accounts by navigating browser through bank authorization pages.
     * Returns empty list if the linking process is cancelled from the browser.
     *
     * @param bankId the bank id
     * @return observable list of linked accounts
     * @throws BankAuthorizationRequiredException if bank authorization payload
     *     is required to link accounts
     */
    public Observable<List<Account>> initiateAccountLinking(final String bankId)
            throws BankAuthorizationRequiredException {
        final String callbackUrl = String.format(
                "https://%s/auth/callback",
                getWebAppUrl(getTokenCluster()));
        final Browser browser = browserFactory.create();
        final Observable<List<Account>> accountLinkingObservable = browser.url()
                .map(url -> {
                    String accessToken = findFirstCapturingGroup(
                            url.toExternalForm(),
                            callbackUrl + "[/?]?.*#.*access_token=([^&]+).*");
                    if (accessToken != null) {
                        return accessToken;
                    }
                    String error = findFirstCapturingGroup(
                            url.toExternalForm(),
                            callbackUrl + "[/?]?.*error=([^&]+).*");
                    if (error != null) {
                        throw new Exception("Bank linking error: " + error);
                    }
                    browser.goTo(url);
                    return "";
                })
                .filter(accessToken -> !accessToken.isEmpty())
                .flatMap(accessToken -> linkAccounts(bankId, accessToken));

        return getBankInfo(bankId)
                .flatMap(bankInfo ->
                        Single.create((SingleOnSubscribe<List<Account>>) emitter -> {
                            accountLinkingObservable
                                    .subscribe(
                                            accounts -> {
                                                emitter.onSuccess(accounts);
                                                browser.close();
                                            },
                                            ex -> {
                                                emitter.onError(ex);
                                                browser.close();
                                            },
                                            () -> emitter.onSuccess(Collections.emptyList()));
                            String linkingUrl = bankInfo.getBankLinkingUri();
                            String url = String.format(
                                    "%s&redirect_uri=%s"
                                            // request BALANCE and TRANSACTION access on linking
                                            + "&resource=BALANCES&resource=TRANSACTIONS",
                                    linkingUrl,
                                    URLEncoder.encode(callbackUrl, "UTF-8"));
                            browser.goTo(new URL(url));
                        }).toObservable());
    }

    /**
     * Links accounts by navigating browser through bank authorization pages.
     *
     * @param bankId the bank id
     * @return list of linked accounts
     * @throws BankAuthorizationRequiredException if bank authorization payload
     *     is required to link accounts
     */
    public List<Account> initiateAccountLinkingBlocking(String bankId) {
        return initiateAccountLinking(bankId).blockingSingle();
    }

    /**
     * Links a funding bank accounts to Token and returns it to the caller.
     *
     * @param authorization an authorization to accounts, from the bank
     * @return list of linked accounts
     */
    public Observable<List<Account>> linkAccounts(
            BankAuthorization authorization) {
        return toAccountList(client.linkAccounts(authorization));
    }

    /**
     * Links funding bank accounts to Token and returns them to the caller.
     *
     * @param bankId bank id
     * @param accessToken OAuth access token
     * @return list of linked accounts
     * @throws BankAuthorizationRequiredException if bank authorization payload
     *     is required to link accounts
     */
    public Observable<List<Account>> linkAccounts(String bankId, String accessToken)
            throws BankAuthorizationRequiredException {
        return toAccountList(client.linkAccounts(OauthBankAuthorization.newBuilder()
                .setBankId(bankId)
                .setAccessToken(accessToken)
                .build()));
    }

    /**
     * Links a funding bank account to Token and returns it to the caller.
     *
     * @param authorization an authorization to accounts, from the bank
     * @return list of linked accounts
     */
    public List<Account> linkAccountsBlocking(BankAuthorization authorization) {
        return linkAccounts(authorization).blockingSingle();
    }

    /**
     * Links funding bank accounts to Token and returns them to the caller.
     *
     * @param bankId bank id
     * @param accessToken OAuth access token
     * @return list of linked accounts
     * @throws BankAuthorizationRequiredException if bank authorization payload
     *     is required to link accounts
     */
    public List<Account> linkAccountsBlocking(String bankId, String accessToken) {
        return linkAccounts(bankId, accessToken).blockingSingle();
    }

    /**
     * Unlinks bank accounts previously linked via linkAccounts call.
     *
     * @param accountIds account ids to unlink
     * @return nothing
     */
    public Completable unlinkAccounts(List<String> accountIds) {
        return client.unlinkAccounts(accountIds);
    }

    /**
     * Unlinks bank accounts previously linked via linkAccounts call.
     *
     * @param accountIds list of account ids to unlink
     */
    public void unlinkAccountsBlocking(List<String> accountIds) {
        unlinkAccounts(accountIds).blockingAwait();
    }

    /**
     * Looks up current account balance.
     *
     * @param accountId the account id
     * @param keyLevel key level
     * @return current balance
     */
    public Observable<Money> getCurrentBalance(String accountId, Key.Level keyLevel) {
        return client.getBalance(accountId, keyLevel).map(Balance::getCurrent);
    }

    /**
     * Looks up account current balance.
     *
     * @param accountId account id
     * @param keyLevel key level
     * @return balance
     */
    public Money getCurrentBalanceBlocking(String accountId, Key.Level keyLevel) {
        return getCurrentBalance(accountId, keyLevel).blockingSingle();
    }

    /**
     * Looks up available account balance.
     *
     * @param accountId the account id
     * @param keyLevel key level
     * @return available balance
     */
    public Observable<Money> getAvailableBalance(String accountId, Key.Level keyLevel) {
        return client.getBalance(accountId, keyLevel).map(Balance::getAvailable);
    }

    /**
     * Looks up account available balance.
     *
     * @param accountId account id
     * @param keyLevel key level
     * @return balance
     */
    public Money getAvailableBalanceBlocking(String accountId, Key.Level keyLevel) {
        return getAvailableBalance(accountId, keyLevel).blockingSingle();
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
                .flatMap(member -> {
                    List<String> toRemoveIds = new LinkedList<>();
                    for (Key key : member.getKeysList()) {
                        if (!storedKeys.contains(key)) {
                            toRemoveIds.add(key.getId());
                        }
                    }
                    return removeKeys(toRemoveIds).toObservable();
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
     * Replaces auth'd member's profile name.
     *
     * @param profileName profile name to set
     * @return updated profile
     */
    public Completable setProfileName(String profileName) {
        return client.setProfileName(profileName);
    }

    /**
     * Replaces the authenticated member's profile namex.
     *
     * @param profileName profile name
     */
    public void setProfileNameBlocking(String profileName) {
        setProfileName(profileName).blockingAwait();
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
     * Gets a list of the member's notifications.
     *
     * @param offset offset to start
     * @param limit how many notifications to get
     * @return list of notifications
     */
    public Observable<PagedList<Notification, String>> getNotifications(
            @Nullable String offset,
            int limit) {
        return client.getNotifications(offset, limit);
    }

    /**
     * Gets a list of the member's notifications.
     *
     * @param offset offset to start
     * @param limit how many notifications to get
     * @return list of notifications
     */
    public PagedList<Notification, String> getNotificationsBlocking(
            @Nullable String offset,
            int limit) {
        return getNotifications(offset, limit).blockingSingle();
    }

    /**
     * Gets a notification by id.
     *
     * @param notificationId Id of the notification
     * @return notification
     */
    public Observable<Notification> getNotification(String notificationId) {
        return client.getNotification(notificationId);
    }

    /**
     * Gets a notification by id.
     *
     * @param notificationId Id of the notification
     * @return notification
     */
    public Notification getNotificationBlocking(String notificationId) {
        return getNotification(notificationId).blockingSingle();
    }

    /**
     * Updates the status of a notification.
     *
     * @param notificationId the notification id to update
     * @param status the status to update
     * @return nothing
     */
    public Completable updateNotificationStatus(String notificationId, Status status) {
        return client.updateNotificationStatus(notificationId, status);
    }

    /**
     * Updates the status of a notification.
     *
     * @param notificationId the notification id to update
     * @param status the status to update
     */
    public void updateNotificationStatusBlocking(String notificationId, Status status) {
        updateNotificationStatus(notificationId, status).blockingAwait();
    }

    /**
     * Removes a subscriber.
     *
     * @param subscriberId subscriberId
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable unsubscribeFromNotifications(String subscriberId) {
        return client.unsubscribeFromNotifications(subscriberId);
    }

    /**
     * Removes a subscriber by Id.
     *
     * @param subscriberId subscriberId
     */
    public void unsubscribeFromNotificationsBlocking(String subscriberId) {
        unsubscribeFromNotifications(subscriberId).blockingAwait();
    }

    /**
     * Creates a subscriber to push notifications.
     *
     * @param handler specify the handler of the notifications
     * @param handlerInstructions map of instructions for the handler
     * @return subscriber Subscriber
     */
    public Observable<Subscriber> subscribeToNotifications(
            String handler,
            Map<String, String> handlerInstructions) {
        return client.subscribeToNotifications(handler, handlerInstructions);
    }

    /**
     * Subscribes a device to receive push notifications.
     *
     * @param handler specify the handler of the notifications
     * @return subscriber Subscriber
     */
    public Observable<Subscriber> subscribeToNotifications(String handler) {
        return subscribeToNotifications(handler, new HashMap<String, String>());
    }

    /**
     * Subscribes a device to receive push notifications.
     *
     * @param handler specify the handler of the notifications
     * @param handlerInstructions map of instructions for the handler
     * @return subscriber Subscriber
     */
    public Subscriber subscribeToNotificationsBlocking(
            String handler,
            Map<String, String> handlerInstructions) {
        return subscribeToNotifications(handler, handlerInstructions).blockingSingle();
    }

    /**
     * Subscribes a device to receive push notifications.
     *
     * @param handler specify the handler of the notifications
     * @return subscriber Subscriber
     */
    public Subscriber subscribeToNotificationsBlocking(String handler) {
        return subscribeToNotifications(handler).blockingSingle();
    }

    /**
     * Gets subscribers.
     *
     * @return subscribers
     */
    public Observable<List<Subscriber>> getSubscribers() {
        return client.getSubscribers();
    }

    /**
     * Gets a list of all subscribers.
     *
     * @return subscribers Subscribers
     */
    public List<Subscriber> getSubscribersBlocking() {
        return getSubscribers().blockingSingle();
    }

    /**
     * Gets a subscriber by id.
     *
     * @param subscriberId Id of the subscriber
     * @return subscriber
     */
    public Observable<Subscriber> getSubscriber(String subscriberId) {
        return client.getSubscriber(subscriberId);
    }

    /**
     * Gets a subscriber by Id.
     *
     * @param subscriberId subscriberId
     * @return subscribers Subscribers
     */
    public Subscriber getSubscriberBlocking(String subscriberId) {
        return getSubscriber(subscriberId).blockingSingle();
    }

    /**
     * Sign with a Token signature a token request state payload.
     *
     * @param tokenRequestId token request id
     * @param tokenId token id
     * @param state state
     * @return signature
     */
    public Observable<Signature> signTokenRequestState(
            String tokenRequestId,
            String tokenId,
            String state) {
        return client.signTokenRequestState(tokenRequestId, tokenId, state);
    }

    /**
     * Sign with a Token signature a token request state payload.
     *
     * @param tokenRequestId token request id
     * @param tokenId token id
     * @param state state
     * @return signature
     */
    public Signature signTokenRequestStateBlocking(
            String tokenRequestId,
            String tokenId,
            String state) {
        return signTokenRequestState(tokenRequestId, tokenId, state).blockingSingle();
    }

    /**
     * Stores a linking request.
     *
     * @param callbackUrl callback URL
     * @param tokenRequestId token request ID
     * @return linking request ID
     */
    public Observable<String> storeLinkingRequest(
            String callbackUrl,
            String tokenRequestId) {
        return client.storeLinkingRequest(callbackUrl, tokenRequestId);
    }

    /**
     * Stores a linking request.
     *
     * @param callbackUrl callback URL
     * @param tokenRequestId token request ID
     * @return linking request ID
     */
    public String storeLinkingRequestBlocking(
            String callbackUrl,
            String tokenRequestId) {
        return storeLinkingRequest(callbackUrl, tokenRequestId).blockingSingle();
    }

    /**
     * Apply SCA for the given list of account IDs.
     *
     * @param accountIds list of account ids
     * @return completable
     */
    public Completable applySca(List<String> accountIds) {
        return client.applySca(accountIds);
    }

    /**
     * Apply SCA for the given list of account IDs.
     *
     * @param accountIds list of account ids
     */
    public void applyScaBlocking(List<String> accountIds) {
        applySca(accountIds).blockingAwait();
    }

    /**
     * Creates a test bank account in a fake bank and links the account.
     *
     * @param balance account balance to set
     * @param currency currency code, e.g. "EUR"
     * @return the linked account
     */
    public Observable<Account> createTestBankAccount(double balance, String currency) {
        return createTestBankAccountImpl(balance, currency)
                .map(acc -> new Account(acc, client, Member.this));
    }

    /**
     * Creates a test bank account in a fake bank and links the account.
     *
     * @param balance account balance to set
     * @param currency currency code, e.g. "EUR"
     * @return the linked account
     */
    public Account createTestBankAccountBlocking(double balance, String currency) {
        return createTestBankAccount(balance, currency).blockingSingle();
    }

    private Observable<List<Account>> toAccountList(
            Observable<List<AccountProtos.Account>> accounts) {
        return accounts.map(acc -> {
            List<Account> result = new LinkedList<>();
            for (AccountProtos.Account account : acc) {
                result.add(new Account(Member.this, account, client));
            }
            return result;
        });
    }

    private Observable<Account> toAccount(Observable<AccountProtos.Account> account) {
        return account
                .map(acc -> new Account(Member.this, acc, client));
    }

    /**
     * Sets the app's callback url.
     *
     * @param appCallbackUrl the app callback url to set
     * @return completable
     */
    public Completable setAppCallbackUrl(String appCallbackUrl) {
        return client.setAppCallbackUrl(appCallbackUrl);
    }

    /**
     * Sets the app's callback url.
     *
     * @param appCallbackUrl the app callback url to set
     */
    public void setAppCallbackUrlBlocking(String appCallbackUrl) {
        client.setAppCallbackUrl(appCallbackUrl).blockingAwait();
    }
}
