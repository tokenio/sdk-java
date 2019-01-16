package io.token.sample;

import static io.token.TokenClient.TokenCluster.SANDBOX;
import static io.token.proto.common.member.MemberProtos.CreateMemberType.PERSONAL;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.security.UnsecuredFileSystemKeyStore;
import io.token.user.Member;
import io.token.user.TokenClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class CreateMemberSample {
    /**
     * Creates and returns a new token member.
     *
     * @return a new Member instance
     */
    public static Member createMember() {
        // Create the client, which communicates with
        // the Token cloud.
        try {
            Path keys = Files.createDirectories(Paths.get("./keys"));
            TokenClient tokenClient = TokenClient.builder()
                    .withKeyStore(new UnsecuredFileSystemKeyStore(keys.toFile()))
                    .connectTo(SANDBOX)
                    .build();

            // An alias is a "human-readable" reference to a member.
            // Here, we use a random email. This works in test environments.
            // but in production, TokenOS would try to verify we own the address,
            // so a random address wouldn't be useful for much.
            // We use a random address because otherwise, if we ran a second
            // time, Token would say the alias was already taken.
            Alias alias = Alias.newBuilder()
                    .setType(Alias.Type.EMAIL)
                    .setValue(randomAlphabetic(10).toLowerCase()
                            + "+noverify@example.com")
                    .build();

            Member newMember = tokenClient.createMemberBlocking(alias);
            // let user recover member by verifying email if they lose keys
            newMember.useDefaultRecoveryRule();

            return newMember;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
