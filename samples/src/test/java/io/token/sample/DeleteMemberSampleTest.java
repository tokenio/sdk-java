package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.grpc.StatusRuntimeException;
import io.token.Member;
import io.token.TokenIO;

import org.junit.Test;

public class DeleteMemberSampleTest {
    @Test
    public void createPaymentTokenTest() {
        try (TokenIO tokenIO = createClient()) {
            Member member = createMemberAndLinkAccounts(tokenIO);

            assertThat(tokenIO.getMember(member.memberId()).memberId())
                    .isEqualTo(member.memberId());

            member.deleteMember();

            assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() ->
                    tokenIO.getMember(member.memberId()));
        }
    }
}
