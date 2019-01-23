package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.grpc.StatusRuntimeException;
import io.token.bank.Member;
import io.token.bank.TokenClient;

import org.junit.Test;

public class DeleteMemberSampleTest {
    @Test
    public void createPaymentTokenTest() {
        try (TokenClient tokenClient = createClient()) {
            Member member = createMemberAndLinkAccounts(tokenClient);

            assertThat(tokenClient.getMemberBlocking(member.memberId()).memberId())
                    .isEqualTo(member.memberId());

            member.deleteMemberBlocking();

            assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() ->
                    tokenClient.getMemberBlocking(member.memberId()));
        }
    }
}
