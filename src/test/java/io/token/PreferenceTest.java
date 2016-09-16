package io.token;

import org.junit.Rule;
import org.junit.Test;

import static io.token.testing.sample.Sample.string;
import static org.assertj.core.api.Assertions.assertThat;

public class PreferenceTest {
    @Rule
    public TokenRule rule = new TokenRule();
    private Member member = rule.member();

    @Test
    public void setAndGetPreference() {
        String preference = string();
        member.setPreferences(preference);
        String result = member.getPreferences();
        assertThat(result).isEqualTo(preference);
    }
}
