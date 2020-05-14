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

import static com.google.common.io.BaseEncoding.base64;
import static io.token.TokenClient.TokenCluster.SANDBOX;
import static io.token.tpp.exceptions.EidasRegistrationException.registrationException;
import static io.token.tpp.exceptions.EidasRegistrationException.tookTooLong;
import static io.token.proto.common.eidas.EidasProtos.EidasVerificationStatus.EIDAS_STATUS_ERROR;
import static io.token.proto.common.eidas.EidasProtos.EidasVerificationStatus.EIDAS_STATUS_PENDING;
import static io.token.proto.common.eidas.EidasProtos.EidasVerificationStatus.EIDAS_STATUS_SUCCESS;
import static io.token.proto.common.member.MemberProtos.CreateMemberType.BUSINESS;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.security.crypto.CryptoType.RS256;
import static io.token.tpp.util.Util.hashString;
import static io.token.tpp.util.Util.urlEncode;
import static io.token.tpp.util.Util.verifySignature;
import static io.token.util.Util.getWebAppUrl;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.eidas.EidasProtos.EidasRecoveryPayload;
import io.token.proto.common.eidas.EidasProtos.EidasVerificationStatus;
import io.token.proto.common.eidas.EidasProtos.RegisterWithEidasPayload;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.token.TokenProtos;
import io.token.proto.gateway.Gateway.GetEidasVerificationStatusResponse;
import io.token.proto.gateway.Gateway.RegisterWithEidasResponse;
import io.token.rpc.client.lite.RpcChannelFactoryLite;
import io.token.security.CryptoEngine;
import io.token.security.CryptoEngineFactory;
import io.token.security.InMemoryKeyStore;
import io.token.security.KeyStore;
import io.token.security.SecretKey;
import io.token.security.Signer;
import io.token.security.TokenCryptoEngine;
import io.token.security.TokenCryptoEngineFactory;
import io.token.security.crypto.CryptoRegistry;
import io.token.tokenrequest.TokenRequest;
import io.token.tokenrequest.TokenRequestResult;
import io.token.tokenrequest.TokenRequestState;
import io.token.tpp.exceptions.EidasRegistrationException;
import io.token.tpp.exceptions.EidasTimeoutException;
import io.token.tpp.exceptions.InvalidStateException;
import io.token.tpp.rpc.Client;
import io.token.tpp.rpc.ClientFactory;
import io.token.tpp.rpc.UnauthenticatedClient;
import io.token.tpp.security.EidasCryptoEngineFactory;
import io.token.tpp.security.EidasKeyStore;
import io.token.tpp.security.InMemoryEidasKeyStore;
import io.token.tpp.tokenrequest.TokenRequestCallback;
import io.token.tpp.tokenrequest.TokenRequestCallbackParameters;
import io.token.tpp.tokenrequest.TokenRequestTransferDestinationsCallbackParameters;
import io.token.tpp.util.Util;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

public class TokenClient extends io.token.TokenClient {
    private static final String TOKEN_REQUEST_TEMPLATE =
            "https://%s/request-token/%s?state=%s";

    /**
     * Creates an instance of a Token SDK.
     *
     * @param channel GRPC channel
     * @param cryptoFactory crypto factory instance
     * @param tokenCluster token cluster
     */
    TokenClient(
            ManagedChannel channel,
            CryptoEngineFactory cryptoFactory,
            TokenCluster tokenCluster) {
        super(channel, cryptoFactory, tokenCluster);
    }

    /**
     * Creates a new {@link Builder} instance that is used to configure and
     * build a {@link TokenClient} instance.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new instance of {@link TokenClient} that's configured to use
     * the specified environment.
     *
     * @param cluster token cluster to connect to
     * @param developerKey developer key
     * @return {@link TokenClient} instance
     */
    public static TokenClient create(TokenCluster cluster, String developerKey) {
        return TokenClient.builder()
                .connectTo(cluster)
                .devKey(developerKey)
                .build();
    }

    /**
     * Creates a new instance of {@link TokenClient} that's configured to use
     * the specified environment and crypto engine factory.
     *
     * @param cluster token cluster to connect to
     * @param developerKey developer key
     * @param cryptoEngineFactory crypto engine factory to use
     * @return {@link TokenClient} instance
     */
    public static TokenClient create(
            TokenCluster cluster,
            String developerKey,
            CryptoEngineFactory cryptoEngineFactory) {
        return TokenClient.builder()
                .connectTo(cluster)
                .devKey(developerKey)
                .withCryptoEngine(cryptoEngineFactory)
                .build();
    }

