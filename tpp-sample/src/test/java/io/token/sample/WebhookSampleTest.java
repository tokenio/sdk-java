package io.token.sample;

import static io.token.sample.TestUtil.createClient;
import static io.token.sample.TestUtil.randomAlias;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import io.grpc.StatusRuntimeException;
import io.token.proto.common.webhook.WebhookProtos.Webhook.Config;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;
import org.junit.Ignore;
import org.junit.Test;

public class WebhookSampleTest {
    @Test
    @Ignore
    public void webhook() {
        try (TokenClient tokenClient = createClient()) {
            Member tpp = tokenClient.createMemberBlocking(randomAlias());

            WebhookSample.setWebhookConfig(tpp);
            Config config = WebhookSample.getWebhookConfig(tpp);
            assertThat(config.getUrl()).isNotEmpty();
            assertThat(config.getTypeList().size()).isNotZero();

            WebhookSample.deleteWebhookConfig(tpp);
            assertThatExceptionOfType(StatusRuntimeException.class)
                    .isThrownBy(() -> WebhookSample.getWebhookConfig(tpp));
        }
    }
}
