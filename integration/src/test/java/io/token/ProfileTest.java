package io.token;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.common.TokenRule;
import io.token.proto.common.member.MemberProtos.Profile;

import org.apache.commons.lang3.builder.ToStringExclude;
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
    public void updatePartial() {
        Member member = rule.member();
        Profile firstProfile = Profile.newBuilder()
                .setDisplayNameFirst("Paul")
                .setDisplayNameLast("Hewson")
                .build();
        Profile backProfile = member.setProfile(firstProfile);
        Profile outProfile = member.getProfile(member.memberId());
        assertThat(firstProfile).isEqualTo(backProfile).isEqualTo(outProfile);
        Profile secondProfile = Profile.newBuilder()
                .setDisplayNameFirst("Bono")
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
}
