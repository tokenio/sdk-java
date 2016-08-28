package io.token.rpc;

import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberAddKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberAliasOperation;
import io.token.proto.common.member.MemberProtos.MemberUpdate;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.token.TokenProtos.PaymentToken;
import io.token.proto.common.token.TokenProtos.SignedToken;
import io.token.proto.gateway.Gateway.*;
import io.token.proto.gateway.GatewayServiceGrpc.GatewayServiceFutureStub;
import io.token.security.SecretKey;
import io.token.util.codec.ByteEncoding;
import rx.Observable;

import java.util.List;

import static io.token.proto.common.token.TokenProtos.TokenSignature.Action.ENDORSED;
import static io.token.rpc.util.Converters.toObservable;
import static io.token.security.Crypto.sign;

/**
 * An authenticated RPC client that is used to talk to Token gateway. The
 * class is a thin wrapper on top of gRPC generated client. Makes the API
 * easier to use.
 */
public final class Client {
    private final SecretKey key;
    private final GatewayServiceFutureStub gateway;

    /**
     * @param key secret key that is used to sign payload for certain requests.
     *            This is generally the same key that is used for
     *            authentication.
     * @param gateway gateway gRPC stub
     */
    public Client(SecretKey key, GatewayServiceFutureStub gateway) {
        this.key = key;
        this.gateway = gateway;
    }

    /**
     * Looks up member information for the current user. The user is defined by
     * the key used for authentication.
     *
     * @return member information
     */
    public Observable<Member> getMember() {
        return toObservable(gateway.getMember(GetMemberRequest.getDefaultInstance()))
                .map(GetMemberResponse::getMember);
    }

    /**
     * Adds a public key to the list of the approved keys.
     *
     * @param member member to add the key to
     * @param level key level
     * @param publicKey public key to add to the approved list
     * @return member information
     */
    public Observable<Member> addKey(Member member, int level, byte[] publicKey) {
        return updateMember(MemberUpdate.newBuilder()
                .setMemberId(member.getId())
                .setPrevHash(member.getLastHash())
                .setAddKey(MemberAddKeyOperation.newBuilder()
                        .setPublicKey(ByteEncoding.serialize(publicKey))
                        .setLevel(level))
                .build());
    }

    /**
     * Removes a public key from the list of the approved keys.
     *
     * @param member member to remove the key for
     * @param keyId key ID of the key to remove
     * @return member information
     */
    public Observable<Member> removeKey(Member member, String keyId) {
        return updateMember(MemberUpdate.newBuilder()
                .setMemberId(member.getId())
                .setPrevHash(member.getLastHash())
                .setRemoveKey(MemberProtos.MemberRemoveKeyOperation.newBuilder()
                        .setKeyId(keyId))
                .build());
    }

    /**
     * Adds an alias for a given user.
     *
     * @param member member to add the key to
     * @param alias new unique alias to add
     * @return member information
     */
    public Observable<Member> addAlias(Member member, String alias) {
        return updateMember(MemberUpdate.newBuilder()
                .setMemberId(member.getId())
                .setPrevHash(member.getLastHash())
                .setAddAlias(MemberAliasOperation.newBuilder()
                        .setAlias(alias))
                .build());
    }

    /**
     * Removes an existing alias for a given user.
     *
     * @param member member to add the key to
     * @param alias new unique alias to add
     * @return member information
     */
    public Observable<Member> removeAlias(Member member, String alias) {
        return updateMember(MemberUpdate.newBuilder()
                .setMemberId(member.getId())
                .setPrevHash(member.getLastHash())
                .setRemoveAlias(MemberAliasOperation.newBuilder()
                        .setAlias(alias))
                .build());
    }

    /**
     * Creates a new payment token.
     *
     * @param token payment token
     * @return payment token returned by the server
     */
    public Observable<SignedToken> createToken(PaymentToken token) {
        return toObservable(gateway.createPaymentToken(CreatePaymentTokenRequest.newBuilder()
                .setToken(token)
                .build())
        ).map(CreatePaymentTokenResponse::getToken);
    }

    /**
     * Looks up a existing token.
     *
     * @param tokenId token id
     * @return token returned by the server
     */
    public Observable<SignedToken> lookupToken(String tokenId) {
        return toObservable(gateway.lookupToken(LookupTokenRequest.newBuilder()
                .setTokenId(tokenId)
                .build())
        ).map(LookupTokenResponse::getToken);
    }

    /**
     * Looks up a list of existing token.
     *
     * @param offset offset to start at
     * @param limit max number of records to return
     * @return token returned by the server
     */
    public Observable<List<SignedToken>> lookupTokens(int offset, int limit) {
        return toObservable(gateway.lookupTokens(LookupTokensRequest.newBuilder()
                .setOffset(offset)
                .setLimit(limit)
                .build())
        ).map(LookupTokensResponse::getTokensList);
    }

    /**
     * Endorses a token.
     *
     * @param token token to endorse
     * @return endorsed token returned by the server
     */
    public Observable<SignedToken> endorseToken(SignedToken token) {
        return toObservable(gateway.endorseToken(EndorseTokenRequest.newBuilder()
                .setTokenId(token.getId())
                .setSignature(Signature.newBuilder()
                        .setKeyId(key.getId())
                        .setSignature(sign(key, token, ENDORSED)))
                .build())
        ).map(EndorseTokenResponse::getToken);
    }

    /**
     * Declines a token.
     *
     * @param token token to decline
     * @return declined token returned by the server
     */
    public Observable<SignedToken> declineToken(SignedToken token) {
        return toObservable(gateway.declineToken(DeclineTokenRequest.newBuilder()
                .setTokenId(token.getId())
                .setSignature(Signature.newBuilder()
                        .setKeyId(key.getId())
                        .setSignature(sign(key, token, ENDORSED)))
                .build())
        ).map(DeclineTokenResponse::getToken);
    }

    /**
     * Revokes a token.
     *
     * @param token token to revoke
     * @return revoked token returned by the server
     */
    public Observable<SignedToken> revokeToken(SignedToken token) {
        return toObservable(gateway.revokeToken(RevokeTokenRequest.newBuilder()
                .setTokenId(token.getId())
                .setSignature(Signature.newBuilder()
                        .setKeyId(key.getId())
                        .setSignature(sign(key, token, ENDORSED)))
                .build())
        ).map(RevokeTokenResponse::getToken);
    }

    private Observable<Member> updateMember(MemberUpdate update) {
        return toObservable(gateway.updateMember(UpdateMemberRequest.newBuilder()
                .setUpdate(update)
                .setSignature(Signature.newBuilder()
                        .setKeyId(key.getId())
                        .setSignature(sign(key, update)))
                .build())
        ).map(UpdateMemberResponse::getMember);
    }
}
