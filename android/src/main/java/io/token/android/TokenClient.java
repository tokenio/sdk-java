package io.token.android;

import io.grpc.ManagedChannel;
import io.token.security.CryptoEngineFactory;
import io.token.user.browser.BrowserFactory;

public class TokenClient extends io.token.user.TokenClient {
    /**
     * Creates an instance of a Token SDK.
     *
     * @param channel GRPC channel
     * @param cryptoFactory crypto factory instance
     * @param tokenCluster token cluster
     */
    TokenClient(
            ManagedChannel channel,
            CryptoEngineFactory cryptoFactory,
            TokenCluster tokenCluster,
            BrowserFactory browserFactory) {
        super(channel, cryptoFactory, tokenCluster, browserFactory);
    }

    /**
     * Creates a new {@link Builder} instance that is used to configure and
     * build a {@link TokenClient} instance.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends io.token.user.TokenClient.Builder {
        @Override
        protected String getPlatform() {
            return "android";
        }

        @Override
        public TokenClient build() {
            return (TokenClient) super.build();
        }
    }
}
