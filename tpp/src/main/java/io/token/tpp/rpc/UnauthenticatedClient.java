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

package io.token.tpp.rpc;

import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.tpp.util.Util.TOKEN;
import static io.token.util.Util.toObservable;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Strings;
import io.reactivex.Observable;
import io.token.proto.common.eidas.EidasProtos.EidasRecoveryPayload;
import io.token.proto.common.eidas.EidasProtos.RegisterWithEidasPayload;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberAddKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.gateway.Gateway;
import io.token.proto.gateway.Gateway.GetMemberRequest;
import io.token.proto.gateway.Gateway.GetMemberResponse;
import io.token.proto.gateway.Gateway.RegisterWithEidasRequest;
import io.token.proto.gateway.Gateway.RegisterWithEidasResponse;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.rpc.util.Converters;
import io.token.security.CryptoEngine;
import io.token.security.Signer;
import io.token.tokenrequest.TokenRequest;
import io.token.tokenrequest.TokenRequestResult;

import java.util.Optional;
import java.util.stream.Stream;


/**
 * Similar to {@link Client} but is only used for a handful of requests that
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

    /**
     * Looks up member information for the given member ID. The user is defined by
     * the key used for authentication.
     *
     * @param memberId member id
     * @return an observable of member
     */
    public Observable<Member> getMember(String memberId) {
        return Converters
                .toObservable(gateway.getMember(GetMemberRequest.newBuilder()
                        .setMemberId(memberId)
                        .build()))
                .map(GetMemberResponse::getMember);
    }

    /**
     * Return the token member.
     *
     * @return token member
     */
    public Observable<Member> getTokenMember() {
        return getMemberId(TOKEN).flatMap(this::getMember);
    }

    /**
     * Get the token request result based on a token's tokenRequestId.
     *
     * @param tokenRequestId token request id
     * @return token request result
     */
    public Observable<TokenRequestResult> getTokenRequestResult(String tokenRequestId) {
        return toObservable(gateway
                .getTokenRequestResult(Gateway.GetTokenRequestResultRequest.newBuilder()
                        .setTokenRequestId(tokenRequestId)
                        .build()))
                .map(response -> TokenRequestResult.create(
                        response.getTokenId(),
                        Optional.ofNullable(Strings.emptyToNull(response.getTransferId())),
                        Optional.ofNullable(Strings.emptyToNull(
                                response.getStandingOrderSubmissionId())),
                        response.getSignature()));
    }

    /**
     * Retrieves a transfer token request.
     *
     * @param tokenRequestId token request id
     *
     * @return token request that was stored with the request id
     */
    public Observable<TokenRequest> retrieveTokenRequest(String tokenRequestId) {
        return toObservable(gateway.retrieveTokenRequest(Gateway.RetrieveTokenRequestRequest
                .newBuilder()
                .setRequestId(tokenRequestId)
                .build()))
                .map(response -> TokenRequest
                        .fromProtos(
                                response.getTokenRequest().getRequestPayload(),
                                response.getTokenRequest().getRequestOptions()));
    }

    /**
     * Create and onboard a business member under realm of a bank using eIDAS certificate.
     *
     * @param payload payload with eIDAS certificate and bank id
     * @param signature payload signed with the private key corresponding to the certificate
     *     public key
     * @return member id, registered key id and id of the certificate verification request
     */
    public Observable<RegisterWithEidasResponse> registerWithEidas(
            RegisterWithEidasPayload payload,
            String signature) {
        return toObservable(gateway.registerWithEidas(RegisterWithEidasRequest.newBuilder()
                .setPayload(payload)
                .setSignature(signature)
                .build()));
    }

    /**
     * Recovers an eIDAS-verified member with eidas payload.
     *
     * @param payload a payload containing member id, the certificate and a new key to add to the
     *      member
     * @param signature a payload signature with the private key corresponding to the certificate
     * @param cryptoEngine a crypto engine that must contain the privileged key that is included in
     *      the payload (if it does not contain keys for other levels they will be generated)
     * @return an observable of a new member
     */
    public Observable<Member> recoverEidasMember(
            EidasRecoveryPayload payload,
            String signature,
            CryptoEngine cryptoEngine) {
        SecurityProtos.Key privilegedKey = payload.getKey();
        SecurityProtos.Key standardKey = getOrGenerateKeyForLevel(cryptoEngine, STANDARD);
        SecurityProtos.Key lowKey = getOrGenerateKeyForLevel(cryptoEngine, LOW);
        Signer signer = cryptoEngine.createSigner(privilegedKey.getId());
        String memberId = payload.getMemberId();
        return toObservable(gateway.recoverEidasMember(Gateway.RecoverEidasRequest.newBuilder()
                .setPayload(payload)
                .setSignature(signature)
                .build()))
                .flatMap(response -> toObservable(gateway
                            .getMember(GetMemberRequest.newBuilder()
                                    .setMemberId(memberId)
                                    .build()))
                            .map(memberRes -> MemberProtos.MemberUpdate.newBuilder()
                                    .setPrevHash(memberRes.getMember().getLastHash())
                                    .setMemberId(memberId)
                                    .addOperations(MemberProtos.MemberOperation.newBuilder()
                                            .setRecover(response.getRecoveryEntry()))
                                    .addAllOperations(Stream.of(privilegedKey, standardKey, lowKey)
                                            .map(key -> MemberOperation.newBuilder()
                                                    .setAddKey(MemberAddKeyOperation.newBuilder()
                                                            .setKey(key))
                                                    .build())
                                            .collect(toList()))
                                    .build()))
                .flatMap(memberUpdate -> toObservable(
                        gateway.updateMember(Gateway.UpdateMemberRequest
                                .newBuilder()
                                .setUpdate(memberUpdate)
                                .setUpdateSignature(SecurityProtos.Signature.newBuilder()
                                        .setKeyId(signer.getKeyId())
                                        .setMemberId(memberId)
                                        .setSignature(signer.sign(memberUpdate)))
                                .build()))
                        .map(Gateway.UpdateMemberResponse::getMember));
    }

    private static SecurityProtos.Key getOrGenerateKeyForLevel(
            CryptoEngine cryptoEngine,
            SecurityProtos.Key.Level level) {
        return cryptoEngine
                .getPublicKeys()
                .stream()
                .filter(key -> key.getLevel().equals(level))
                .findFirst()
                .orElse(cryptoEngine.generateKey(level));
    }
}
