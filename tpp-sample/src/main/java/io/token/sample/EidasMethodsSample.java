package io.token.sample;

import static io.token.proto.AliasHasher.normalize;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.BANK;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EIDAS;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.util.Util.generateNonce;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.eidas.EidasProtos;
import io.token.proto.common.eidas.EidasProtos.EidasRecoveryPayload;
import io.token.proto.common.eidas.EidasProtos.VerifyEidasPayload;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.security.SecurityProtos.Key.Algorithm;
import io.token.proto.gateway.Gateway.GetEidasVerificationStatusResponse;
import io.token.proto.gateway.Gateway.RegisterWithEidasResponse;
import io.token.proto.gateway.Gateway.VerifyEidasResponse;
import io.token.security.CryptoEngine;
import io.token.security.InMemoryKeyStore;
import io.token.security.KeyStore;
import io.token.security.SecretKey;
import io.token.security.Signer;
import io.token.security.TokenCryptoEngine;
import io.token.security.crypto.Crypto;
import io.token.security.crypto.CryptoRegistry;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

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
     * Note, that tokenClient needs to be created with a CryptoEngine that handles RSA keys, for
     * example:<br><br>
     * <pre>
     * CryptoEngineFactory cryptoEngineFactory = new TokenCryptoEngineFactory(
     *         keyStore,
     *         CryptoType.RS256);
     * TokenClient tokenClient = TokenClient.builder()
     *         .connectTo(SANDBOX)
     *         .withCryptoEngine(cryptoEngineFactory)
     *         .build();
     * </pre>
     *
     * @param tokenClient token client
     * @param keyStore a key store that is used by token client (can be empty)
     * @param bankId id of the bank the TPP trying to get access to
     * @param eidasKeyPair eIDAS key pair for the provided certificate
     * @param certificate base64 encoded eIDAS certificate (a single line, no header and footer)
     * @return a newly created member
     */
    public static Member registerWithEidas(
            TokenClient tokenClient,
            KeyStore keyStore,
            String bankId,
            KeyPair eidasKeyPair,
            String certificate) {
        // create a signer using the certificate private key
        Algorithm signingAlgorithm = Algorithm.RS256;
        Crypto crypto = CryptoRegistry.getInstance().cryptoFor(signingAlgorithm);
        // key id is not important here
        Signer payloadSigner = crypto.signer(generateNonce(), eidasKeyPair.getPrivate());

        EidasProtos.RegisterWithEidasPayload payload = EidasProtos.RegisterWithEidasPayload
                .newBuilder()
                .setCertificate(certificate)
                .setBankId(bankId)
                .build();

        RegisterWithEidasResponse resp = tokenClient
                .registerWithEidas(payload, payloadSigner.sign(payload))
                .blockingSingle();
        String memberId = resp.getMemberId();
        // don't forget to add the registered key to the key store used by the tokenClient
        keyStore.put(memberId, SecretKey.create(resp.getKeyId(), PRIVILEGED, eidasKeyPair));

        // now we can load a member and also check a status of the certificate verification
        Member member = tokenClient.getMemberBlocking(memberId);
        GetEidasVerificationStatusResponse statusResp = member
                .getEidasVerificationStatus(resp.getVerificationId())
                .blockingSingle();

        return member;
    }

    public static Member registerWithEidasBlocking(
            TokenClient tokenClient,
            KeyStore keyStore,
            String bankId,
            X509Certificate certificate,
            PrivateKey privateKey) {
        return tokenClient.registerWithEidasBlocking(bankId, certificate, privateKey, keyStore);
    }
}
