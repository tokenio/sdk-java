package io.token.sample;

import static com.google.common.io.BaseEncoding.base64;
import static com.google.common.io.BaseEncoding.base64Url;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EIDAS;
import static io.token.proto.common.eidas.EidasProtos.EidasCertificateStatus.CERTIFICATE_VALID;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.sample.EidasMethodsSample.createMemberWithEidas;
import static io.token.sample.EidasMethodsSample.registerWithEidas;
import static io.token.sample.TestUtil.createClient;
import static io.token.util.Util.generateNonce;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.gateway.Gateway.GetEidasCertificateStatusResponse;
import io.token.security.CryptoEngineFactory;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;
import io.token.tpp.security.EidasCryptoEngineFactory;
import io.token.tpp.security.EidasKeyStore;
import io.token.tpp.security.InMemoryEidasKeyStore;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.Test;

public class EidasMethodsSampleTest {
    private static final BouncyCastleProvider bcProvider = new BouncyCastleProvider();
    private static final String directBankId = "gold";

    @Test
    public void verifyEidasTest() throws Exception {
        try (TokenClient tokenClient = createClient()) {
            String tppAuthNumber = RandomStringUtils.randomAlphanumeric(15);
            KeyPair keyPair = generateKeyPair();
            String certificate = encode(generateCert(keyPair, tppAuthNumber));
            Member verifiedTppMember = EidasMethodsSample.verifyEidas(
                    tokenClient,
                    tppAuthNumber,
                    certificate,
                    directBankId,
                    keyPair.getPrivate());
            List<Alias> verifiedAliases = verifiedTppMember.aliasesBlocking();
            assertThat(verifiedAliases.size()).isEqualTo(1);
            assertThat(verifiedAliases.get(0).getValue()).isEqualTo(tppAuthNumber);
            assertThat(verifiedAliases.get(0).getType()).isEqualTo(EIDAS);
            GetEidasCertificateStatusResponse eidasInfo = verifiedTppMember
                    .getEidasCertificateStatus()
                    .blockingSingle();
            assertThat(eidasInfo.getCertificate()).isEqualTo(certificate);
            assertThat(eidasInfo.getStatus()).isEqualTo(CERTIFICATE_VALID);
        }
    }

    @Test
    public void recoverEidasTest() throws Exception {
        try (TokenClient tokenClient = createClient();
             TokenClient anotherTokenClient = createClient();) {
            String tppAuthNumber = RandomStringUtils.randomAlphanumeric(15);
            KeyPair keyPair = generateKeyPair();
            String certificate = encode(generateCert(keyPair, tppAuthNumber));
            // create and verify member first
            Member verifiedTppMember = EidasMethodsSample.verifyEidas(
                    tokenClient,
                    tppAuthNumber,
                    certificate,
                    directBankId,
                    keyPair.getPrivate());

            // now pretend we lost the keys and need to recover the member
            Member recoveredMember = EidasMethodsSample.recoverEidas(
                    anotherTokenClient,
                    verifiedTppMember.memberId(),
                    tppAuthNumber,
                    certificate,
                    keyPair.getPrivate());
            List<Alias> verifiedAliases = recoveredMember.aliasesBlocking();
            assertThat(verifiedAliases.size()).isEqualTo(1);
            assertThat(verifiedAliases.get(0).getValue()).isEqualTo(tppAuthNumber);
            assertThat(verifiedAliases.get(0).getType()).isEqualTo(EIDAS);
        }
    }

    @Test
    public void registerWithEidasTest() throws Exception {
        String authNumber = generateNonce();
        KeyPair keyPair = generateKeyPair();
        X509Certificate certificate = generateCert(keyPair, authNumber);
        EidasKeyStore keyStore = new InMemoryEidasKeyStore(certificate, keyPair.getPrivate());
        CryptoEngineFactory cryptoEngineFactory = new EidasCryptoEngineFactory(keyStore);
        try (TokenClient tokenClient = createClient(cryptoEngineFactory)) {
            Member member = registerWithEidas(tokenClient, keyStore, directBankId);
            List<SecurityProtos.Key> keys = member.getKeys().blockingSingle();
            assertThat(keys.get(0).getLevel()).isEqualTo(PRIVILEGED);
            assertThat(keys.get(0).getPublicKey()).isEqualTo(
                    base64Url().encode(keyPair.getPublic().getEncoded()));
            assertThat(keys.get(0).getId()).isEqualTo(keyStore
                    .getCertificateSerialNumber()
                    .toString());
            assertThat(member.aliases().blockingSingle().get(0).getValue()).isEqualTo(authNumber);
        }
    }

    @Test
    public void createMemberWithEidasTest() throws Exception {
        String authNumber = generateNonce();
        KeyPair keyPair = generateKeyPair();
        X509Certificate certificate = generateCert(keyPair, authNumber);
        EidasKeyStore keyStore = new InMemoryEidasKeyStore(certificate, keyPair.getPrivate());
        CryptoEngineFactory cryptoEngineFactory = new EidasCryptoEngineFactory(keyStore);
        try (TokenClient tokenClient = createClient(cryptoEngineFactory)) {
            Optional<Member> memberOpt = createMemberWithEidas(tokenClient, keyStore, directBankId);
            assertThat(memberOpt).isPresent();
            Member member = memberOpt.get();
            List<SecurityProtos.Key> keys = member.getKeys().blockingSingle();
            assertThat(keys.get(0).getLevel()).isEqualTo(PRIVILEGED);
            assertThat(keys.get(0).getPublicKey()).isEqualTo(
                    base64Url().encode(keyPair.getPublic().getEncoded()));
            assertThat(keys.get(0).getId()).isEqualTo(keyStore
                    .getCertificateSerialNumber()
                    .toString());
            assertThat(member.aliases().blockingSingle().get(0).getValue()).isEqualTo(authNumber);
        }
    }

    private static KeyPair generateKeyPair() throws Exception {
        Security.addProvider(bcProvider);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    private static String encode(X509Certificate certificate) throws Exception {
        return base64().encode(certificate.getEncoded());
    }

    private static X509Certificate generateCert(KeyPair keyPair, String tppAuthNumber)
            throws Exception {
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);

        ASN1ObjectIdentifier asn1oid = new ASN1ObjectIdentifier("2.5.4.97");
        X500Name dnName = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, "Token.io")
                .addRDN(asn1oid, tppAuthNumber)
                .build();
        BigInteger certSerialNumber = new BigInteger(Long.toString(now));
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.YEAR, 1);
        Date endDate = calendar.getTime();
        String signatureAlgorithm = "SHA256WithRSA";

        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(
                keyPair.getPrivate());

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                dnName,
                certSerialNumber,
                startDate,
                endDate,
                dnName,
                keyPair.getPublic());

        // Extensions --------------------------
        BasicConstraints basicConstraints = new BasicConstraints(true);
        certBuilder.addExtension(
                new ASN1ObjectIdentifier("2.5.29.19"),
                true,
                basicConstraints);
        // -------------------------------------
        return new JcaX509CertificateConverter()
                .setProvider(bcProvider)
                .getCertificate(certBuilder.build(contentSigner));
    }
}
