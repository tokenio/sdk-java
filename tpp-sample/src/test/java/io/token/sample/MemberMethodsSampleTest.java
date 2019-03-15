package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.member.MemberProtos.Profile;
import io.token.security.CryptoEngine;
import io.token.security.InMemoryKeyStore;
import io.token.security.KeyStore;
import io.token.security.TokenCryptoEngine;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import org.junit.Test;

public class MemberMethodsSampleTest {
    @Test
    public void keysTest() {
        try (TokenClient tokenClient = createClient()) {
            KeyStore keyStore = new InMemoryKeyStore();
            CryptoEngine cryptoEngine = new TokenCryptoEngine("member-id", keyStore);

            Member member = tokenClient.createMemberBlocking(randomAlias());
            MemberMethodsSample.keys(cryptoEngine, member);
        }
    }

    @Test
    public void profilesTest() {
        try (TokenClient tokenClient = createClient()) {
            Member member = tokenClient.createMemberBlocking(randomAlias());
            Profile profile = MemberMethodsSample.profiles(member);

            assertThat(profile.getDisplayNameFirst()).isNotEmpty();
            assertThat(profile.getDisplayNameLast()).isNotEmpty();
        }
    }
}
