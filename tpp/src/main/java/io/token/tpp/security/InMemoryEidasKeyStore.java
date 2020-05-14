package io.token.tpp.security;

import static io.token.exceptions.KeyNotFoundException.keyNotFoundForId;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static java.lang.String.format;

import io.token.proto.common.security.SecurityProtos.Key.Level;
import io.token.security.SecretKey;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import java.util.List;

/**
 * In memory implementation of the {@link EidasKeyStore}. Used for testing.
 */
public final class InMemoryEidasKeyStore implements EidasKeyStore {
    private final SecretKey key;
    private final X509Certificate eidasCertificate;

    public InMemoryEidasKeyStore(X509Certificate eidasCertificate, PrivateKey privateKey) {
        this.eidasCertificate = eidasCertificate;
        this.key = SecretKey.create(
                getCertificateSerialNumber().toString(),
                PRIVILEGED,
                new KeyPair(eidasCertificate.getPublicKey(), privateKey));
    }

    public InMemoryEidasKeyStore(String eidasCertificateDer, PrivateKey privateKey) throws CertificateException {
        this(extractCertificate(eidasCertificateDer), privateKey);
    }

    public InMemoryEidasKeyStore(File certificatePemFile, File privateKeyPemFile) throws
            CertificateException, InvalidKeySpecException, IOException {
        this(
                readCertificateFromFile(certificatePemFile),
                readPrivateKeyFromFile(privateKeyPemFile));
    }

    @Override
    public SecretKey getKey() {
        return key;
    }

    @Override
    public BigInteger getCertificateSerialNumber() {
        return eidasCertificate.getSerialNumber();
    }

    @Override
    public void put(String memberId, SecretKey key) {
        throw new UnsupportedOperationException("This key store does not accept new keys - "
                + "it stores the only key provided at the moment of the store creation.");
    }

    @Override
    public X509Certificate getEidasCertificate() {
        return eidasCertificate;
    }

    @Override
    public void deleteKeys(String memberId) {
        throw new UnsupportedOperationException("This key store does not support key rotation - "
                + "it always stores the only key provided at the moment of the store creation.");
    }

    private static X509Certificate readCertificateFromFile(File file)
            throws CertificateException, IOException {
        FileInputStream is = new FileInputStream(file);
        return (X509Certificate) CertificateFactory
                .getInstance("X.509")
                .generateCertificate(is);
    }

    private static PrivateKey readPrivateKeyFromFile(File file)
            throws IOException, InvalidKeySpecException {
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Files.readAllBytes(file.toPath()));
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create X509Certificate from the DER encoded certificate.
     *
     * @param derBase64Cert DER encoded certificate
     * @return a certificate
     */
    public static X509Certificate extractCertificate(String derBase64Cert)
            throws CertificateException {
        String cert = format("-----BEGIN CERTIFICATE-----\n%s-----END CERTIFICATE-----",
                derBase64Cert);
        InputStream is = new ByteArrayInputStream(cert.getBytes());
        return (X509Certificate) CertificateFactory
                .getInstance("X.509")
                .generateCertificate(is);
    }
}
