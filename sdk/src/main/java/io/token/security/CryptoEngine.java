package io.token.security;

import io.token.proto.common.security.SecurityProtos.Key;

public interface CryptoEngine {
    /**
     * Generates a keys of the specified level. If the key with the specified level
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
}
