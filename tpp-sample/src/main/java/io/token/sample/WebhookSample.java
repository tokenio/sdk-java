package io.token.sample;

import io.token.proto.common.webhook.WebhookProtos.EventType;
import io.token.proto.common.webhook.WebhookProtos.Webhook.Config;
import io.token.tpp.Member;

/**
 * Manages the webhook config.
 */
public class WebhookSample {
    /**
     * Sets a webhook config.
     *
     * @param tpp the TPP member.
     */
    public static void setWebhookConfig(Member tpp) {
        // Create the webhook config
        Config config = Config.newBuilder()
                .setUrl("http://your.webhook.url")
                .addType(EventType.TRANSFER_STATUS_CHANGED)
                .build();

        tpp.setWebhookConfigBlocking(config);
    }

    /**
     * Gets the webhook config.
     *
     * @param tpp the TPP member.
     * @return config
     */
    public static Config getWebhookConfig(Member tpp) {
        return tpp.getWebhookConfigBlocking();
    }

    /**
     * Deletes the webhook config.
     *
     * @param tpp the TPP member.
     */
    public static void deleteWebhookConfig(Member tpp) {
        tpp.deleteWebhookConfigBlocking();
    }
}
