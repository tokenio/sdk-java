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

package io.token.tpp;

import static io.token.proto.common.blob.BlobProtos.Blob.AccessMode.PUBLIC;
import static io.token.proto.gateway.Gateway.GetTokensRequest.Type.ACCESS;
import static io.token.proto.gateway.Gateway.GetTokensRequest.Type.TRANSFER;
import static io.token.util.Util.generateNonce;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.TokenClient.TokenCluster;
import io.token.proto.PagedList;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.blob.BlobProtos.Blob.Payload;
import io.token.proto.common.eidas.EidasProtos.VerifyEidasPayload;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.CustomerTrackingMetadata;
import io.token.proto.common.submission.SubmissionProtos.StandingOrderSubmission;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.transfer.TransferProtos;
import io.token.proto.common.transfer.TransferProtos.BulkTransfer;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.proto.gateway.Gateway.GetEidasVerificationStatusResponse;
import io.token.proto.gateway.Gateway.VerifyEidasResponse;
import io.token.tokenrequest.TokenRequest;
import io.token.tpp.rpc.Client;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Member in the Token system. Each member has an active secret
 * and public key pair that is used to perform authentication.
 */
public class Member extends io.token.Member implements Representable {
    private static final Logger logger = LoggerFactory.getLogger(Member.class);
    private final Client client;

    /**
     * Creates an instance of {@link Member}.
     *
     * @param memberId member ID
     * @param partnerId member ID of partner
     * @param client RPC client used to perform operations against the server
     * @param cluster Token cluster, e.g. sandbox, production
     */
    Member(
            String memberId,
            @Nullable String partnerId,
            @Nullable String realmId,
            Client client,
            TokenCluster cluster) {
        super(memberId, partnerId, realmId, client, cluster);
        this.client = client;
    }

    /**
     * Replaces auth'd member's public profile.
     *
     * @param profile profile to set
     * @return updated profile
     */
    public Observable<MemberProtos.Profile> setProfile(MemberProtos.Profile profile) {
        return client.setProfile(profile);
    }

