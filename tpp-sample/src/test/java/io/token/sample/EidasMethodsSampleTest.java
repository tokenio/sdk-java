package io.token.sample;

import static io.token.proto.common.alias.AliasProtos.Alias.Type.EIDAS;
import static io.token.proto.common.eidas.EidasProtos.EidasCertificateStatus.CERTIFICATE_VALID;
import static io.token.sample.TestUtil.createClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.common.io.BaseEncoding;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.gateway.Gateway.GetEidasCertificateStatusResponse;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
    private static BouncyCastleProvider bcProvider = new BouncyCastleProvider();

    @Test
    public void verifyEidasTest() throws Exception {
        try (TokenClient tokenClient = createClient()) {
            String tppAuthNumber = RandomStringUtils.randomAlphanumeric(15);
            KeyPair keyPair = generateKeyPair();
            String certificate = generateCert(keyPair, tppAuthNumber);
            Member verifiedTppMember = EidasMethodsSample.verifyEidas(
                    tokenClient,
                    tppAuthNumber,
                    certificate,
                    "gold",
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
            String certificate = generateCert(keyPair, tppAuthNumber);
            String bankId = "gold";
            // create and verify member first
            Member verifiedTppMember = EidasMethodsSample.verifyEidas(
                    tokenClient,
                    tppAuthNumber,
                    certificate, bankId,
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

    private static KeyPair generateKeyPair() throws Exception {
        Security.addProvider(bcProvider);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    private static String generateCert(KeyPair keyPair, String tppAuthNumber) throws Exception {
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
        X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider(bcProvider)
                .getCertificate(certBuilder.build(contentSigner));
        return BaseEncoding.base64().encode(certificate.getEncoded());
    }
}
