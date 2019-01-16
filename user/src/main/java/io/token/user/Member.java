/**
 * Copyright (c) 2018 Token, Inc.
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

import static io.token.proto.common.blob.BlobProtos.Blob.AccessMode.PUBLIC;
import static io.token.user.util.Util.getWebAppUrl;
import static io.token.user.util.Util.parseOauthAccessToken;
import static io.token.util.Util.generateNonce;

import com.google.protobuf.ByteString;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.token.TokenClient.TokenCluster;
import io.token.proto.PagedList;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.banklink.Banklink.OauthBankAuthorization;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.bank.BankProtos.BankInfo;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.blob.BlobProtos.Blob.Payload;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.member.MemberProtos.ProfilePictureSize;
import io.token.proto.common.member.MemberProtos.ReceiptContact;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.notification.NotificationProtos.Notification;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.subscriber.SubscriberProtos.Subscriber;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.transaction.TransactionProtos.Balance;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transfer.TransferProtos.TransferPayload;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.proto.gateway.Gateway.GetTokensRequest;
import io.token.user.rpc.Client;
import io.token.user.browser.Browser;
import io.token.user.browser.BrowserFactory;
import io.token.user.exceptions.BankAuthorizationRequiredException;

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
     * @param member internal member representation, fetched from server
     * @param client RPC client used to perform operations against the server
     * @param cluster Token cluster, e.g. sandbox, production
     * @param browserFactory browser factory for displaying UI for linking
     */
    Member(
            MemberProtos.Member member,
            Client client,
            TokenCluster cluster,
            BrowserFactory browserFactory) {
        super(member, client, cluster);
        this.client = client;
        this.browserFactory = browserFactory;
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
     * Get the default bank account for this member.
     *
     * @param memberId the member's id
     * @return observable string
     */
    public Observable<Account> getDefaultAccount(String memberId) {
        return client
                .getDefaultAccount(memberId)
                .map(new Function<AccountProtos.Account, Account>() {
                    public Account apply(AccountProtos.Account account) {
                        return new Account(Member.this, account, client);
                    }
                });
    }

    /**
     * Gets the default bank account.
     *
     * @return the default bank account id
     */
    public Account getDefaultAccount() {
        return getDefaultAccount(this.memberId()).blockingSingle();
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
     * Creates a new transfer token builder.
     *
     * @param amount transfer amount
     * @param currency currency code, e.g. "USD"
     * @return transfer token returned by the server
     */
    public TransferTokenBuilder createTransferToken(double amount, String currency) {
        return new TransferTokenBuilder(this, amount, currency);
    }

    /**
     * Creates a new transfer token from a token payload.
     *
     * @param payload transfer token payload
     * @return transfer token returned by the server
     */
    public Observable<Token> createTransferToken(TokenPayload payload) {
        return client.createTransferToken(payload);
    }

    /**
     * Creates a new transfer token from a token payload.
     *
     * @param payload transfer token payload
     * @param tokenRequestId token request id
     * @return transfer token returned by the server
     */
    public Observable<Token> createTransferToken(TokenPayload payload, String tokenRequestId) {
        return client.createTransferToken(payload, tokenRequestId);
    }

    /**
     * Creates a new transfer token from a token payload.
     *
     * @param payload transfer token payload
     * @return transfer token returned by the server
     */
    public Token createTransferTokenBlocking(TokenPayload payload) {
        return createTransferToken(payload).blockingSingle();
    }

    /**
     * Creates a new transfer token from a token payload.
     *
     * @param payload transfer token payload
     * @param tokenRequestId token request id
     * @return transfer token returned by the server
     */
    public Token createTransferTokenBlocking(TokenPayload payload, String tokenRequestId) {
        return createTransferToken(payload, tokenRequestId).blockingSingle();
    }

    /**
     * Creates an access token built from a given {@link AccessTokenBuilder}.
     *
     * @param accessTokenBuilder an {@link AccessTokenBuilder} to create access token from
     * @return the access token created
     */
    public Observable<Token> createAccessToken(AccessTokenBuilder accessTokenBuilder) {
        return client.createAccessToken(accessTokenBuilder.from(memberId()).build());
    }

    /**
     * Creates an access token built from a given {@link AccessTokenBuilder}.
     *
     * @param accessTokenBuilder an {@link AccessTokenBuilder} to create access token from
     * @param tokenRequestId token request id
     * @return the access token created
     */
    public Observable<Token> createAccessToken(
            AccessTokenBuilder accessTokenBuilder,
            String tokenRequestId) {
        return client.createAccessToken(
                accessTokenBuilder.from(memberId()).build(),
                tokenRequestId);
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
     * Creates an access token built from a given {@link AccessTokenBuilder}.
     *
     * @param accessTokenBuilder an {@link AccessTokenBuilder} to create access token from
     * @param tokenRequestId token request id
     * @return the access token created
     */
    public Token createAccessTokenBlocking(
            AccessTokenBuilder accessTokenBuilder,
            String tokenRequestId) {
        return createAccessToken(accessTokenBuilder, tokenRequestId).blockingSingle();
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
     * Looks up transfer tokens owned by the member.git st
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
     * @return transfer tokens owned by the member
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
     * @return transfer tokens owned by the member
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
     * Retrieves a blob that is attached to a transfer token.
     *
     * @param tokenId id of the token
     * @param blobId id of the blob
     * @return Blob
     */
    public Observable<Blob> getTokenBlob(String tokenId, String blobId) {
        return client.getTokenBlob(tokenId, blobId);
    }

    /**
     * Retrieves a blob that is attached to a token.
     *
     * @param tokenId id of the token
     * @param blobId id of the blob
     * @return Blob
     */
    public Blob getTokenBlobBlocking(String tokenId, String blobId) {
        return getTokenBlob(tokenId, blobId).blockingSingle();
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(Token token) {
        return redeemToken(token, null, null, null, null, null);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param refId transfer reference id
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(Token token, String refId) {
        return redeemToken(token, null, null, null, null, refId);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param destination transfer instruction destination
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(Token token, TransferEndpoint destination) {
        return redeemToken(token, null, null, null, destination, null);
    }

    /**
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param destination transfer instruction destination
     * @param refId transfer reference id
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(
            Token token,
            TransferEndpoint destination,
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
     * @param destination the transfer instruction destination
     * @param refId transfer reference id
     * @return transfer record
     */
    public Observable<Transfer> redeemToken(
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
    public Transfer redeemTokenBlocking(Token token, TransferEndpoint destination) {
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
    public Transfer redeemTokenBlocking(Token token, TransferEndpoint destination, String refId) {
        return redeemToken(token, destination, refId).blockingSingle();
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
            @Nullable TransferEndpoint destination,
            @Nullable String refId) {
        return redeemToken(token, amount, currency, description, destination, refId)
                .blockingSingle();
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
                .filter(new Predicate<URL>() {
                    @Override
                    public boolean test(URL url) {
                        if (url.toExternalForm().matches(
                                callbackUrl + "([/?]?.*#).*access_token=.+")) {
                            return true;
                        }
                        browser.goTo(url);
                        return false;
                    }
                })
                .flatMap(new Function<URL, Observable<List<Account>>>() {
                    @Override
                    public Observable<List<Account>> apply(URL url) {
                        String accessToken = parseOauthAccessToken(url.toExternalForm());
                        if (accessToken == null) {
                            throw new IllegalArgumentException("No access token found");
                        }
                        return linkAccounts(bankId, accessToken);
                    }
                });

        return getBankInfo(bankId)
                .flatMap(new Function<BankInfo, ObservableSource<List<Account>>>() {
                    @Override
                    public ObservableSource<List<Account>> apply(final BankInfo bankInfo) {
                        return Single.create(new SingleOnSubscribe<List<Account>>() {
                            @Override
                            public void subscribe(final SingleEmitter<List<Account>> emitter)
                                    throws Exception {
                                accountLinkingObservable
                                        .subscribe(
                                                new Consumer<List<Account>>() {
                                                    @Override
                                                    public void accept(List<Account> accounts) {
                                                        emitter.onSuccess(accounts);
                                                        browser.close();
                                                    }
                                                },
                                                new Consumer<Throwable>() {
                                                    @Override
                                                    public void accept(Throwable ex) {
                                                        emitter.onError(ex);
                                                        browser.close();
                                                    }
                                                },
                                                new Action() {
                                                    @Override
                                                    public void run() {
                                                        emitter.onSuccess(Collections.EMPTY_LIST);
                                                    }
                                                });
                                String linkingUrl = bankInfo.getBankLinkingUri();
                                String url = String.format(
                                        "%s&redirect_uri=%s",
                                        linkingUrl,
                                        URLEncoder.encode(callbackUrl, "UTF-8"));
                                browser.goTo(new URL(url));
                            }
                        }).toObservable();
                    }
                });
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
        return client.getBalance(accountId, keyLevel).map(new Function<Balance, Money>() {
            @Override
            public Money apply(Balance balance) throws Exception {
                return balance.getCurrent();
            }
        });
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
        return client.getBalance(accountId, keyLevel).map(new Function<Balance, Money>() {
            @Override
            public Money apply(Balance balance) throws Exception {
                return balance.getAvailable();
            }
        });
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
     * Creates a test bank account in a fake bank.
     *
     * @param balance account balance to set
     * @param currency currency code, e.g. "EUR"
     * @return OAuth bank authorization
     */
    public Observable<OauthBankAuthorization> createTestBankAccount(
            double balance, String currency) {
        return client.createTestBankAccount(Money.newBuilder()
                .setCurrency(currency)
                .setValue(Double.toString(balance))
                .build());
    }

    /**
     * Creates a test bank account in a fake bank.
     *
     * @param balance account balance to set
     * @param currency currency code, e.g. "EUR"
     * @return OAuth bank authorization
     */
    public OauthBankAuthorization createTestBankAccountBlocking(double balance, String currency) {
        return createTestBankAccount(balance, currency).blockingSingle();
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
     * Removes a subscriber.
     *
     * @param subscriberId subscriberId
     * @return completable that indicates whether the operation finished or had an error
     */
    public Completable unsubscribeFromNotifications(String subscriberId) {
        return client
                .unsubscribeFromNotifications(subscriberId);
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

    private Observable<List<Account>> toAccountList(
            Observable<List<AccountProtos.Account>> accounts) {
        return accounts.map(new Function<List<AccountProtos.Account>, List<Account>>() {
            @Override
            public List<Account> apply(List<AccountProtos.Account> accounts) {
                List<Account> result = new LinkedList<>();
                for (AccountProtos.Account account : accounts) {
                    result.add(new Account(Member.this, account, client));
                }
                return result;
            }
        });
    }

    private Observable<Account> toAccount(Observable<AccountProtos.Account> account) {
        return account
                .map(new Function<AccountProtos.Account, Account>() {
                    @Override
                    public Account apply(AccountProtos.Account account) {
                        return new Account(Member.this, account, client);
                    }
                });
    }
}