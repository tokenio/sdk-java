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

package io.token.tpp.rpc;

import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.CANCELLED;
import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.ENDORSED;
import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.SUCCESS;
import static io.token.rpc.util.Converters.toCompletable;
import static io.token.util.Util.toObservable;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.proto.PagedList;
import io.token.proto.common.blob.BlobProtos;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.SecurityMetadata;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TokenRequestOptions;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload;
import io.token.proto.common.transfer.TransferProtos;
import io.token.proto.common.transfer.TransferProtos.Transfer;
import io.token.proto.gateway.Gateway.CancelTokenRequest;
import io.token.proto.gateway.Gateway.CancelTokenResponse;
import io.token.proto.gateway.Gateway.CreateAccessTokenRequest;
import io.token.proto.gateway.Gateway.CreateAccessTokenResponse;
import io.token.proto.gateway.Gateway.CreateBlobRequest;
import io.token.proto.gateway.Gateway.CreateBlobResponse;
import io.token.proto.gateway.Gateway.CreateCustomizationRequest;
import io.token.proto.gateway.Gateway.CreateCustomizationResponse;
import io.token.proto.gateway.Gateway.CreateTransferRequest;
import io.token.proto.gateway.Gateway.CreateTransferResponse;
import io.token.proto.gateway.Gateway.CreateTransferTokenRequest;
import io.token.proto.gateway.Gateway.CreateTransferTokenResponse;
import io.token.proto.gateway.Gateway.EndorseTokenRequest;
import io.token.proto.gateway.Gateway.EndorseTokenResponse;
import io.token.proto.gateway.Gateway.GetActiveAccessTokenRequest;
import io.token.proto.gateway.Gateway.GetActiveAccessTokenResponse;
import io.token.proto.gateway.Gateway.GetBlobRequest;
import io.token.proto.gateway.Gateway.GetBlobResponse;
import io.token.proto.gateway.Gateway.GetTokenBlobRequest;
import io.token.proto.gateway.Gateway.GetTokenBlobResponse;
import io.token.proto.gateway.Gateway.GetTokenRequest;
import io.token.proto.gateway.Gateway.GetTokenResponse;
import io.token.proto.gateway.Gateway.GetTokensRequest;
import io.token.proto.gateway.Gateway.GetTokensResponse;
import io.token.proto.gateway.Gateway.GetTransferRequest;
import io.token.proto.gateway.Gateway.GetTransferResponse;
import io.token.proto.gateway.Gateway.GetTransfersRequest;
import io.token.proto.gateway.Gateway.GetTransfersResponse;
import io.token.proto.gateway.Gateway.StoreTokenRequestRequest;
import io.token.proto.gateway.Gateway.StoreTokenRequestResponse;
import io.token.proto.gateway.Gateway.UpdateTokenRequestRequest;
import io.token.rpc.GatewayProvider;
import io.token.security.CryptoEngine;
import io.token.security.Signer;

import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * An authenticated RPC client that is used to talk to Token gateway. The
 * class is a thin wrapper on top of gRPC generated client. Makes the API
 * easier to use.
 */
public final class Client extends io.token.rpc.Client {
    private SecurityMetadata securityMetadata = SecurityMetadata.getDefaultInstance();
    private String onBehalfOf;

    /**
     * Creates a client instance.
     *
     * @param memberId member id
     * @param crypto the crypto engine used to sign for authentication, request payloads, etc
     * @param gateway gateway gRPC stub
     */
    Client(
            String memberId,
            CryptoEngine crypto,
            GatewayProvider gateway) {
        super(memberId, crypto, gateway);
    }

