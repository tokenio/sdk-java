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

package io.token.util;

import static io.token.proto.AliasHasher.normalize;
import static io.token.proto.AliasHasher.normalizeAndHash;

import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.token.TokenClient.TokenCluster;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.member.MemberProtos.MemberAddKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberAliasOperation;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata.AddAliasMetadata;
import io.token.proto.common.member.MemberProtos.MemberRecoveryRulesOperation;
import io.token.proto.common.member.MemberProtos.RecoveryRule;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.util.codec.ByteEncoding;

import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.ExecutionException;


/**
 * Utility methods.
 */
public class Util {

    /**
     * The token realm.
     */
    public static final String TOKEN_REALM = "token";

    private static final int NONCE_NUM_BYTES = 12;

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
     * Converts agent id to AddKey operation.
     *
     * @param agentId agentId to add
     * @return member operation
     */
    public static MemberOperation toRecoveryAgentOperation(String agentId) {
        return MemberOperation
                .newBuilder()
                .setRecoveryRules(MemberRecoveryRulesOperation
                        .newBuilder()
                        .setRecoveryRule(RecoveryRule.newBuilder().setPrimaryAgent(agentId)))
                .build();
    }

    /**
     * Converts alias to AddAlias operation.
     *
     * @param alias alias to add
     * @return member operation
     */
    public static MemberOperation toAddAliasOperation(Alias alias) {
        return MemberOperation
                .newBuilder()
                .setAddAlias(MemberAliasOperation
                        .newBuilder()
                        .setAliasHash(normalizeAndHash(alias))
                        .setRealm(alias.getRealm())
                        .setRealmId(alias.getRealmId()))
                .build();
    }

    /**
     * Converts alias to MemberOperationMetadata.
     *
     * @param alias alias to add
     * @return member operation metadata
     */
    public static MemberOperationMetadata toAddAliasOperationMetadata(Alias alias) {
        Alias normalized = normalize(alias);
        return MemberOperationMetadata.newBuilder()
                .setAddAliasMetadata(AddAliasMetadata.newBuilder()
                        .setAlias(normalized)
                        .setAliasHash(normalizeAndHash(normalized)))
                .build();
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
                    public void subscribe(final SingleEmitter<T> emitter) {
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
     * Get the cluster-dependent url to the web-app.
     *
     * @param cluster Token cluster
     * @return web-app url
     */
    public static String getWebAppUrl(TokenCluster cluster) {
        switch (cluster) {
            case PRODUCTION:
                return "web-app.token.io";
            case BETA:
                return "web-app.beta.token.io";
            case SANDBOX:
                return "web-app.sandbox.token.io";
            case STAGING:
                return "web-app.stg.token.io";
            case PERFORMANCE:
                return "web-app.perf.token.io";
            case DEVELOPMENT:
                return "web-app.dev.token.io";
            case CUSTOM:
                return Optional.ofNullable(System.getenv("TOKEN_WEBAPP_URL"))
                        .orElseThrow(() -> new RuntimeException("TOKEN_WEBAPP_URL not found"));
            default:
                throw new IllegalArgumentException("Unrecognized cluster: " + cluster);
        }
    }
}
