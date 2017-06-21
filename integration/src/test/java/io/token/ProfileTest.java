package io.token;

import static io.grpc.Status.Code.NOT_FOUND;
import static io.grpc.Status.Code.PERMISSION_DENIED;
import static io.token.testing.sample.Sample.address;
import static io.token.testing.sample.Sample.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.grpc.StatusRuntimeException;
import io.token.common.TokenRule;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.member.MemberProtos;
import io.token.proto.common.member.MemberProtos.AddressRecord;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.testing.sample.Sample;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ProfileTest {
    @Rule
    public TokenRule rule = new TokenRule();

    @Test
    public void setProfile() {
        Member member = rule.member();
        Profile inProfile = Profile.newBuilder()
                .setMemberId(member.memberId())
                .setDisplayName("Tomás de Aquino")
                .build();
        member.setProfile(inProfile);
        Profile outProfile = member.getProfile(member.memberId());
        assertThat(inProfile.getMemberId()).isEqualTo(outProfile.getMemberId());
        assertThat(inProfile.getDisplayName()).isEqualTo(outProfile.getDisplayName());
        assertThat(inProfile.getPictureBlobId()).isEqualTo(outProfile.getPictureBlobId());
    }

    @Test
    public void setProfile_notYours() {
        Member member = rule.member();
        Member otherMember = rule.member();
        Profile profile = Profile.newBuilder()
                .setMemberId(otherMember.memberId())
                .setDisplayName("Tomás de Aquino")
                .build();

        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> member.setProfile(profile))
                .matches(e -> e.getStatus().getCode() == PERMISSION_DENIED);
    }

    @Test
    public void readProfile_notYours() {
        Member member = rule.member();
        Profile inProfile = Profile.newBuilder()
                .setMemberId(member.memberId())
                .setDisplayName("Tomás de Aquino")
                .build();
        member.setProfile(inProfile);

        Member otherMember = rule.member();
        Profile outProfile = otherMember.getProfile(member.memberId());
        assertThat(inProfile.getMemberId()).isEqualTo(outProfile.getMemberId());
        assertThat(inProfile.getDisplayName()).isEqualTo(outProfile.getDisplayName());
        assertThat(inProfile.getPictureBlobId()).isEqualTo(outProfile.getPictureBlobId());
    }
}
