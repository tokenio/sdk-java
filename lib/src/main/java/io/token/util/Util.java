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
import static io.token.proto.common.alias.AliasProtos.Alias.Type.USERNAME;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.member.MemberProtos.MemberAddKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberAliasOperation;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata.AddAliasMetadata;
import io.token.proto.common.security.SecurityProtos.Key;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;


/**
 * Utility methods.
 */
public abstract class Util {
    private Util() {
    }

    /**
     * Generates a random string.
     *
     * @return generated random string
     */
    public static String generateNonce() {
        return randomAlphabetic(20);
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
     * @return member operation
     */
    public static MemberOperation toAddAliasOperation(Alias alias) {
        return MemberOperation
                .newBuilder()
                .setAddAlias(MemberAliasOperation
                        .newBuilder()
                        .setAliasHash(hashAlias(alias)))
                .build();
    }

    /**
     * Converts alias to MemberOperationMetadata.
     *
     * @param alias alias to add
     * @return member operation metadata
     */
    public static MemberOperationMetadata toAddAliasOperationMetadata(Alias alias) {
        return MemberOperationMetadata.newBuilder()
                .setAddAliasMetadata(AddAliasMetadata.newBuilder()
                        .setAlias(alias)
                        .setAliasHash(hashAlias(alias)))
                .build();
    }

    /**
     * Converts alias to MemberOperationMetadata.
     *
     * @param alias alias to add
     * @return MemberOperation metadata
     */
    public static MemberOperationMetadata toMemberOperationMetadata(Alias alias) {
        return MemberOperationMetadata
                .newBuilder()
                .setAddAliasMetadata(AddAliasMetadata
                        .newBuilder()
                        .setAliasHash(hashAlias(alias))
                        .setAlias(alias))
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
     * Loads the body of the resource associated with the specified url.
     *
     * @param url the url to fetch
     * @return the body of the resource
     * @throws IOException if there was a problem loading the url
     */
    public static Observable<String> fetchUrl(final URL url) throws IOException {
        return Observable.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                InputStream is = url.openConnection().getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder builder = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                return builder.toString();
            }
        });
    }
}
