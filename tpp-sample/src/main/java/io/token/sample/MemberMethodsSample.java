package io.token.sample;

import static io.token.proto.common.alias.AliasProtos.Alias.Type.DOMAIN;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;

import com.google.common.io.BaseEncoding;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.security.CryptoEngine;
import io.token.tpp.Member;
import io.token.tpp.TokenClient;

import java.util.Arrays;

public class MemberMethodsSample {
    private static final byte[] PICTURE = BaseEncoding.base64().decode(
            "/9j/4AAQSkZJRgABAQEASABIAAD//gATQ3JlYXRlZCB3aXRoIEdJTVD/2wBDA"
                    + "BALDA4MChAODQ4SERATGCgaGBYWGDEjJR0oOjM9PDkzODdASFxOQERXRT"
                    + "c4UG1RV19iZ2hnPk1xeXBkeFxlZ2P/2wBDARESEhgVGC8aGi9jQjhCY2N"
                    + "jY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2Nj"
                    + "Y2NjY2P/wgARCAAIAAgDAREAAhEBAxEB/8QAFAABAAAAAAAAAAAAAAAAA"
                    + "AAABv/EABQBAQAAAAAAAAAAAAAAAAAAAAD/2gAMAwEAAhADEAAAAT5//8"
                    + "QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABBQJ//8QAFBEBAAAAAAA"
                    + "AAAAAAAAAAAAAAP/aAAgBAwEBPwF//8QAFBEBAAAAAAAAAAAAAAAAAAAA"
                    + "AP/aAAgBAgEBPwF//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQAGP"
                    + "wJ//8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPyF//9oADAMBAA"
                    + "IAAwAAABAf/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAwEBPxB//8Q"
                    + "AFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAgEBPxB//8QAFBABAAAAAAAA"
                    + "AAAAAAAAAAAAAP/aAAgBAQABPxB//9k=");

    /**
     * Adds and removes an alias.
     *
     * @param member member
     */
    public static void aliases(Member member) {
        Alias alias = Alias.newBuilder()
                .setType(DOMAIN)
                .setValue("verified-domain.com")
                .build();

        // add the alias
        member.addAliasBlocking(alias);

        // remove the alias
        member.removeAliasBlocking(alias);
    }

    /**
     * Resolves a user's alias.
     *
     * @param client token client
     */
    public static void resolveAlias(TokenClient client) {
        Alias alias = Alias.newBuilder()
                .setValue("user-email@example.com")
                .build();

        // If this call fails then the alias does not correspond to an existing member.
        TokenMember resolved = client.resolveAliasBlocking(alias);

        // resolved member ID from alias
        String memberId = resolved.getId();

        // The resolved alias
        // will have the correct type, e.g. EMAIL.
        Alias resolvedAlias = resolved.getAlias();
    }

    /**
     * Adds and removes keys.
     *
     * @param crypto crypto engine
     * @param member member
     */
    public static void keys(CryptoEngine crypto, Member member) {
        Key lowKey = crypto.generateKey(LOW);
        member.approveKeyBlocking(lowKey);

        Key standardKey = crypto.generateKey(STANDARD);
        Key privilegedKey = crypto.generateKey(PRIVILEGED);
        member.approveKeysBlocking(Arrays.asList(standardKey, privilegedKey));

        member.removeKeyBlocking(lowKey.getId());
    }

    /**
     * Sets a profile name and picture.
     *
     * @param member member
     * @return profile
     */
    public static Profile profiles(Member member) {
        Profile name = Profile.newBuilder()
                .setDisplayNameFirst("Tycho")
                .setDisplayNameLast("Nestoris")
                .build();
        member.setProfileBlocking(name);
        member.setProfilePictureBlocking("image/jpeg", PICTURE);

        Profile profile = member.getProfileBlocking(member.memberId());
        return profile;
    }
}
