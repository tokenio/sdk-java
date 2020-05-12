/**
 * Copyright (c) 2019 Token, Inc.
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
import static java.util.concurrent.TimeUnit.MILLISECONDS;

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
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

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

    /**
     * Repetitively run a function until the result of the function is acceptable (defined
     * by <code>retryIf</code> predicate) or the total wait time exceeds the requested amount
     * (<code>timeoutMs</code>). Returns the last result of the function, acceptable or not
     * (in the case of timeout).
     *
     * @param timeoutMs a maximum total waiting time between retries
     * @param waitTimeMs initial wait time before retry
     * @param backOffFactor a factor by which the wait time is multiplied after each retry
     * @param maxWaitTimeMs max wait time between retries
     * @param function function that should be called
     * @param retryIf a boolean function that checks the result of the <code>function</code> and
     *     returns true if need to retry
     * @param <T> the type of the esult of the <code>function</code>
     * @return last result of the <code>function</code>
     * @throws Exception whatever checked exceptions <code>function</code> throws
     * @throws InterruptedException if any thread has interrupted the current thread
     * @throws IllegalArgumentException if any of the time arguments or the
     *     <code>backOffFactor</code> is negative
     */
    public static <T> T retryWithExponentialBackoff(
            long timeoutMs,
            long waitTimeMs,
            double backOffFactor,
            long maxWaitTimeMs,
            Callable<T> function,
            Predicate<T> retryIf) throws Exception {
        if (timeoutMs < 0 || waitTimeMs < 0 || backOffFactor < 0 || maxWaitTimeMs < 0) {
            throw new IllegalArgumentException(
                    "All time arguments and the backOffFactor should be non-negative.");
        }
        long totalTime = 0;
        T result = function.call();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        while (retryIf.test(result)) {
            if (totalTime >= timeoutMs) {
                return result;
            }
            result = scheduler.schedule(function, waitTimeMs, MILLISECONDS).get();
            totalTime += waitTimeMs;
            waitTimeMs = Math.min((long) (waitTimeMs * backOffFactor), maxWaitTimeMs);
        }
        return result;
    }

    /**
     * Repetitively run a function until the result of the function is acceptable (defined
     * by <code>retryIf</code> predicate) or the total wait time exceeds the requested amount
     * (<code>timeoutMs</code>). Returns the last result of the function, acceptable or not
     * (in the case of timeout).<br><br>
     * To be used for functions that do not throw checked exceptions.
     *
     * @param timeoutMs a maximum total waiting time between retries
     * @param waitTimeMs initial wait time before retry
     * @param backOffFactor a factor by which the wait time is multiplied after each retry
     * @param maxWaitTimeMs max wait time between retries
     * @param function function that should be called
     * @param retryIf a boolean function that checks the result of the <code>function</code> and
     *     returns true if need to retry
     * @param <T> the type of the esult of the <code>function</code>
     * @return last result of the <code>function</code>
     * @throws InterruptedException if any thread has interrupted the current thread
     * @throws IllegalArgumentException if any of the time arguments or the
     *     <code>backOffFactor</code> is negative
     */
    public static <T> T retryWithExponentialBackoffNoThrow(
            long timeoutMs,
            long waitTimeMs,
            double backOffFactor,
            long maxWaitTimeMs,
            Callable<T> function,
            Predicate<T> retryIf) throws InterruptedException {
        try {
            return retryWithExponentialBackoff(
                    timeoutMs,
                    waitTimeMs,
                    backOffFactor,
                    maxWaitTimeMs,
                    function,
                    retryIf);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