    /**
     * Creates a new Token member with a set of auto-generated keys, an alias, and member type.
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member.
     * @return newly created member
     */
    public Observable<Member> createMember(final Alias alias) {
        return createMember(alias, null);
    }

    /**
     * Creates a new Token member with a set of auto-generated keys, an alias, and member type.
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member.
     * @param partnerId ID of partner member.
     * @return newly created member
     */
    public Observable<Member> createMember(final Alias alias, String partnerId) {
        return createMember(alias, partnerId, null);
    }

    /**
     * Creates a new Token member with a set of auto-generated keys, an alias, and member type.
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member.
     * @param partnerId ID of partner member
     * @param realmId member Id of existing member to which this new member is associated with
     * @return newly created member
     */
    public Observable<Member> createMember(
            final Alias alias,
            @Nullable final String partnerId,
            @Nullable final String realmId) {
        return createMemberImpl(alias, BUSINESS, partnerId, null, realmId)
                .map(new Function<io.token.Member, Member>() {
                    @Override
                    public Member apply(io.token.Member mem) {
                        CryptoEngine crypto = cryptoFactory.create(mem.memberId());
                        final Client client = ClientFactory.authenticated(
                                channel,
                                mem.memberId(),
                                crypto);
                        return new Member(
                                mem.memberId(),
                                mem.partnerId(),
                                mem.realmId(),
                                client,
                                mem.getTokenCluster());
                    }
                });
    }

    /**
     * Creates a new Token member with a set of auto-generated keys, an alias, and member type.
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member.
     * @return newly created member
     */
    public Member createMemberBlocking(final Alias alias) {
        return createMember(alias).blockingSingle();
    }

    /**
     * Creates a new business-use Token member with a set of auto-generated keys and and an alias.
     *
     * @param alias alias to associate with member
     * @param partnerId ID of partner member
     * @return newly created member
     */
    public Member createMemberBlocking(Alias alias, String partnerId) {
        return createMember(alias, partnerId).blockingSingle();
    }

    /**
     * Creates a new Token member in the provided realm with a set of auto-generated keys, an alias,
     * and member type.
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member.
     * @param realmId member id of an existing Member to whose realm this new member belongs.
     * @return newly created member
     */
    public Observable<Member> createMemberInRealm(final Alias alias, String realmId) {
        return createMember(alias, null, realmId);
    }

    /**
     * Creates a new Token member in the provided realm with a set of auto-generated keys, an alias,
     * and member type.
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member.
     * @param realmId member id of the Member whose realm this new Member belongs.
     * @return newly created member
     */
    public Member createMemberInRealmBlocking(final Alias alias, String realmId) {
        return createMemberInRealm(alias, realmId).blockingSingle();
    }

    /**
     * Sets up a member given a specific ID of a member that already exists in the system. If
     * the member ID already has keys, this will not succeed. Used for testing since this
     * gives more control over the member creation process.
     *
     * <p>Adds an alias and a set of auto-generated keys to the member.</p>
     *
     * @param alias nullable member alias to use, must be unique. If null, then no alias will
     *     be created with the member
     * @param memberId member id
     * @return newly created member
     */
    @VisibleForTesting
    public Observable<Member> setUpMember(final Alias alias, final String memberId) {
        CryptoEngine crypto = cryptoFactory.create(memberId);
        final Client client = ClientFactory.authenticated(channel, memberId, crypto);
        return setUpMemberImpl(alias, memberId, null)
                .map(new Function<io.token.Member, Member>() {
                    @Override
                    public Member apply(io.token.Member mem) {
                        return new Member(
                                mem.memberId(),
                                mem.partnerId(),
                                mem.realmId(),
                                client,
                                mem.getTokenCluster());
                    }
                });
    }

    /**
     * Return a Member set up to use some Token member's keys (assuming we have them).
     *
     * @param memberId member id
     * @return member
     */
    public Observable<Member> getMember(String memberId) {
        CryptoEngine crypto = cryptoFactory.create(memberId);
        final Client client = ClientFactory.authenticated(channel, memberId, crypto);
        return getMemberImpl(memberId, client)
                .map(new Function<io.token.Member, Member>() {
                    @Override
                    public Member apply(io.token.Member mem) {
                        return new Member(
                                mem.memberId(),
                                mem.partnerId(),
                                mem.realmId(),
                                client,
                                mem.getTokenCluster());
                    }
                });
    }

