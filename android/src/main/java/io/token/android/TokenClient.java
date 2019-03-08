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

    public static final class Builder extends io.token.user.TokenClient.Builder {
        @Override
        protected String getPlatform() {
            return "android";
        }
    }
}
