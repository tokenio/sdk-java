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

package io.token.user.rpc;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;
import static io.token.proto.common.alias.AliasProtos.VerificationStatus.SUCCESS;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.user.util.Util.TOKEN;
import static io.token.user.util.Util.generateNonce;
import static io.token.user.util.Util.normalizeAlias;
import static io.token.user.util.Util.toObservable;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.user.NotifyResult;
import io.token.user.TokenRequest;
import io.token.user.exceptions.MemberNotFoundException;
import io.token.user.exceptions.VerificationException;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.bank.BankProtos.Bank;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.member.MemberProtos.CreateMemberType;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberAddKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation.Authorization;
import io.token.proto.common.member.MemberProtos.MemberUpdate;
import io.token.proto.common.member.MemberProtos.ReceiptContact;
import io.token.proto.common.notification.NotificationProtos.AddKey;
import io.token.proto.common.notification.NotificationProtos.EndorseAndAddKey;
import io.token.proto.common.notification.NotificationProtos.NotifyBody;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.gateway.Gateway;
import io.token.proto.gateway.Gateway.BeginRecoveryRequest;
import io.token.proto.gateway.Gateway.BeginRecoveryResponse;
import io.token.proto.gateway.Gateway.CompleteRecoveryRequest;
import io.token.proto.gateway.Gateway.CompleteRecoveryResponse;
import io.token.proto.gateway.Gateway.CreateMemberRequest;
import io.token.proto.gateway.Gateway.CreateMemberResponse;
import io.token.proto.gateway.Gateway.GetBanksRequest;
import io.token.proto.gateway.Gateway.GetBanksResponse;
import io.token.proto.gateway.Gateway.GetBlobResponse;
import io.token.proto.gateway.Gateway.GetMemberRequest;
import io.token.proto.gateway.Gateway.GetMemberResponse;
import io.token.proto.gateway.Gateway.GetTokenRequestResultRequest;
import io.token.proto.gateway.Gateway.GetTokenRequestResultResponse;
import io.token.proto.gateway.Gateway.InvalidateNotificationRequest;
import io.token.proto.gateway.Gateway.InvalidateNotificationResponse;
import io.token.proto.gateway.Gateway.NotifyRequest;
import io.token.proto.gateway.Gateway.NotifyResponse;
import io.token.proto.gateway.Gateway.RequestTransferRequest;
import io.token.proto.gateway.Gateway.RequestTransferResponse;
import io.token.proto.gateway.Gateway.ResolveAliasRequest;
import io.token.proto.gateway.Gateway.ResolveAliasResponse;
import io.token.proto.gateway.Gateway.RetrieveTokenRequestRequest;
import io.token.proto.gateway.Gateway.RetrieveTokenRequestResponse;
import io.token.proto.gateway.Gateway.TriggerCreateAndEndorseTokenNotificationRequest;
import io.token.proto.gateway.Gateway.TriggerCreateAndEndorseTokenNotificationResponse;
import io.token.proto.gateway.Gateway.TriggerEndorseAndAddKeyNotificationRequest;
import io.token.proto.gateway.Gateway.TriggerEndorseAndAddKeyNotificationResponse;
import io.token.proto.gateway.Gateway.UpdateMemberRequest;
import io.token.proto.gateway.Gateway.UpdateMemberResponse;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.rpc.util.Converters;
import io.token.user.security.CryptoEngine;
import io.token.security.Signer;
import io.token.user.tokenrequest.TokenRequestResult;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Similar to {@link io.token.user.rpc.Client} but is only used for a handful of requests that
 * don't require authentication. We use this client to create new member or
 * getMember an existing one and switch to the authenticated {@link Client}.
 */
public final class UnauthenticatedClient extends io.token.rpc.UnauthenticatedClient {
    /**
     * Creates an instance.
     *
     * @param gateway gateway gRPC stub
     */
    public UnauthenticatedClient(GatewayServiceFutureStub gateway) {
        super(gateway);
    }
}