    /**
     * Return a Member set up to use some Token member's keys (assuming we have them).
     *
     * @param memberId member id
     * @return member
     */
    public Member getMemberBlocking(String memberId) {
        return getMember(memberId).blockingSingle();
    }

    /**
     * Completes account recovery.
     *
     * @param memberId the member id
     * @param recoveryOperations the member recovery operations
     * @param privilegedKey the privileged public key in the member recovery operations
     * @param cryptoEngine the new crypto engine
     * @return an observable of the updated member
     */
    public Observable<Member> completeRecovery(
            String memberId,
            List<MemberRecoveryOperation> recoveryOperations,
            SecurityProtos.Key privilegedKey,
            final CryptoEngine cryptoEngine) {
        return completeRecoveryImpl(memberId, recoveryOperations, privilegedKey, cryptoEngine)
                .map(new Function<io.token.Member, Member>() {
                    @Override
                    public Member apply(io.token.Member mem) {
                        final Client client = ClientFactory.authenticated(
                                channel,
                                mem.memberId(),
                                cryptoEngine);
                        return new Member(
                                mem.memberId(),
                                mem.partnerId(),
                                mem.realmId(),
                                client,
                                mem.getTokenCluster());
                    }
                });
    }

    /**
     * Completes account recovery.
     *
     * @param memberId the member id
     * @param recoveryOperations the member recovery operations
     * @param privilegedKey the privileged public key in the member recovery operations
     * @param cryptoEngine the new crypto engine
     * @return an observable of the updated member
     */
    public Member completeRecoveryBlocking(
            String memberId,
            List<MemberRecoveryOperation> recoveryOperations,
            SecurityProtos.Key privilegedKey,
            final CryptoEngine cryptoEngine) {
        return completeRecovery(memberId, recoveryOperations, privilegedKey, cryptoEngine)
                .blockingSingle();
    }

    /**
     * Completes account recovery if the default recovery rule was set.
     *
     * @param memberId the member id
     * @param verificationId the verification id
     * @param code the code
     * @param cryptoEngine the new crypto engine
     * @return the new member
     */
    public Observable<Member> completeRecoveryWithDefaultRule(
            String memberId,
            String verificationId,
            String code,
            final CryptoEngine cryptoEngine) {
        return completeRecoveryWithDefaultRuleImpl(memberId, verificationId, code, cryptoEngine)
                .map(new Function<io.token.Member, Member>() {
                    @Override
                    public Member apply(io.token.Member mem) {
                        final Client client = ClientFactory.authenticated(
                                channel,
                                mem.memberId(),
                                cryptoEngine);
                        return new Member(
                                mem.memberId(),
                                mem.partnerId(),
                                mem.realmId(),
                                client,
                                mem.getTokenCluster());
                    }
                });
    }

    /**
     * Completes account recovery if the default recovery rule was set.
     *
     * @param memberId the member id
     * @param verificationId the verification id
     * @param code the code
     * @param cryptoEngine the new crypto engine
     * @return the new member
     */
    public Member completeRecoveryWithDefaultRuleBlocking(
            String memberId,
            String verificationId,
            String code,
            final CryptoEngine cryptoEngine) {
        return completeRecoveryWithDefaultRule(memberId, verificationId, code, cryptoEngine)
                .blockingSingle();
    }

