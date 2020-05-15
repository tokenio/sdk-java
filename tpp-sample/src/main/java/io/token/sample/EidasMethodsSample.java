package io.token.sample;

import static com.google.common.io.BaseEncoding.base64;
import static io.token.proto.AliasHasher.normalize;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.BANK;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EIDAS;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.security.crypto.CryptoType.RS256;
import static java.lang.String.format;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.eidas.EidasProtos.EidasRecoveryPayload;
import io.token.proto.common.eidas.EidasProtos.RegisterWithEidasPayload;
import io.token.proto.common.eidas.EidasProtos.VerifyEidasPayload;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.security.SecurityProtos.Key.Algorithm;
import io.token.proto.gateway.Gateway.GetEidasVerificationStatusResponse;
import io.token.proto.gateway.Gateway.RegisterWithEidasResponse;
import io.token.proto.gateway.Gateway.VerifyEidasResponse;
import io.token.security.CryptoEngine;
import io.token.security.InMemoryKeyStore;
import io.token.security.SecretKey;
import io.token.security.Signer;
import io.token.security.TokenCryptoEngine;
import io.token.security.crypto.Crypto;
import io.token.security.crypto.CryptoRegistry;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;
import io.token.tpp.exceptions.EidasTimeoutException;
import io.token.tpp.security.EidasKeyStore;

