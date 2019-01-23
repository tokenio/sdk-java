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

package io.token.bank;

import static io.token.proto.gateway.Gateway.GetTokensRequest.Type.ACCESS;
import static io.token.proto.gateway.Gateway.GetTokensRequest.Type.TRANSFER;

import io.reactivex.Observable;
import io.token.Account;
import io.token.TokenClient.TokenCluster;
import io.token.bank.rpc.Client;
import io.token.proto.PagedList;
import io.token.proto.common.blob.BlobProtos;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.proto.common.token.TokenProtos.TokenOperationResult;
import io.token.proto.common.transfer.TransferProtos.Transfer;

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

    /**
     * Creates an instance of {@link Member}.
     *
     * @param member internal member representation, fetched from server
     * @param client RPC client used to perform operations against the server
     * @param cluster Token cluster, e.g. sandbox, production
     */
    Member(
            MemberProtos.Member member,
            Client client,
            TokenCluster cluster) {
        super(member, client, cluster);
        this.client = client;
    }

    Member(io.token.Member member, Client client) {
        super(member);
        this.client = client;
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
     * Looks up access tokens owned by the member.
     *
     * @param offset optional offset to start at
     * @param limit max number of records to return
     * @return transfer tokens owned by the member
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
     * @return transfer tokens owned by the member
     */
    public PagedList<Token, String> getAccessTokensBlocking(@Nullable String offset, int limit) {
        return getAccessTokens(offset, limit).blockingSingle();
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
     * Retrieves a blob that is attached to a transfer token.
     *
     * @param tokenId id of the token
     * @param blobId id of the blob
     * @return Blob
     */
    public Observable<BlobProtos.Blob> getTokenAttachment(String tokenId, String blobId) {
        return client.getTokenBlob(tokenId, blobId);
    }

    /**
     * Retrieves a blob that is attached to a token.
     *
     * @param tokenId id of the token
     * @param blobId id of the blob
     * @return Blob
     */
    public BlobProtos.Blob getTokenAttachmentBlocking(String tokenId, String blobId) {
        return getTokenAttachment(tokenId, blobId).blockingSingle();
    }

    /**
     * Creates a test bank account in a fake bank and links the account.
     *
     * @param balance account balance to set
     * @param currency currency code, e.g. "EUR"
     * @return the linked account
     */
    public Observable<Account> createAndLinkTestBankAccount(double balance, String currency) {
        return createAndLinkTestBankAccountImpl(balance, currency);
    }

    /**
     * Creates a test bank account in a fake bank and links the account.
     *
     * @param balance account balance to set
     * @param currency currency code, e.g. "EUR"
     * @return the linked account
     */
    public Account createAndLinkTestBankAccountBlocking(double balance, String currency) {
        return createAndLinkTestBankAccount(balance, currency).blockingSingle();
    }
}
