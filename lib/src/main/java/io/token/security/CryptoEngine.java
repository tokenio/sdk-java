/**
 * Copyright (c) 2017 Token, Inc.
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

package io.token.security;

import io.token.proto.common.security.SecurityProtos.Key;

import java.util.List;

/**
 * Provides cryptographic support for secret management.
 */
public interface CryptoEngine {
    /**
     * Generates keys of the specified level. If the key with the specified level
     * already exists, it is replaced. Old key is still kept around because it
     * could be used for signature verification later.
     *
     * @param keyLevel key privilege level
     * @return newly generated key information
     */
    Key generateKey(Key.Level keyLevel);

    /**
     * Signs the data with the identified by the supplied key id.
     *
     * @param keyLevel level of the key to use
     * @return signer that is used to generate digital signatures
     */
    Signer createSigner(Key.Level keyLevel);

    /**
     * Verifies the payload signature.
     *
     * @param keyId key id
     * @return signature verifier
     */
    Verifier createVerifier(String keyId);

    /**
     * Returns public keys that the CryptoEngine can use to sign.
     *
     * @return list of public keys
     */
    List<Key> getPublicKeys();
}
