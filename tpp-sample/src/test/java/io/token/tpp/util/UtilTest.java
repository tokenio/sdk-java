package io.token.tpp.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class UtilTest {
    @Test
    public void retryWithExponentialBackoff() throws Exception {
        long startTime = System.currentTimeMillis();
        assertThat(Util.retryWithExponentialBackoff(
                300,
                10,
                2,
                50,
                () -> returnTrueAfter(startTime + 200),
                b -> !b)).isTrue();
    }

    @Test
    public void retryWithExponentialBackoff_timeout() throws Exception {
        long startTime = System.currentTimeMillis();
        assertThat(Util.retryWithExponentialBackoff(
                100,
                10,
                2,
                50,
                () -> returnTrueAfter(startTime + 1000),
                b -> !b)).isFalse();
    }

    private boolean returnTrueAfter(long timeMs) {
        long curTime = System.currentTimeMillis();
        if (curTime < timeMs) {
            return false;
        }
        return true;
    }
}
