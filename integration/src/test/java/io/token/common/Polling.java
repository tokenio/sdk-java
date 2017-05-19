package io.token.common;

import com.google.common.util.concurrent.Uninterruptibles;

import java.util.concurrent.TimeUnit;

/**
 * Helper class for polling for asynchronous events
 */
public class Polling {
    public static void waitUntil(long timeoutMs, Runnable function) {
        waitUntil(timeoutMs, 1, 2, function);
    }

    public static void waitUntil(long timeoutMs, long waitTimeMs, Runnable function) {
        waitUntil(timeoutMs, waitTimeMs,1, function);
    }

    public static void waitUntil(
            long timeoutMs,
            long waitTimeMs,
            int backOffFactor,
            Runnable function) {
        for (long start = System.currentTimeMillis(); ; waitTimeMs *= backOffFactor) {
            try {
                function.run();
                return;
            } catch (AssertionError caughtError) {
                if (System.currentTimeMillis() - start < timeoutMs) {
                    Uninterruptibles.sleepUninterruptibly(waitTimeMs, TimeUnit.MILLISECONDS);
                } else {
                    throw caughtError;
                }
            }
        }
    }
}
