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

import static io.token.tpp.util.Util.TOKEN;
import static io.token.util.Util.toObservable;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.gateway.Gateway;
import io.token.proto.gateway.Gateway.GetMemberRequest;
import io.token.proto.gateway.Gateway.GetMemberResponse;
import io.token.proto.gateway.Gateway.GetTokenRequestResultResponse;
import io.token.proto.gateway.Gateway.RetrieveTokenRequestResponse;
import io.token.rpc.GatewayProvider;
import io.token.rpc.util.Converters;
import io.token.tokenrequest.TokenRequest;
import io.token.tokenrequest.TokenRequestResult;


/**
 * Similar to {@link Client} but is only used for a handful of requests that
 * don't require authentication. We use this client to create new member or
 * getMember an existing one and switch to the authenticated {@link Client}.
 */
public final class UnauthenticatedClient extends io.token.rpc.UnauthenticatedClient {
    /**
     * Creates an instance.
     *
     * @param gateway gateway service builder
     */
    public UnauthenticatedClient(GatewayProvider gateway) {
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
                .toObservable(gateway()
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
     * Return the token member.
     *
     * @return token member
     */
    public Observable<Member> getTokenMember() {
        return getMemberId(TOKEN).flatMap(
                new Function<String, Observable<Member>>() {
                    @Override
                    public Observable<Member> apply(String memberId) {
                        return getMember(memberId);
                    }
                });
    }

    /**
     * Get the token request result based on a token's tokenRequestId.
     *
     * @param tokenRequestId token request id
     * @return token request result
     */
    public Observable<TokenRequestResult> getTokenRequestResult(String tokenRequestId) {
        return toObservable(gateway()
                .getTokenRequestResult(Gateway.GetTokenRequestResultRequest.newBuilder()
                        .setTokenRequestId(tokenRequestId)
                        .build()))
                .map(new Function<GetTokenRequestResultResponse, TokenRequestResult>() {
                    @Override
                    public TokenRequestResult apply(GetTokenRequestResultResponse response)  {
                        return TokenRequestResult.create(
                                response.getTokenId(),
                                response.getSignature());
                    }
                });
    }

    /**
     * Retrieves a transfer token request.
     *
     * @param tokenRequestId token request id
     *
     * @return token request that was stored with the request id
     */
    public Observable<TokenRequest> retrieveTokenRequest(String tokenRequestId) {
        return toObservable(gateway()
                .retrieveTokenRequest(Gateway.RetrieveTokenRequestRequest
                .newBuilder()
                .setRequestId(tokenRequestId)
                .build()))
                .map(new Function<RetrieveTokenRequestResponse, TokenRequest>() {
                    @Override
                    public TokenRequest apply(RetrieveTokenRequestResponse response) {
                        return TokenRequest
                                .fromProtos(
                                        response.getTokenRequest().getRequestPayload(),
                                        response.getTokenRequest().getRequestOptions());
                    }
                });
    }
}
