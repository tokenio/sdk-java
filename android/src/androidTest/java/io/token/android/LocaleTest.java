package io.token.android;

import static io.token.TokenClient.TokenCluster.SANDBOX;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;

import android.support.test.runner.AndroidJUnit4;
import io.token.user.TokenClient;
import io.token.util.common.StringUtil;

import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LocaleTest {
    private static final Locale ARABIC = Locale.forLanguageTag("ar-EG");

    @Test
    public void format() {
        Locale.setDefault(ARABIC);
        assertEquals("api.token.io:443",
                StringUtil.format("%s:%d", "api.token.io", 443));
    }

    @Test
    public void tokenClient() {
        Locale.setDefault(ARABIC);
        assertThatCode(() -> TokenClient.builder().connectTo(SANDBOX).build())
                .doesNotThrowAnyException();
    }
}