    /**
     * Creates and uploads a blob.
     *
     * @param payload payload of the blob
     * @return id of the blob
     */
    public Observable<String> createBlob(BlobProtos.Blob.Payload payload) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createBlob(CreateBlobRequest
                        .newBuilder()
                        .setPayload(payload)
                        .build()))
                .map(new Function<CreateBlobResponse, String>() {
                    public String apply(CreateBlobResponse response) {
                        return response.getBlobId();
                    }
                });
    }

    /**
     * Retrieves a blob from the server.
     *
     * @param blobId id of the blob
     * @return Blob
     */
    public Observable<BlobProtos.Blob> getBlob(String blobId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getBlob(GetBlobRequest
                        .newBuilder()
                        .setBlobId(blobId)
                        .build()))
                .map(new Function<GetBlobResponse, BlobProtos.Blob>() {
                    public BlobProtos.Blob apply(GetBlobResponse response) {
                        return response.getBlob();
                    }
                });
    }

    /**
     * Creates a new instance with On-Behalf-Of authentication set.
     *
     * @param tokenId access token ID to be used
     * @param customerInitiated whether the customer initiated the calls
     * @return new client instance
     */
    public Client forAccessToken(String tokenId, boolean customerInitiated) {
        Client updated = new Client(memberId, crypto, gateway);
        updated.useAccessToken(tokenId, customerInitiated);
        updated.setSecurityMetadata(securityMetadata);
        return updated;
    }

    /**
     * Sets the On-Behalf-Of authentication value to be used
     * with this client.  The value must correspond to an existing
     * Access Token ID issued for the client member. Sets customer initiated
     * to false.
     *
     * @param accessTokenId the access token id to be used
     */
    public void useAccessToken(String accessTokenId) {
        useAccessToken(accessTokenId, false);
    }

    /**
     * Sets the On-Behalf-Of authentication value to be used
     * with this client.  The value must correspond to an existing
     * Access Token ID issued for the client member. Uses the given customer
     * initiated flag.
     *
     * @param accessTokenId the access token id to be used
     * @param customerInitiated whether the customer initiated the calls
     */
    public void useAccessToken(String accessTokenId, boolean customerInitiated) {
        this.onBehalfOf = accessTokenId;
        this.customerInitiated = customerInitiated;
    }

    /**
     * Stores a token request.
     *
     * @param payload token request payload
     * @param options token request options
     * @return reference id for token request
     */
    public Observable<String> storeTokenRequest(
            TokenRequestPayload payload,
            TokenRequestOptions options) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .storeTokenRequest(StoreTokenRequestRequest.newBuilder()
                        .setRequestPayload(payload)
                        .setRequestOptions(options)
                        .build()))
                .map(new Function<StoreTokenRequestResponse, String>() {
                    @Override
                    public String apply(StoreTokenRequestResponse storeTokenRequestResponse) {
                        return storeTokenRequestResponse.getTokenRequest().getId();
                    }
                });
    }

    /**
     * Updates an existing token request.
     *
     * @param requestId token request ID
     * @param options new token request options
     * @return completable
     */
    public Completable updateTokenRequest(String requestId, TokenRequestOptions options) {
        return toCompletable(gateway
                .withAuthentication(authenticationContext())
                .updateTokenRequest(UpdateTokenRequestRequest.newBuilder()
                        .setRequestId(requestId)
                        .setRequestOptions(options)
                        .build()));
    }

    /**
     * Creates a customization.
     *
     * @param logo logo
     * @param colors map of ARGB colors #AARRGGBB
     * @param consentText consent text
     * @return customization id
     */
    public Observable<String> createCustomization(
            BlobProtos.Blob.Payload logo,
            Map<String, String> colors,
            String consentText) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createCustomization(CreateCustomizationRequest.newBuilder()
                        .setLogo(logo)
                        .putAllColors(colors)
                        .setConsentText(consentText)
                        .build()))
                .map(new Function<CreateCustomizationResponse, String>() {
                    @Override
                    public String apply(CreateCustomizationResponse createCustomizationResponse) {
                        return createCustomizationResponse.getCustomizationId();
                    }
                });
    }

    /**
     * Looks up a existing access token where the calling member is the grantor and given member is
     * the grantee.
     *
     * @param toMemberId beneficiary of the active access token
     * @return token returned by the server
     */
    public Observable<Token> getActiveAccessToken(String toMemberId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getActiveAccessToken(GetActiveAccessTokenRequest
                        .newBuilder()
                        .setToMemberId(toMemberId)
                        .build()))
                .map(new Function<GetActiveAccessTokenResponse, Token>() {
                    public Token apply(GetActiveAccessTokenResponse response) {
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
                .withAuthentication(authenticationContext())
                .getTokens(GetTokensRequest
                        .newBuilder()
                        .setType(type)
                        .setPage(pageBuilder(offset, limit))
                        .build()))
                .map(new Function<GetTokensResponse, PagedList<Token, String>>() {
                    public PagedList<Token, String> apply(GetTokensResponse response) {
                        return PagedList.create(response.getTokensList(), response.getOffset());
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
                .withAuthentication(authenticationContext())
                .getToken(GetTokenRequest
                        .newBuilder()
                        .setTokenId(tokenId)
                        .build()))
                .map(new Function<GetTokenResponse, Token>() {
                    public Token apply(GetTokenResponse response) {
                        return response.getToken();
                    }
                });
    }


    /**
     * Retrieves a blob that is attached to a token.
     *
     * @param tokenId id of the token
     * @param blobId id of the blob
     * @return Blob
     */
    public Observable<BlobProtos.Blob> getTokenBlob(String tokenId, String blobId) {
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getTokenBlob(GetTokenBlobRequest
                        .newBuilder()
                        .setTokenId(tokenId)
                        .setBlobId(blobId)
                        .build()))
                .map(new Function<GetTokenBlobResponse, BlobProtos.Blob>() {
                    public BlobProtos.Blob apply(GetTokenBlobResponse response) {
                        return response.getBlob();
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
                .withAuthentication(authenticationContext())
                .getTransfer(GetTransferRequest
                        .newBuilder()
                        .setTransferId(transferId)
                        .build()))
                .map(new Function<GetTransferResponse, Transfer>() {
                    public Transfer apply(GetTransferResponse response) {
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
            request.setFilter(GetTransfersRequest
                    .TransferFilter
                    .newBuilder()
                    .setTokenId(tokenId)
                    .build());
        }

        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .getTransfers(request.build()))
                .map(new Function<GetTransfersResponse, PagedList<Transfer, String>>() {
                    public PagedList<Transfer, String> apply(GetTransfersResponse response) {
                        return PagedList.create(response.getTransfersList(), response.getOffset());
                    }
                });
    }

    /**
     * Sets security metadata included in all requests.
     *
     * @param securityMetadata security metadata
     */
    public void setSecurityMetadata(SecurityMetadata securityMetadata) {
        this.securityMetadata = securityMetadata;
    }

    /**
     * Clears security metadata.
     */
    public void clearSecurityMetadata() {
        this.securityMetadata = SecurityMetadata.getDefaultInstance();
    }

    /**
     * Redeems a transfer token.
     *
     * @param transfer transfer parameters, such as amount, currency, etc
     * @return transfer record
     */
    public Observable<Transfer> createTransfer(TransferProtos.TransferPayload transfer) {
        Signer signer = crypto.createSigner(Key.Level.LOW);
        return toObservable(gateway
                .withAuthentication(authenticationContext())
                .createTransfer(CreateTransferRequest
                        .newBuilder()
                        .setPayload(transfer)
                        .setPayloadSignature(Signature
                                .newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(transfer)))
                        .build()))
                .map(new Function<CreateTransferResponse, Transfer>() {
                    public Transfer apply(CreateTransferResponse response) {
                        return response.getTransfer();
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
                .withAuthentication(authenticationContext())
                .cancelToken(CancelTokenRequest
                        .newBuilder()
                        .setTokenId(token.getId())
                        .setSignature(Signature
                                .newBuilder()
                                .setMemberId(memberId)
                                .setKeyId(signer.getKeyId())
                                .setSignature(signer.sign(tokenAction(token, CANCELLED))))
                        .build()))
                .map(new Function<CancelTokenResponse, TokenOperationResult>() {
                    public TokenOperationResult apply(CancelTokenResponse response) {
                        return response.getResult();
                    }
                });
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof io.token.rpc.Client)) {
            return false;
        }

        Client other = (Client) obj;
        return memberId.equals(other.memberId)
                && Objects.equals(onBehalfOf, other.onBehalfOf);
    }

    @Override
    public int hashCode() {
        return (memberId + (onBehalfOf == null ? "" : onBehalfOf)).hashCode();
    }

    @Override
    protected String getOnBehalfOf() {
        return onBehalfOf;
    }
}
