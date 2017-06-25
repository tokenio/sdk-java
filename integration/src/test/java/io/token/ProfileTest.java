package io.token;

import static io.grpc.Status.Code.UNKNOWN;
import static io.token.proto.common.member.MemberProtos.ProfilePictureSize.ORIGINAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import io.token.common.TokenRule;
import io.token.proto.common.blob.BlobProtos.Blob;
import io.token.proto.common.member.MemberProtos.Profile;

import java.util.Base64;

import org.junit.Rule;
import org.junit.Test;

public class ProfileTest {
    @Rule
    public TokenRule rule = new TokenRule();

    @Test
    public void setProfile() {
        Member member = rule.member();
        Profile inProfile = Profile.newBuilder()
                .setDisplayNameFirst("Tomás")
                .setDisplayNameLast("de Aquino")
                .build();
        Profile backProfile = member.setProfile(inProfile);
        Profile outProfile = member.getProfile(member.memberId());
        assertThat(inProfile).isEqualTo(backProfile).isEqualTo(outProfile);
    }

    @Test
    public void updateProfile() {
        Member member = rule.member();
        Profile firstProfile = Profile.newBuilder()
                .setDisplayNameFirst("Katy")
                .setDisplayNameLast("Hudson")
                .build();
        Profile backProfile = member.setProfile(firstProfile);
        Profile outProfile = member.getProfile(member.memberId());
        assertThat(firstProfile).isEqualTo(backProfile).isEqualTo(outProfile);
        Profile secondProfile = Profile.newBuilder()
                .setDisplayNameFirst("Katy")
                .setDisplayNameLast("Perry")
                .build();
        backProfile = member.setProfile(secondProfile);
        outProfile = member.getProfile(member.memberId());
        assertThat(secondProfile).isEqualTo(backProfile).isEqualTo(outProfile);
    }

    @Test
    public void updateToMononym() {
        Member member = rule.member();
        Profile firstProfile = Profile.newBuilder()
                .setDisplayNameFirst("Paul")
                .setDisplayNameLast("Hewson")
                .build();
        Profile backProfile = member.setProfile(firstProfile);
        Profile outProfile = member.getProfile(member.memberId());
        assertThat(firstProfile).isEqualTo(backProfile).isEqualTo(outProfile);
        Profile secondProfile = Profile.newBuilder()
                .setDisplayNameFirst("Bono") // we expect replace, not merge, not "Bono Hewson"
                .build();
        backProfile = member.setProfile(secondProfile);
        outProfile = member.getProfile(member.memberId());
        assertThat(secondProfile).isEqualTo(backProfile).isEqualTo(outProfile);
    }

    @Test
    public void readProfile_notYours() {
        Member member = rule.member();
        Profile inProfile = Profile.newBuilder()
                .setDisplayNameFirst("Tomás")
                .setDisplayNameFirst("de Aquino")
                .build();
        member.setProfile(inProfile);

        Member otherMember = rule.member();
        Profile outProfile = otherMember.getProfile(member.memberId());
        assertThat(inProfile).isEqualTo(outProfile);
    }

    @Test
    public void setProfilePicture() {
        // "The tiniest gif ever" , a 1x1 gif
        // http://probablyprogramming.com/2009/03/15/the-tiniest-gif-ever
        byte[] tinyGif = Base64.getDecoder()
                .decode("R0lGODlhAQABAIABAP///wAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==");

        Member member = rule.member();
        member.setProfilePicture("image/gif", tinyGif);

        Member otherMember = rule.member();
        Blob blob = otherMember.getProfilePicture(member.memberId(), ORIGINAL);

        ByteString tinyGifString = ByteString.copyFrom(tinyGif);
        assertThat(blob.getPayload().getData()).isEqualTo(tinyGifString);
    }

    @Test
    public void getNoProfilePicture() {
        Member member = rule.member();
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> member.getProfilePicture(member.memberId(), ORIGINAL))
                .matches(e -> e.getStatus().getCode() == UNKNOWN);
    }
}