    /**
     * Replaces the authenticated member's public profile.
     *
     * @param profile Profile to set
     * @return updated profile
     */
    public MemberProtos.Profile setProfileBlocking(MemberProtos.Profile profile) {
        return setProfile(profile).blockingSingle();
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
     * Looks up funding bank accounts linked to Token.
     *
     * @return list of accounts
     */
    public Observable<List<Account>> getAccounts() {
        return getAccountsImpl()
                .map(new Function<List<io.token.Account>, List<Account>>() {
                    @Override
                    public List<Account> apply(List<io.token.Account> accs) {
                        List<Account> accounts = Lists.newArrayList();
                        for (io.token.Account acc : accs) {
                            accounts.add(new Account(acc, Member.this));
                        }
                        return accounts;
                    }
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
                .map(new Function<io.token.Account, Account>() {
                    @Override
                    public Account apply(io.token.Account acc) throws Exception {
                        return new Account(acc, Member.this);
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
     * Retrieves a blob from the server.
     *
     * @param blobId id of the blob
     * @return Blob
     */
    public Observable<Blob> getBlob(String blobId) {
        return client.getBlob(blobId);
    }

    /**
     * Retrieves a blob from the server.
     *
     * @param blobId id of the blob
     * @return Blob
     */
    public Blob getBlobBlocking(String blobId) {
        return getBlob(blobId).blockingSingle();
    }

    /**
     * Creates a {@link Representable} that acts as another member using the access token
     * that was granted by that member.
     *
     * @param tokenId the token id
     * @return the {@link Representable}
     */
    public Representable forAccessToken(String tokenId) {
        return forAccessToken(tokenId, false);
    }

    /**
     * Creates a {@link Representable} that acts as another member using the access token
     * that was granted by that member.
     *
     * @param tokenId the token id
     * @param customerInitiated whether the call is initiated by the customer
     * @return the {@link Representable}
     */
    public Representable forAccessToken(String tokenId, boolean customerInitiated) {
        Client cloned = client.forAccessToken(tokenId, customerInitiated);
        return new Member(memberId, partnerId, realmId, cloned, cluster);
    }

    public Representable forAccessToken(
            String tokenId,
            CustomerTrackingMetadata customerTrackingMetadata) {
        Client cloned = client.forAccessToken(tokenId, customerTrackingMetadata);
        return new Member(memberId, partnerId, realmId, cloned, cluster);
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
     * @return transfer record
     */
    @Deprecated
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
     * @param destination transfer instruction destination
     * @param refId transfer reference id
     * @return transfer record
     * @deprecated Use TransferDestination instead of TransferEndpoint.
     */
    @Deprecated
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
     * @param destination transfer instruction destination
     * @return transfer record
     * @deprecated Use TransferDestination instead of TransferEndpoint.
     */
    @Deprecated
    public Observable<Transfer> redeemToken(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable TransferEndpoint destination) {
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
     * @param destination transfer instruction destination
     * @return transfer record
     * @deprecated Use TransferDestination instead of TransferEndpoint.
     */
    @Deprecated
    public Observable<Transfer> redeemToken(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable TransferEndpoint destination) {
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
        TransferProtos.TransferPayload.Builder payload = TransferProtos.TransferPayload.newBuilder()
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
        } else if (!token.getPayload().getRefId().isEmpty() && amount == null) {
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
     * @param amount transfer amount
     * @param currency transfer currency code, e.g. "EUR"
     * @param description transfer description
     * @param destination the transfer instruction destination
     * @param refId transfer reference id
     * @return transfer record
     * @deprecated Use TransferDestination instead of TransferEndpoint.
     */
    @Deprecated
    public Observable<Transfer> redeemToken(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable TransferEndpoint destination,
            @Nullable String refId) {
        return redeemTokenInternal(token, amount, currency, description, destination, refId);
    }

    // Remove when deprecated TransferEndpoint methods are removed.
    @Deprecated
    private Observable<Transfer> redeemTokenInternal(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable TransferEndpoint destination,
            @Nullable String refId) {
        TransferProtos.TransferPayload.Builder payload = TransferProtos.TransferPayload.newBuilder()
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
        } else if (!token.getPayload().getRefId().isEmpty() && amount == null) {
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
     * @return transfer record
     * @deprecated Use TransferDestination instead of TransferEndpoint.
     */
    @Deprecated
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
     * @param destination transfer instruction destination
     * @param refId transfer reference id
     * @return transfer record
     * @deprecated Use TransferDestination instead of TransferEndpoint.
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
     * @param destination transfer instruction destination
     * @return transfer record
     * @deprecated Use TransferDestination instead of TransferEndpoint.
     */
    @Deprecated
    public Transfer redeemTokenBlocking(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable TransferEndpoint destination) {
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
     * @param destination transfer instruction destination
     * @return transfer record
     * @deprecated Use TransferDestination instead of TransferEndpoint.
     */
    @Deprecated
    public Transfer redeemTokenBlocking(
            Token token,
            @Nullable Double amount,
            @Nullable String currency,
            @Nullable String description,
            @Nullable TransferEndpoint destination) {
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
     * Redeems a transfer token.
     *
     * @param token transfer token to redeem
     * @param amount transfer amount
     * @param currency transfer currency code, e.g. "EUR"
     * @param description transfer description
     * @param destination transfer instruction destination
     * @param refId transfer reference id
     * @return transfer record
     * @deprecated Use TransferDestination instead of TransferEndpoint.
     */
    @Deprecated
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
     * Stores a token request. This can be retrieved later by the token request id.
     *
     * @param tokenRequest token request
     * @return token request id
     */
    public Observable<String> storeTokenRequest(TokenRequest tokenRequest) {
        return client.storeTokenRequest(
                tokenRequest.getTokenRequestPayload(),
                tokenRequest.getTokenRequestOptions());
    }

    /**
     * Stores a token request to be retrieved later (possibly by another member).
     *
     * @param tokenRequest token request
     * @return ID to reference the stored token request
     */
    public String storeTokenRequestBlocking(TokenRequest tokenRequest) {
        return storeTokenRequest(tokenRequest).blockingSingle();
    }

    /**
     * Sets destination account for once if it hasn't been set.
     *
     * @param tokenRequestId token request Id
     * @param transferDestinations destination account
     * @return observable that completes when request handled
     */
    public Completable setTokenRequestTransferDestinations(
            String tokenRequestId,
            List<TransferDestination> transferDestinations) {
        return client.setTokenRequestTransferDestinations(tokenRequestId, transferDestinations);
    }

    /**
     * Sets destination account for once if it hasn't been set.
     *
     * @param tokenRequestId token request Id
     * @param transferDestinations destination account
     */
    public void setTokenRequestTransferDestinationsBlocking(
            String tokenRequestId,
            List<TransferDestination> transferDestinations) {
        setTokenRequestTransferDestinations(tokenRequestId, transferDestinations).blockingAwait();
    }

    /**
     * Creates a customization.
     *
     * @param logo logo
     * @param colors map of ARGB colors #AARRGGBB
     * @param consentText consent text
     * @param name display name
     * @param appName corresponding app name
     * @return customization id
     */
    public Observable<String> createCustomization(
            Payload logo,
            Map<String, String> colors,
            String consentText,
            String name,
            String appName) {
        return client.createCustomization(logo, colors, consentText, name, appName);
    }

    /**
     * Creates a customization.
     *
     * @param logo logo
     * @param colors map of ARGB colors #AARRGGBB
     * @param consentText consent text
     * @param name display name
     * @param appName corresponding app name
     * @return customization id
     */
    public String createCustomizationBlocking(
            Payload logo,
            Map<String, String> colors,
            String consentText,
            String name,
            String appName) {
        return createCustomization(logo, colors, consentText, name, appName).blockingSingle();
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
     * Looks up access tokens owned by the member.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return access tokens owned by the member
     */
    public Observable<PagedList<Token, String>> getAccessTokens(
            @Nullable String offset,
            int limit) {
        return client.getTokens(ACCESS, offset, limit);
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
     * Looks up transfer tokens owned by the member.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return transfer tokens owned by the member
     */
    public Observable<PagedList<Token, String>> getTransferTokens(
            @Nullable String offset,
            int limit) {
        return client.getTokens(TRANSFER, offset, limit);
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

    /**
     * Trigger a step up notification for transaction requests.
     *
     * @param accountId account id
     * @return notification status
     */
    public Observable<NotifyStatus> triggerTransactionStepUpNotification(String accountId) {
        return client.triggerTransactionStepUpNotification(accountId);
    }

    /**
     * Trigger a step up notification for transaction requests.
     *
     * @param accountId account id
     * @return notification status
     */
    public NotifyStatus triggerTransactionStepUpNotificationBlocking(String accountId) {
        return triggerTransactionStepUpNotification(accountId).blockingSingle();
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
                .map(acc -> new Account(acc, Member.this));
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

    /**
     * Verifies eIDAS alias with an eIDAS certificate, containing auth number equal to the value
     * of the alias. Before making this call make sure that:<ul>
     * <li>The member is under the realm of a bank (the one tpp tries to gain access to)</li>
     * <li>An eIDAS-type alias with the value equal to auth number of the TPP is added
     * to the member</li>
     * <li>The realmId of the alias is equal to the member's realmId</li>
     * </ul>
     *
     * @param payload payload containing the member id and the base64 encoded eIDAS certificate
     * @param signature the payload signed with a private key corresponding to the certificate
     * @return a result of the verification process
     */
    public Observable<VerifyEidasResponse> verifyEidas(
            VerifyEidasPayload payload,
            String signature) {
        return client.verifyEidas(payload, signature);
    }

    /**
     * Retrieves an eIDAS verification status by verificationId.
     *
     * @param verificationId verification id
     * @return a status of the verification operation together with the certificate and alias value
     */
    public Observable<GetEidasVerificationStatusResponse> getEidasVerificationStatus(
            String verificationId) {
        return client.getEidasVerificationStatus(verificationId);
    }

    /**
     * Get url to bank authorization page for a token request.
     *
     * @param bankId bank ID
     * @param tokenRequestId token request ID
     * @return url
     */
    public Observable<String> getBankAuthUrl(String bankId, String tokenRequestId) {
        return client.getBankAuthUrl(bankId, tokenRequestId);
    }

    /**
     * Get url to bank authorization page for a token request.
     *
     * @param bankId bank ID
     * @param tokenRequestId token request ID
     * @return url
     */
    public String getBankAuthUrlBlocking(String bankId, String tokenRequestId) {
        return getBankAuthUrl(bankId, tokenRequestId).blockingSingle();
    }

    /**
     * Forward the callback from the bank (after user authentication) to Token.
     *
     * @param bankId bank ID
     * @param query HTTP query string
     * @return token request ID
     */
    public Observable<String> onBankAuthCallback(String bankId, String query) {
        return client.onBankAuthCallback(bankId, query);
    }

    /**
     * Forward the callback from the bank (after user authentication) to Token.
     *
     * @param bankId bank ID
     * @param query HTTP query string
     * @return token request ID
     */
    public String onBankAuthCallbackBlocking(String bankId, String query) {
        return onBankAuthCallback(bankId, query).blockingSingle();
    }

    /**
     * Get the raw consent from the bank associated with a token.
     *
     * @param tokenId token ID
     * @return raw consent
     */
    public Observable<String> getRawConsent(String tokenId) {
        return client.getRawConsent(tokenId);
    }

    /**
     * Get the raw consent from the bank associated with a token.
     *
     * @param tokenId token ID
     * @return raw consent
     */
    public String getRawConsentBlocking(String tokenId) {
        return getRawConsent(tokenId).blockingSingle();
    }
}
