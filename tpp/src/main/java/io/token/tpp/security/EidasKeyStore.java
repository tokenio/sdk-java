/**
 * Copyright (c) 2020 Token, Inc.
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

import static io.token.exceptions.KeyNotFoundException.keyNotFoundForId;

import io.token.proto.common.security.SecurityProtos.Key.Level;
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
    /**
     * Get a certificate serial number. Used as a key ID.
     *
     * @return a serial number of the certificate
     * @see <a href="https://tools.ietf.org/html/rfc5280#section-4.1.2.2">Serial Number Standard</a>
     */
    BigInteger getCertificateSerialNumber();

    /**
     * Get a certificate stored by this key store.
     *
     * @return a certificate
     */
    X509Certificate getCertificate();

    /**
     * Get a key stored by this key store.
     *
     * @return a secret key
     */
    SecretKey getKey();

    @Override
    default SecretKey getById(String memberId, String keyId) {
        if (!getKey().getId().equals(keyId)) {
            throw keyNotFoundForId(keyId);
        }
        return getKey();
    }

    @Override
    default SecretKey getByLevel(String memberId, Level keyLevel) {
        return getKey();
    }

    @Override
    default List<SecretKey> listKeys(String memberId) {
        return Collections.singletonList(getKey());
    }
}
