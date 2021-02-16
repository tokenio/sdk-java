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

package io.token.tpp.security;

import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static java.lang.String.format;

import io.token.security.SecretKey;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * In memory implementation of the {@link EidasKeyStore}. Used for testing.
 */
public final class InMemoryEidasKeyStore implements EidasKeyStore {
    private final SecretKey key;
    private final X509Certificate eidasCertificate;

    /**
     * Creates a key store with a single private key and a corresponding certificate.
     *
     * @param eidasCertificate an eIDAS certificate
     * @param privateKey a private key
     */
    public InMemoryEidasKeyStore(X509Certificate eidasCertificate, PrivateKey privateKey) {
        this.eidasCertificate = eidasCertificate;
        this.key = SecretKey.create(
                getCertificateSerialNumber().toString(),
                PRIVILEGED,
                new KeyPair(eidasCertificate.getPublicKey(), privateKey));
    }

    /**
     * Creates a key store with a single private key and a corresponding certificate.
     *
     * @param eidasCertificate a base64-encoded eIDAS certificate without a footer and a header
     * @param privateKey a private key
     * @throws CertificateException if an exception occurred while parsing the certificate
     */
    public InMemoryEidasKeyStore(String eidasCertificate, PrivateKey privateKey)
            throws CertificateException {
        this(extractCertificate(eidasCertificate), privateKey);
    }

    /**
     * Creates a key store with a single private key and a corresponding certificate.
     *
     * @param certificatePemFile a file with a certificate in PEM format
     * @param privateKeyPemFile a file with a key in PEM format
     * @throws GeneralSecurityException if an exception occurred while reading the certificate/key
     * @throws IOException if an I/O error occurs while reading from the files
     */
    public InMemoryEidasKeyStore(File certificatePemFile, File privateKeyPemFile)
            throws GeneralSecurityException, IOException {
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
    public X509Certificate getCertificate() {
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

    private static X509Certificate extractCertificate(String certString)
            throws CertificateException {
        String cert = format(
                "-----BEGIN CERTIFICATE-----\n%s-----END CERTIFICATE-----",
                certString);
        InputStream is = new ByteArrayInputStream(cert.getBytes());
        return (X509Certificate) CertificateFactory
                .getInstance("X.509")
                .generateCertificate(is);
    }
}
