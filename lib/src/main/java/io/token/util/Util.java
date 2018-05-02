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

package io.token.util;

import static io.token.proto.ProtoHasher.hashAndSerializeJson;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.DOMAIN;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.USERNAME;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.Message;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.Function;
import io.token.Account;
import io.token.AccountAsync;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.member.MemberProtos.Member;
import io.token.proto.common.member.MemberProtos.MemberAddKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberAliasOperation;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata.AddAliasMetadata;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.security.KeyNotFoundException;
import io.token.security.crypto.Crypto;
import io.token.security.crypto.CryptoRegistry;
import io.token.util.codec.ByteEncoding;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;


/**
 * Utility methods.
 */
public abstract class Util {

    /**
     * The token alias.
     */
    public static final Alias TOKEN = Alias.newBuilder()
            .setType(DOMAIN)
            .setValue("token.io")
            .build();

    private static final int NONCE_NUM_BYTES = 20;

    private Util() {
    }

    /**
     * Generates a random string.
     *
     * @return generated random string
     */
    public static String generateNonce() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[NONCE_NUM_BYTES];
        random.nextBytes(bytes);
        return ByteEncoding.serializeHumanReadable(bytes);
    }

    /**
     * Converts Key to AddKey operation.
     *
     * @param key key to add
     * @return member operation
     */
    public static MemberOperation toAddKeyOperation(Key key) {
        return MemberOperation
                .newBuilder()
                .setAddKey(MemberAddKeyOperation
                        .newBuilder()
                        .setKey(key))
                .build();
    }

    /**
     * Get alias with normalized value. E.g. "Captain@gmail.com" to "captain@gmail.com".
     *
     * @param rawAlias { EMAIL, "Captain@gmail.com" }
     * @return alias with possibly-different value field
     */
    public static Alias normalizeAlias(Alias rawAlias) {
        switch (rawAlias.getType()) {
            case EMAIL:
            case DOMAIN:
                return rawAlias.toBuilder()
                        .setValue(rawAlias.getValue().toLowerCase().trim())
                        .build();

            default:
                return rawAlias.toBuilder()
                        .build();
        }
    }

    /**
     * Converts alias to AddAlias operation.
     *
     * @param alias alias to add
     * @param realm realm
     * @return member operation
     */
    public static MemberOperation toAddAliasOperation(Alias alias, String realm) {
        return MemberOperation
                .newBuilder()
                .setAddAlias(MemberAliasOperation
                        .newBuilder()
                        .setAliasHash(hashAlias(alias))
                        .setRealm(realm))
                .build();
    }

    /**
     * Converts alias to MemberOperationMetadata.
     *
     * @param alias alias to add
     * @param realm realm
     * @return member operation metadata
     */
    public static MemberOperationMetadata toAddAliasOperationMetadata(Alias alias, String realm) {
        return MemberOperationMetadata.newBuilder()
                .setAddAliasMetadata(AddAliasMetadata.newBuilder()
                        .setAlias(alias)
                        .setAliasHash(hashAlias(alias))
                        .setRealm(realm))
                .build();
    }

    /**
     * Hashes an alias to a String.
     *
     * @param alias alias to hash
     * @return hashed alias
     */
    public static String hashAlias(Alias alias) {
        if (alias.getType() == USERNAME) {
            return alias.getValue();
        }
        return hashAndSerializeJson(alias);
    }

    /**
     * Converts {@code future} to {@link Observable}.
     *
     * @param future future to convert
     * @param <T> future result type
     * @return Observable
     */
    public static <T> Observable<T> toObservable(final ListenableFuture<T> future) {
        return Single
                .create(new SingleOnSubscribe<T>() {
                    @Override
                    public void subscribe(final SingleEmitter<T> emitter) throws Exception {
                        future.addListener(
                                new Runnable() {
                                    public void run() {
                                        if (emitter.isDisposed()) {
                                            return;
                                        }
                                        try {
                                            emitter.onSuccess(Uninterruptibles
                                                    .getUninterruptibly(future));
                                        } catch (ExecutionException ex) {
                                            // We are dealing with StatusRuntimeExceptions here,
                                            // possibly wrapping the actual custom Token exceptions.
                                            Throwable cause = ex.getCause().getCause();
                                            if (cause != null) {
                                                emitter.onError(cause);
                                            } else {
                                                emitter.onError(ex.getCause());
                                            }
                                        } catch (Throwable ex) {
                                            emitter.onError(ex);
                                        }
                                    }
                                },
                                MoreExecutors.newDirectExecutorService()
                        );
                    }
                })
                .toObservable();
    }

    /**
     * Retrieve the access token from the URL fragment, given the full URL.
     *
     * @param fullUrl full url
     * @return oauth access token, or null if not found
     */
    public static @Nullable String parseOauthAccessToken(String fullUrl) {
        String[] urlParts = fullUrl.split("#|&");
        for (int i = urlParts.length - 1; i >= 0; i--) {
            if (urlParts[i].contains("access_token=")) {
                return urlParts[i].substring(13);
            }
        }
        return null;
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
     * Hashes a string.
     *
     * @param value value to hash
     * @return hash
     */
    public static String hashString(String value) {
        return Hashing
                .sha256()
                .hashString(value, Charset.forName("ASCII"))
                .toString();
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
        Key key = getSigningKey(member, signature);

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
    public static Key getSigningKey(Member member, Signature signature) {
        Key key = null;
        String keyId = signature.getKeyId();

        for (Key k : member.getKeysList()) {
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
     * Converts async account list observable to account list observable.
     *
     * @param asyncAccounts async accounts
     * @return accounts
     */
    public static Observable<List<Account>> toAccountList(
            Observable<List<AccountAsync>> asyncAccounts) {
        return asyncAccounts.map(new Function<List<AccountAsync>, List<Account>>() {
            public List<Account> apply(List<AccountAsync> asyncList) {
                List<Account> accounts = new LinkedList<>();
                for (AccountAsync async : asyncList) {
                    accounts.add(async.sync());
                }
                return accounts;
            }
        });
    }
}
