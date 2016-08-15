package io.token.rx;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Uninterruptibles;
import rx.Observable;
import rx.Single;

import java.util.concurrent.ExecutionException;

public interface Converters {
    static <T> Observable<T> toObservable(ListenableFuture<T> future) {
        Single<T> single = Single.create(subscriber -> future.addListener(() -> {
            if (subscriber.isUnsubscribed()) {
                return;
            }

            try {
                T result = Uninterruptibles.getUninterruptibly(future);
                subscriber.onSuccess(result);
            } catch (ExecutionException e) {
                subscriber.onError(e.getCause());
            }
        }, MoreExecutors.newDirectExecutorService()));

        return single.toObservable();
    }
}