import java.security.PrivateKey;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class EidasMethodsSample {

    /**
     * Creates a TPP member and verifies it using eIDAS certificate.
     *
     * @param client token client
     * @param tppAuthNumber authNumber of the TPP
     * @param certificate base64 encoded eIDAS certificate (a single line, no header and footer)
     * @param bankId id of the bank the TPP trying to get access to
     * @param privateKey private key corresponding to the public key in the certificate
     * @return verified business member
     */
    public static Member verifyEidas(
            TokenClient client,
            String tppAuthNumber,
            String certificate,
            String bankId,
            PrivateKey privateKey) {
        Algorithm signingAlgorithm = Algorithm.RS256;
        Crypto crypto = CryptoRegistry.getInstance().cryptoFor(signingAlgorithm);
        Signer signer = crypto.signer("eidas", privateKey);

        // resolve memberId of the bank TPP is trying to get access to
        String bankMemberId = client
                .resolveAliasBlocking(Alias.newBuilder().setValue(bankId).setType(BANK).build())
                .getId();
        // create an eIDAS alias under realm of the target bank
        Alias eidasAlias = normalize(Alias.newBuilder()
                .setValue(tppAuthNumber)
                .setRealmId(bankMemberId)
                .setType(EIDAS)
                .build());
        // create a member under realm of the bank with eIDAS alias
        Member tpp = client.createMember(eidasAlias, null, bankMemberId).blockingSingle();
        // construct a payload with all the required data
        VerifyEidasPayload payload = VerifyEidasPayload
                .newBuilder()
                .setAlgorithm(signingAlgorithm)
                .setAlias(eidasAlias)
                .setCertificate(certificate)
                .setMemberId(tpp.memberId())
                .build();
        // verify eIDAS
        VerifyEidasResponse response = tpp
                .verifyEidas(payload, signer.sign(payload))
                .blockingSingle();
        // get the verification status (useful if verifyEidas response has IN_PROGRESS status)
        GetEidasVerificationStatusResponse statusResponse = tpp
                .getEidasVerificationStatus(response.getVerificationId())
                .blockingSingle();

        return tpp;
    }

    /**
     * Recovers a TPP member and verifies its EIDAS alias using eIDAS certificate.
     *
     * @param client token client
     * @param memberId id of the member to be recovered
     * @param tppAuthNumber authNumber of the TPP
     * @param certificate base64 encoded eIDAS certificate (a single line, no header and footer)
     * @param certificatePrivateKey private key corresponding to the public key in the certificate
     * @return verified business member
     */
    public static Member recoverEidas(
            TokenClient client,
            String memberId,
            String tppAuthNumber,
            String certificate,
            PrivateKey certificatePrivateKey) {
        // create a signer using the certificate private key
        Algorithm signingAlgorithm = Algorithm.RS256;
        Crypto crypto = CryptoRegistry.getInstance().cryptoFor(signingAlgorithm);
        Signer payloadSigner = crypto.signer("eidas", certificatePrivateKey);

        // generate a new privileged key to add to the member
        CryptoEngine cryptoEngine = new TokenCryptoEngine(memberId, new InMemoryKeyStore());
        SecurityProtos.Key newKey = cryptoEngine.generateKey(PRIVILEGED);

        // construct a payload with all the required data
        EidasRecoveryPayload payload = EidasRecoveryPayload
                .newBuilder()
                .setMemberId(memberId)
                .setCertificate(certificate)
                .setAlgorithm(signingAlgorithm)
                .setKey(newKey)
                .build();

        Member recoveredMember = client
                .recoverEidasMember(payload, payloadSigner.sign(payload), cryptoEngine)
                .blockingSingle();

        // the eidas alias becomes unverified after the recovery, so we need to verify it again
        Alias eidasAlias = normalize(Alias.newBuilder()
                .setValue(tppAuthNumber)
                .setRealmId(recoveredMember.realmId())
                .setType(EIDAS)
                .build());
        VerifyEidasPayload verifyPayload = VerifyEidasPayload.newBuilder()
                .setMemberId(memberId)
                .setAlias(eidasAlias)
                .setCertificate(certificate)
                .setAlgorithm(signingAlgorithm)
                .build();

        recoveredMember
                .verifyEidas(verifyPayload, payloadSigner.sign(verifyPayload))
                .blockingSingle();

        return recoveredMember;
    }

    /**
     * Creates a TPP member under realm of a bank and registers it with the provided eIDAS
     * certificate. The created member has a registered PRIVILEGED-level RSA key from the provided
     * certificate and an EIDAS alias with value equal to authNumber from the certificate.<br><br>
     * Note, that tokenClient needs to be created with a CryptoEngine backed by a key store
     * that contains a key pair for the eIDAS certificate to use for the registration:<br><br>
     * <pre>
     * EidasKeyStore keyStore = new InMemoryEidasKeyStore(certificate, privateKey);
     * TokenClient tokenClient = TokenClient.builder()
     *         .connectTo(SANDBOX)
     *         .withCryptoEngine(new EidasCryptoEngineFactory(keyStore))
     *         .build();
     * </pre>
     *
     * @param tokenClient token client
     * @param keyStore a key store that is used by token client and contains eIDAS key pair for the
     *      provided certificate
     * @param bankId id of the bank the TPP trying to get access to
     * @return a newly created member, which might not be onboarded yet
     * @throws Exception if an exception occurs
     */
    public static Member registerWithEidas(
            TokenClient tokenClient,
            EidasKeyStore keyStore,
            String bankId) throws Exception {
        // create a signer using the certificate private key
        SecretKey keyPair = keyStore.getKey();
        Signer payloadSigner = CryptoRegistry
                .getInstance()
                .cryptoFor(RS256)
                .signer(keyPair.getId(), keyPair.getPrivateKey());

        RegisterWithEidasPayload payload = RegisterWithEidasPayload
                .newBuilder()
                .setCertificate(base64().encode(keyStore.getCertificate().getEncoded()))
                .setBankId(bankId)
                .build();

        RegisterWithEidasResponse resp = tokenClient
                .registerWithEidas(payload, payloadSigner.sign(payload))
                .blockingSingle();

        // now we can load a member and also check a status of the certificate verification
        Member member = tokenClient.getMemberBlocking(resp.getMemberId());
        GetEidasVerificationStatusResponse statusResp = member
                .getEidasVerificationStatus(resp.getVerificationId())
                .blockingSingle();

        return member;
    }

    /**
     * Creates a TPP member under realm of a bank and registers it with the provided eIDAS
     * certificate. The created member has a registered PRIVILEGED-level RSA key from the provided
     * certificate and an EIDAS alias with value equal to authNumber from the certificate.<br><br>
     * Note, that tokenClient needs to be created with a CryptoEngine backed by a key store
     * that contains a key pair for the eIDAS certificate to use for the registration:<br><br>
     * <pre>
     * EidasKeyStore keyStore = new InMemoryEidasKeyStore(certificate, privateKey);
     * TokenClient tokenClient = TokenClient.builder()
     *         .connectTo(SANDBOX)
     *         .withCryptoEngine(new EidasCryptoEngineFactory(keyStore))
     *         .build();
     * </pre>
     *
     * @param tokenClient token client
     * @param keyStore a key store that is used by token client and contains eIDAS certificate and
     *      a private key
     * @param bankId id of the bank the TPP trying to get access to
     * @return a newly created and oboarded member
     * @throws Exception if an exception occurs
     */
    public static Optional<Member> createMemberWithEidas(
            TokenClient tokenClient,
            EidasKeyStore keyStore,
            String bankId) throws Exception {
        Member member = null;
        try {
            member = tokenClient.createMemberWithEidas(bankId, keyStore, 30, TimeUnit.SECONDS);
        } catch (EidasTimeoutException ex) {
            System.out.println(format(
                    "Unable to complete eIDAS verification: memberId=%s | verivicationId=%s",
                    ex.getMemberId(),
                    ex.getVerificationId()));
        }
        return Optional.ofNullable(member);
    }
}
