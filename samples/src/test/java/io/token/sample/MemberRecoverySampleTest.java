package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static io.token.sample.TestUtil.waitUntil;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;

import org.junit.Test;

/**
 * Tests for member-recovery sample code.
 */
public class MemberRecoverySampleTest {
    @Test
    public void recoveryDefault() { // "normal consumer" recovery using "shortcuts"
        try (TokenIO tokenIO = createClient()) {
            MemberRecoverySample mrs = new MemberRecoverySample();

            // set up
            Alias originalAlias = randomAlias();
            Member originalMember = tokenIO.createMember(originalAlias);
            // wait until alias is processed by the asynchronous verification job (this is needed
            // only for +noverify aliases)
            waitUntil(() -> assertThat(originalMember.aliases()).contains(originalAlias));
            mrs.setUpDefaultRecoveryRule(originalMember);

            TokenIO otherTokenIO = createClient();
            Member recoveredMember = mrs.recoverWithDefaultRule(
                    otherTokenIO,
                    originalAlias);
            Alias recoveredAlias = recoveredMember.firstAlias();
            assertThat(recoveredAlias).isEqualTo(originalAlias);
        }
    }

    @Test
    public void recoveryComplex() {
        try (TokenIO tokenIO = createClient()) {
            MemberRecoverySample mrs = new MemberRecoverySample();

            TokenIO agentTokenIO = createClient();
            Alias agentAlias = randomAlias();
            Member agentMember = agentTokenIO.createMember(agentAlias);
            // wait until alias is processed by the asynchronous verification job (this is needed
            // only for +noverify aliases)
            waitUntil(() -> assertThat(agentMember.aliases()).contains(agentAlias));

            mrs.agentMember = agentMember;

            // set up
            Alias originalAlias = randomAlias();
            Member originalMember = tokenIO.createMember(originalAlias);
            // wait until alias is processed by the asynchronous verification job (this is needed
            // only for +noverify aliases)
            waitUntil(() -> assertThat(originalMember.aliases()).contains(originalAlias));
            mrs.setUpComplexRecoveryRule(originalMember, tokenIO, agentAlias);

            // recover
            TokenIO otherTokenIO = createClient();
            Member recoveredMember = mrs.recoverWithComplexRule(
                    otherTokenIO,
                    originalAlias);
            // make sure it worked
            Alias recoveredAlias = recoveredMember.firstAlias();
            assertThat(recoveredAlias).isEqualTo(originalAlias);
        }
    }
}
