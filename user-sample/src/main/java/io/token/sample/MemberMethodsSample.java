package io.token.sample;

import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.Util.generateNonce;

import com.google.common.io.BaseEncoding;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.security.CryptoEngine;
import io.token.user.Member;
import io.token.user.TokenClient;

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
     * Adds, removes, and resolves aliases.
     *
     * @param tokenClient token client
     * @param member member
     * @return resolved member ID and alias
     */
    public static TokenMember aliases(TokenClient tokenClient, Member member) {
        Alias alias1 = member.firstAliasBlocking();
        Alias alias2 = Alias.newBuilder()
                .setType(EMAIL)
                .setValue("alias2-" + generateNonce() + "+noverify@token.io")
                .build();
        member.addAliasBlocking(alias2);

        Alias alias3 = Alias.newBuilder()
                .setType(EMAIL)
                .setValue("alias3-" + generateNonce() + "+noverify@token.io")
                .build();
        Alias alias4 = Alias.newBuilder()
                .setType(EMAIL)
                .setValue("alias4-" + generateNonce() + "+noverify@token.io")
                .build();
        member.addAliasesBlocking(Arrays.asList(alias3, alias4));

        member.removeAliasBlocking(alias1);
        member.removeAliasesBlocking(Arrays.asList(alias2, alias3));

        TokenMember resolved = tokenClient.resolveAliasBlocking(alias4);
        return resolved;
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
