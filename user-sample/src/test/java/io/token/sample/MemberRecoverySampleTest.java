package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

/**
 * Tests for member-recovery sample code.
 */
public class MemberRecoverySampleTest {
    @Test
    public void recoveryDefault() { // "normal consumer" recovery using "shortcuts"
        try (TokenClient tokenClient = createClient()) {
            MemberRecoverySample mrs = new MemberRecoverySample();

            // set up
            Alias originalAlias = randomAlias();
            Member originalMember = tokenClient.createMemberBlocking(originalAlias);
            mrs.setUpDefaultRecoveryRule(originalMember);

            TokenClient otherTokenClient = createClient();
            Member recoveredMember = mrs.recoverWithDefaultRule(
                    otherTokenClient,
                    originalAlias);
            Alias recoveredAlias = recoveredMember.firstAliasBlocking();
            assertThat(recoveredAlias).isEqualTo(originalAlias);
        }
    }

    @Test
    public void recoveryComplex() {
        try (TokenClient tokenClient = createClient()) {
            MemberRecoverySample mrs = new MemberRecoverySample();

            TokenClient agentTokenIO = createClient();
            Alias agentAlias = randomAlias();
            Member agentMember = agentTokenIO.createMemberBlocking(agentAlias);

            mrs.agentMember = agentMember;

            // set up
            Alias originalAlias = randomAlias();
            Member originalMember = tokenClient.createMemberBlocking(originalAlias);
            mrs.setUpComplexRecoveryRule(originalMember, tokenClient, agentAlias);

            // recover
            TokenClient otherTokenClient = createClient();
            Member recoveredMember = mrs.recoverWithComplexRule(
                    otherTokenClient,
                    originalAlias);
            // make sure it worked
            Alias recoveredAlias = recoveredMember.firstAliasBlocking();
            assertThat(recoveredAlias).isEqualTo(originalAlias);
        }
    }
}
