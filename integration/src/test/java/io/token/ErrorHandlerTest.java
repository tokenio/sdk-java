package io.token;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static io.token.common.TokenRule.getEnvProperty;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;

import com.typesafe.config.ConfigFactory;
import io.grpc.Metadata;
import io.token.common.EnvConfig;
import io.token.exceptions.VersionMismatchException;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.rpc.client.RpcChannelFactory;
import io.token.security.CryptoEngineFactory;
import io.token.security.TokenCryptoEngineFactory;

import org.junit.Test;

public class ErrorHandlerTest {
    @Test(expected = VersionMismatchException.class)
    public void testVersionMismatch() {
        EnvConfig config = new EnvConfig(ConfigFactory.load(getEnvProperty(
                "TOKEN_ENV",
                "local")));

        CryptoEngineFactory cryptoFactory = new TokenCryptoEngineFactory(null);
        Metadata versionHeaders = new Metadata();
        versionHeaders.put(
                Metadata.Key.of("token-sdk", ASCII_STRING_MARSHALLER),
                "java");
        versionHeaders.put(
                Metadata.Key.of("token-sdk-version", ASCII_STRING_MARSHALLER),
                "0.0.0");
        try (TokenIO tokenIO = new TokenIOAsync(
                RpcChannelFactory
                        .builder(
                                config.getGateway().getHost(),
                                config.getGateway().getPort(),
                                config.useSsl())
                        .withMetadata(versionHeaders)
                        .build(),
                cryptoFactory)
                .sync()) {
            tokenIO.aliasExists(Alias.newBuilder()
                    .setValue("")
                    .setType(EMAIL)
                    .build());
        }
    }
}