    /**
     * Recovers an eIDAS-verified member with eIDAS payload.
     *
     * @param payload a payload containing member id, the certificate and a new key to add to the
     *     member
     * @param signature a payload signature with the private key corresponding to the certificate
     * @param cryptoEngine a crypto engine that must contain the privileged key that is included in
     *     the payload (if it does not contain keys for other levels they will be generated)
     * @return an observable of a new member
     */
    public Observable<Member> recoverEidasMember(
            EidasRecoveryPayload payload,
            String signature,
            final CryptoEngine cryptoEngine) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.recoverEidasMember(payload, signature, cryptoEngine)
                .map(member -> {
                    final Client client = ClientFactory.authenticated(
                            channel,
                            member.getId(),
                            cryptoEngine);
                    return new Member(
                            member.getId(),
                            member.getPartnerId(),
                            member.getRealmId(),
                            client,
                            tokenCluster);
                });
    }

    /**
     * Creates a business member under realm of a bank with an EIDAS alias (with value equal to the
     * authNumber from the certificate) and a PRIVILEGED-level public key taken from the
     * certificate. Then onboards the member with the provided certificate.
     * A successful onboarding includes verifying the member and the alias and adding permissions
     * based on the certificate.<br>
     * The call is idempotent.<br>
     * If you need to submit another certificate for an existing member, please use VerifyEidas call
     * instead.<br><br>
     * Note, that the call is asynchronous and the newly created member might not be onboarded at
     * the time the call returns. You can check the verification status using
     * member.getEidasVerificationStatus call with the verification id returned by this call.
     *
     * @param payload payload with eIDAS certificate and bank id
     * @param signature payload signed with the private key corresponding to the certificate
     *      public key
     * @return member id, registered key id and id of the certificate verification request
     */
    public Observable<RegisterWithEidasResponse> registerWithEidas(
            RegisterWithEidasPayload payload,
            String signature) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.registerWithEidas(payload, signature);
    }


    /**
     * TODO
     *
     * @param bankId
     * @param keyStore a key store containing an eIDAS certificate and a private key for it
     * @return a registered member
     * @throws EidasRegistrationException
     * @throws EidasTimeoutException
     */
    public Member registerWithEidasBlocking(
            String bankId,
            EidasKeyStore keyStore,
            long timeout,
            TimeUnit timeUnit) throws CertificateEncodingException, InterruptedException {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        SecretKey keyPair = keyStore.getKey();
        Signer payloadSigner = CryptoRegistry
                .getInstance()
                .cryptoFor(RS256)
                .signer(keyPair.getId(), keyPair.getPrivateKey());

        RegisterWithEidasPayload payload = RegisterWithEidasPayload
                .newBuilder()
                .setCertificate(base64().encode(keyStore.getEidasCertificate().getEncoded()))
                .setBankId(bankId)
                .build();

        RegisterWithEidasResponse resp = unauthenticated
                .registerWithEidas(payload, payloadSigner.sign(payload))
                .blockingSingle();

        // get the member
        Member member = getMemberBlocking(resp.getMemberId());
        // wait until it is onboarded
        String statusDetails = "";
        EidasVerificationStatus verificationStatus = EIDAS_STATUS_PENDING;
        long t = 1000;
        long sumT = 0;
        long maxT = timeUnit.toMillis(timeout);
        while (EIDAS_STATUS_PENDING.equals(verificationStatus)) {
            if (sumT > maxT) {
                throw new EidasTimeoutException(resp.getMemberId(), resp.getVerificationId());
            }
            GetEidasVerificationStatusResponse verificationResponse = member
                    .getEidasVerificationStatus(resp.getVerificationId())
                    .blockingSingle();
            verificationStatus = verificationResponse.getEidasStatus();
            statusDetails = verificationResponse.getStatusDetails();
            Thread.sleep(t);
            sumT += t;
            t = Math.min(t * 2, 5000);
        }
        if (!EIDAS_STATUS_SUCCESS.equals(verificationStatus)) {
            throw registrationException(verificationStatus, statusDetails);
        }

        return member;
    }

    /**
     * Generate a Token request URL from a request ID, and state. This does not set a CSRF token
     * or pass in a state.
     *
     * @param requestId request id
     * @return token request url
     */
    public Observable<String> generateTokenRequestUrl(String requestId) {
        return generateTokenRequestUrl(requestId, "", "");
    }

    /**
     * Generate a Token request URL from a request ID, and state. This does not set a CSRF token.
     *
     * @param requestId request id
     * @param state state
     * @return token request url
     */
    @Deprecated // set state on the TokenRequest builder
    public Observable<String> generateTokenRequestUrl(
            String requestId,
            String state) {
        return generateTokenRequestUrl(requestId, state, "");
    }

    /**
     * Generate a Token request URL from a request ID, a state, and a CSRF token.
     *
     * @param requestId request id
     * @param state state
     * @param csrfToken csrf token
     * @return token request url
     */
    @Deprecated // set state and csrf token on the TokenRequest builder
    public Observable<String> generateTokenRequestUrl(
            String requestId,
            String state,
            String csrfToken) {
        String csrfTokenHash = hashString(csrfToken);
        TokenRequestState tokenRequestState = TokenRequestState.create(csrfTokenHash, state);
        return Observable.just(String.format(TOKEN_REQUEST_TEMPLATE,
                getWebAppUrl(tokenCluster),
                requestId,
                urlEncode(tokenRequestState.serialize())));
    }

    /**
     * Generate a Token request URL from a request ID, and state. This does not set a CSRF token
     * or pass in a state.
     *
     * @param requestId request id
     * @return token request url
     */
    public String generateTokenRequestUrlBlocking(String requestId) {
        return generateTokenRequestUrl(requestId).blockingSingle();
    }

    /**
     * Generate a Token request URL from a request ID, and state. This does not set a CSRF token.
     *
     * @param requestId request id
     * @param state state
     * @return token request url
     */
    @Deprecated // set state on the TokenRequest builder
    public String generateTokenRequestUrlBlocking(String requestId, String state) {
        return generateTokenRequestUrl(requestId, state).blockingSingle();
    }

    /**
     * Generate a Token request URL from a request ID, a state, and a CSRF token.
     *
     * @param requestId request id
     * @param state state
     * @param csrfToken csrf token
     * @return token request url
     */
    @Deprecated // set state and csrf token on the TokenRequest builder
    public String generateTokenRequestUrlBlocking(
            String requestId,
            String state,
            String csrfToken) {
        return generateTokenRequestUrl(requestId, state, csrfToken).blockingSingle();
    }

    /**
     * Parse the token request callback URL to extract the state and the token ID. This assumes
     * that no CSRF token was set.
     *
     * @param callbackUrl token request callback url
     * @return TokenRequestCallback object containing the token id and the original state
     */
    @Deprecated // should use the CSRF token
    public Observable<TokenRequestCallback> parseTokenRequestCallbackUrl(final String callbackUrl) {
        return parseTokenRequestCallbackUrl(callbackUrl, "");
    }

    /**
     * Parse the token request callback URL to extract the state and the token ID. Check the
     * CSRF token against the initial request and verify the signature.
     *
     * @param callbackUrl token request callback url
     * @param csrfToken csrfToken
     * @return TokenRequestCallback object containing the token id and the original state
     */
    public Observable<TokenRequestCallback> parseTokenRequestCallbackUrl(
            final String callbackUrl,
            final String csrfToken) {
        try {
            String queryString = new URL(callbackUrl).getQuery();
            return parseTokenRequestCallbackParams(Util.parseQueryString(queryString), csrfToken);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid callback URL: " + callbackUrl);
        }
    }

    /**
     * Parse the Set Transfer Destinations Url callback parameters to extract state,
     * region and supported . Check the CSRF token against the initial request and verify
     * the signature.
     *
     * @param url token request callback url
     * @return TokenRequestSetTransferDestinationUrl object containing the token id and
     *         the original state
     */
    public TokenRequestTransferDestinationsCallbackParameters parseSetTransferDestinationsUrl(
            final String url) {
        Map<String, List<String>> parameters = new QueryStringDecoder(url).parameters();
        return parseSetTransferDestinationsUrlParams(parameters);
    }

    /**
     * Parse the token request callback URL to extract the state and the token ID. This assumes
     * that no CSRF token was set.
     *
     * @param callbackUrl token request callback url
     * @return TokenRequestCallback object containing the token id and the original state
     */
    @Deprecated // should use the CSRF token
    public TokenRequestCallback parseTokenRequestCallbackUrlBlocking(final String callbackUrl) {
        return parseTokenRequestCallbackUrl(callbackUrl).blockingSingle();
    }

    /**
     * Parse the token request callback URL to extract the state and the token ID. Check the
     * CSRF token against the initial request and verify the signature.
     *
     * @param callbackUrl token request callback url
     * @param csrfToken csrfToken
     * @return TokenRequestCallback object containing the token id and the original state
     */
    public TokenRequestCallback parseTokenRequestCallbackUrlBlocking(
            final String callbackUrl,
            final String csrfToken) {
        return parseTokenRequestCallbackUrl(callbackUrl, csrfToken).blockingSingle();
    }

    /**
     * Parse the token request callback parameters to extract the state and the token ID.
     * This assumes that no CSRF token was set.
     *
     * @param callbackParams callback parameter map
     * @return TokenRequestCallback object containing the token ID and the original state
     */
    @Deprecated // should use the CSRF token
    public Observable<TokenRequestCallback> parseTokenRequestCallbackParams(
            Map<String, String> callbackParams) {
        return parseTokenRequestCallbackParams(callbackParams, "");
    }

    /**
     * Parse the token request callback parameters to extract the state and the token ID. Check the
     * CSRF token against the initial request and verify the signature.
     *
     * @param callbackParams callback parameter map
     * @param csrfToken CSRF token
     * @return TokenRequestCallback object containing the token ID and the original state
     */
    public Observable<TokenRequestCallback> parseTokenRequestCallbackParams(
            final Map<String, String> callbackParams,
            final String csrfToken) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getTokenMember().map(new Function<MemberProtos.Member,
                TokenRequestCallback>() {
            @Override
            public TokenRequestCallback apply(MemberProtos.Member tokenMember) throws Exception {
                TokenRequestCallbackParameters params = TokenRequestCallbackParameters
                        .create(callbackParams);

                // check that CSRF token hashes match
                TokenRequestState state = TokenRequestState.parse(params.getSerializedState());
                if (!state.getCsrfTokenHash().equals(hashString(csrfToken))) {
                    throw new InvalidStateException(csrfToken);
                }

                verifySignature(
                        tokenMember,
                        TokenProtos.TokenRequestStatePayload.newBuilder()
                                .setTokenId(params.getTokenId())
                                .setState(urlEncode(params.getSerializedState()))
                                .build(),
                        params.getSignature());

                return TokenRequestCallback.create(
                        params.getTokenId(),
                        state.getInnerState());
            }
        });
    }

    /**
     * Parse the Set Transfer Destinations Url callback parameters to extract country,
     * bank and supported payments.
     *
     * @param urlParams url parameters
     * @return TokenRequestSetTransferDestinationUrl object containing region
     */
    public TokenRequestTransferDestinationsCallbackParameters parseSetTransferDestinationsUrlParams(
            final Map<String, List<String>> urlParams) {
        return TokenRequestTransferDestinationsCallbackParameters.create(urlParams);
    }

    /**
     * Parse the token request callback parameters to extract the state and the token ID.
     * This assumes that no CSRF token was set.
     *
     * @param callbackParams callback parameter map
     * @return TokenRequestCallback object containing the token ID and the original state
     */
    @Deprecated // should use the CSRF token
    public TokenRequestCallback parseTokenRequestCallbackParamsBlocking(
            Map<String, String> callbackParams) {
        return parseTokenRequestCallbackParams(callbackParams, "").blockingSingle();
    }

    /**
     * Parse the token request callback parameters to extract the state and the token ID. Check the
     * CSRF token against the initial request and verify the signature.
     *
     * @param callbackParams callback parameter map
     * @param csrfToken CSRF token
     * @return TokenRequestCallback object containing the token ID and the original state
     */
    public TokenRequestCallback parseTokenRequestCallbackParamsBlocking(
            final Map<String, String> callbackParams,
            final String csrfToken) {
        return parseTokenRequestCallbackParams(callbackParams, csrfToken).blockingSingle();
    }

    /**
     * Get the token request result based on a token's tokenRequestId.
     *
     * @param tokenRequestId token request id
     * @return token request result
     */
    public Observable<TokenRequestResult> getTokenRequestResult(String tokenRequestId) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.getTokenRequestResult(tokenRequestId);
    }

    /**
     * Get the token request result based on a token's tokenRequestId.
     *
     * @param tokenRequestId token request id
     * @return token request result
     */
    public TokenRequestResult getTokenRequestResultBlocking(String tokenRequestId) {
        return getTokenRequestResult(tokenRequestId).blockingSingle();
    }

    /**
     * Return a TokenRequest that was previously stored.
     *
     * @param requestId request id
     * @return token request that was stored with the request id
     */
    public Observable<TokenRequest> retrieveTokenRequest(String requestId) {
        UnauthenticatedClient unauthenticated = ClientFactory.unauthenticated(channel);
        return unauthenticated.retrieveTokenRequest(requestId);
    }

    /**
     * Return a TokenRequest that was previously stored.
     *
     * @param requestId request id
     * @return token request that was stored with the request id
     */
    public TokenRequest retrieveTokenRequestBlocking(String requestId) {
        return retrieveTokenRequest(requestId).blockingSingle();
    }

    public static final class Builder extends io.token.TokenClient.Builder<Builder> {
        /**
         * Creates new builder instance with the defaults initialized.
         */
        public Builder() {
            super();
        }

        @Override
        public TokenClient build() {
            Metadata headers = getHeaders();
            return new TokenClient(
                    RpcChannelFactoryLite.getInstance()
                            .builder(hostName, port, useSsl)
                            .withTimeout(timeoutMs)
                            .withMetadata(headers)
                            .withClientSsl(sslConfig)
                            .build(),
                    cryptoEngine != null
                            ? cryptoEngine
                            : new TokenCryptoEngineFactory(new InMemoryKeyStore()),
                    tokenCluster == null ? SANDBOX : tokenCluster);
        }

        @Override
        protected String getPlatform() {
            return "java-tpp";
        }
    }
}
