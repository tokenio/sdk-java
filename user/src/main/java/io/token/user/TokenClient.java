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

import static io.token.proto.common.member.MemberProtos.CreateMemberType.BUSINESS;
import static io.token.proto.common.member.MemberProtos.CreateMemberType.PERSONAL;
import static io.token.proto.common.member.MemberProtos.CreateMemberType.TRANSIENT;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.Util.generateNonce;
import static io.token.util.Util.normalizeAlias;
import static io.token.util.Util.toAddAliasOperation;
import static io.token.util.Util.toAddAliasOperationMetadata;
import static io.token.util.Util.toAddKeyOperation;
import static io.token.util.Util.toRecoveryAgentOperation;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.ManagedChannel;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.Member;
import io.token.exceptions.VerificationException;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos;
import io.token.proto.common.blob.BlobProtos;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.CreateMemberType;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation.Authorization;
import io.token.proto.common.member.MemberProtos.ReceiptContact;
import io.token.proto.common.notification.NotificationProtos.AddKey;
import io.token.proto.common.notification.NotificationProtos.DeviceMetadata;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.rpc.Client;
import io.token.rpc.ClientFactory;
import io.token.rpc.UnauthenticatedClient;
import io.token.security.CryptoEngine;
import io.token.security.CryptoEngineFactory;
import io.token.security.InMemoryKeyStore;
import io.token.security.Signer;
import io.token.security.TokenCryptoEngine;
import io.token.user.browser.BrowserFactory;
import io.token.user.tokenrequest.TokenRequestCallbackParameters;
import io.token.user.tokenrequest.TokenRequestResult;
import io.token.user.tokenrequest.TokenRequestState;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

public class TokenClient extends io.token.TokenClient {
    private final String devKey;
    private final BrowserFactory browserFactory;

    /**
     * Creates an instance of a Token SDK.
     *
     * @param channel GRPC channel
     * @param cryptoFactory crypto factory instance
     * @param developerKey developer key
     * @param tokenCluster token cluster
     */
    TokenClient(
            ManagedChannel channel,
            CryptoEngineFactory cryptoFactory,
            String developerKey,
            TokenCluster tokenCluster,
            BrowserFactory browserFactory) {
        super(channel, cryptoFactory, tokenCluster);
        this.devKey = developerKey;
        this.browserFactory = browserFactory;
    }

    /**
     * Retrieves a blob from the server.
     *
     * @param blobId id of the blob
     * @return Blob
     */
    public Observable<BlobProtos.Blob> getBlob(String blobId) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getBlob(blobId);
    }

}