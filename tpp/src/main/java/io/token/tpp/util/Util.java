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

package io.token.tpp.util;

import static io.token.proto.common.alias.AliasProtos.Alias.Type.DOMAIN;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.hash.Hashing;
import com.google.protobuf.Message;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.security.SecurityProtos;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.security.KeyNotFoundException;
import io.token.security.crypto.Crypto;
import io.token.security.crypto.CryptoRegistry;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods.
 */
public abstract class Util extends io.token.util.Util {
    /**
     * The token alias.
     */
    public static final Alias TOKEN = Alias.newBuilder()
            .setType(DOMAIN)
            .setValue("token.io")
            .setRealm(TOKEN_REALM)
            .build();

    private Util() {
    }

    /**
     * Returns map of query string parameters, given a query string.
     *
     * @param queryString query string
     * @return map of parameters in query string
     */
    public static Map<String, String> parseQueryString(String queryString) {
        String[] params = queryString.split("&");
        Map<String, String> parameters = new HashMap<>();

        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            parameters.put(name, value);
        }
        return parameters;
    }

    /**
     * URL encodes a string.
     *
     * @param string to encode
     * @return encoded string
     */
    public static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * URL decodes a string.
     *
     * @param string to decode
     * @return decoded string
     */
    public static String urlDecode(String string) {
        try {
            return URLDecoder.decode(string, UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Verify the signature of the payload.
     *
     * @param member member
     * @param payload payload
     * @param signature signature
     */
    public static void verifySignature(
            Member member,
            Message payload,
            Signature signature) {
        SecurityProtos.Key key = getSigningKey(member, signature);

        Crypto crypto = CryptoRegistry.getInstance().cryptoFor(key.getAlgorithm());
        PublicKey publicKey = crypto.toPublicKey(key.getPublicKey());
        crypto.verifier(publicKey).verify(payload, signature.getSignature());
    }

    /**
     * Get the key corresponding to a signature.
     *
     * @param member member
     * @param signature signature
     * @return signing key if member owns it.
     */
    public static SecurityProtos.Key getSigningKey(Member member, Signature signature) {
        SecurityProtos.Key key = null;
        String keyId = signature.getKeyId();

        for (SecurityProtos.Key k : member.getKeysList()) {
            if (k.getId().equals(keyId)) {
                key = k;
                break;
            }
        }

        if (key == null) {
            throw new KeyNotFoundException(keyId);
        }

        return key;
    }
}
