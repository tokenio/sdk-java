package io.token.tpp.security;

import static io.token.exceptions.KeyNotFoundException.keyNotFoundForId;

import io.token.proto.common.security.SecurityProtos;
import io.token.security.KeyStore;
import io.token.security.SecretKey;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

/**
 * A single-key store to store a key from an eIDAS certificate. It returns the same key
 * regardless of the memberId or level requested.<br>
 * Key ID is equal to the decimal String representation of the
 * certificate's serial number (returned by {@link EidasKeyStore#getCertificateSerialNumber()}).
 */

public interface EidasKeyStore extends KeyStore {
    BigInteger getCertificateSerialNumber();

    /**
     * See https://tools.ietf.org/html/rfc5280#section-4.1.2.2
     *
     * @return
     */
    X509Certificate getEidasCertificate();

    /**
     * The key stored by this key store.
     *
     * @return
     */
    SecretKey getKey();

    @Override
    default SecretKey getById(String memberId, String keyId) {
        if(!getKey().getId().equals(keyId)) {
            throw keyNotFoundForId(keyId);
        }
        return getKey();
    }

    @Override
    default SecretKey getByLevel(String memberId, SecurityProtos.Key.Level keyLevel) {
        return getKey();
    }

    @Override
    default List<SecretKey> listKeys(String memberId) {
        return Collections.singletonList(getKey());
    }
}
