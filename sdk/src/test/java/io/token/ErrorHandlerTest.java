package io.token;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static io.token.TokenRule.getEnvProperty;

import com.google.common.net.HostAndPort;
import io.grpc.Metadata;
import io.token.exceptions.VersionMismatchException;
import io.token.rpc.client.RpcChannelFactory;
import io.token.security.CryptoEngineFactory;
import io.token.security.TokenCryptoEngineFactory;

import org.junit.Test;

public class ErrorHandlerTest {
    @Test(expected = VersionMismatchException.class)
    public void testVersionMismatch() {
        HostAndPort gateway = HostAndPort
                .fromString(getEnvProperty("TOKEN_GATEWAY", "localhost"))
                .withDefaultPort(9000);

        CryptoEngineFactory cryptoFactory = new TokenCryptoEngineFactory(null);
        Metadata versionHeaders = new Metadata();
        versionHeaders.put(
                Metadata.Key.of("token-sdk", ASCII_STRING_MARSHALLER),
                "java");
        versionHeaders.put(
                Metadata.Key.of("token-sdk-version", ASCII_STRING_MARSHALLER),
                "0.0.0");
        new TokenIOAsync(
                RpcChannelFactory
                        .builder(gateway.getHost(), gateway.getPort())
                        .withMetadata(versionHeaders)
                        .build(),
                cryptoFactory)
                .sync()
                .usernameExists("");
    }
}
