package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.grpc.StatusRuntimeException;
import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class DeleteMemberSampleTest {
    @Test
    public void createPaymentTokenTest() {
        try (TokenClient tokenClient = createClient()) {
            Member member = createMemberAndLinkAccounts(tokenClient);

            assertThat(tokenClient.getMemberBlocking(member.memberId()).memberId())
                    .isEqualTo(member.memberId());

            DeleteMemberSample.deleteMember(member);

            assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() ->
                    tokenClient.getMemberBlocking(member.memberId()));
        }
    }
}
