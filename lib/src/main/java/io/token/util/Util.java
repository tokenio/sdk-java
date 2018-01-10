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
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.token.browser.Browser;
import io.token.browser.BrowserFactory;
import io.token.proto.ProtoJson;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.member.MemberProtos.MemberAddKeyOperation;
import io.token.proto.common.member.MemberProtos.MemberAliasOperation;
import io.token.proto.common.member.MemberProtos.MemberOperation;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata;
import io.token.proto.common.member.MemberProtos.MemberOperationMetadata.AddAliasMetadata;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.TokenPayload;
import io.token.proto.common.token.TokenProtos.TransferBody;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferInstructions;

import java.net.URL;
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
     * Directs the browser through the bank authorization process, fetching and returning
     * the bank authorization payload at the end.
     *
     * @param initialUrl the first url in the bank auth process
     * @param completionPattern the final url, which provides the authorization payload
     * @param browserFactory browser factory
     * @return bank authorization payload
     */
    public static Observable<BankAuthorization> getBankAuthorization(
            final String initialUrl,
            final String completionPattern,
            final BrowserFactory browserFactory) {
        return Single.create(new SingleOnSubscribe<BankAuthorization>() {
            @Override
            public void subscribe(final SingleEmitter<BankAuthorization> emitter) throws Exception {
                final Browser browser = browserFactory.create();
                browser.url()
                        .filter(new Predicate<URL>() {
                            @Override
                            public boolean test(URL url) {
                                if (url
                                        .toExternalForm()
                                        .matches(completionPattern)) {
                                    return true;
                                }
                                browser.goTo(url);
                                return false;
                            }
                        })
                        .observeOn(Schedulers.newThread())
                        .flatMap(new Function<URL, ObservableSource<String>>() {
                            @Override
                            public ObservableSource<String> apply(URL url) {
                                return browser.fetchData(url);
                            }
                        })
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String json) {
                                        BankAuthorization bankAuthorization = ProtoJson
                                                .fromJson(
                                                        json,
                                                        BankAuthorization.newBuilder());
                                        emitter.onSuccess(bankAuthorization);
                                        browser.close();
                                    }
                                },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable ex) {
                                        emitter.onError(ex);
                                        browser.close();
                                    }
                                });
                browser.goTo(new URL(initialUrl));
            }
        }).toObservable();
    }

    /**
     * Add a bank authorization to an unauthorized token payload.
     *
     * @param payload token payload without bank authorization
     * @param bankAuthorization bank authorization
     * @return token payload with bank authorization
     */
    public static TokenPayload applyBankAuthorization(
            TokenPayload payload,
            BankAuthorization bankAuthorization) {
        BankAccount account = BankAccount.newBuilder()
                .setTokenAuthorization(BankAccount.TokenAuthorization.newBuilder()
                        .setAuthorization(bankAuthorization)
                        .build())
                .build();

        TransferEndpoint endpoint = payload.toBuilder()
                .getTransferBuilder()
                .getInstructionsBuilder()
                .getSourceBuilder()
                .setAccount(account)
                .build();

        TransferInstructions instructions = payload.toBuilder()
                .getTransferBuilder()
                .getInstructionsBuilder()
                .setSource(endpoint)
                .build();

        TransferBody transfer = payload.toBuilder()
                .getTransferBuilder()
                .setInstructions(instructions)
                .build();

        return payload.toBuilder()
                .setTransfer(transfer)
                .build();
    }
}
