package io.token;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.common.TokenRule;
import io.token.proto.common.member.MemberProtos.ProfileText;

import org.junit.Rule;
import org.junit.Test;

public class ProfileTest {
    @Rule
    public TokenRule rule = new TokenRule();

    @Test
    public void setProfileText() {
        Member member = rule.member();
        ProfileText inProfileText = ProfileText.newBuilder()
                .setDisplayNameFirst("Tomás")
                .setDisplayNameLast("de Aquino")
                .build();
        ProfileText backProfileText = member.setProfileText(inProfileText);
        ProfileText outProfileText = member.getProfileText(member.memberId());
        assertThat(inProfileText).isEqualTo(backProfileText).isEqualTo(outProfileText);
    }

    public void updateProfileText() {
        Member member = rule.member();
        ProfileText firstProfileText = ProfileText.newBuilder()
                .setDisplayNameFirst("Katy")
                .setDisplayNameLast("Hudson")
                .build();
        ProfileText backProfileText = member.setProfileText(firstProfileText);
        ProfileText outProfileText = member.getProfileText(member.memberId());
        assertThat(firstProfileText).isEqualTo(backProfileText).isEqualTo(outProfileText);
        ProfileText secondProfileText = ProfileText.newBuilder()
                .setDisplayNameFirst("Katy")
                .setDisplayNameLast("Perry")
                .build();
        backProfileText = member.setProfileText(secondProfileText);
        outProfileText = member.getProfileText(member.memberId());
        assertThat(secondProfileText).isEqualTo(backProfileText).isEqualTo(outProfileText);
    }

    @Test
    public void readProfileText_notYours() {
        Member member = rule.member();
        ProfileText inProfileText = ProfileText.newBuilder()
                .setDisplayNameFirst("Tomás")
                .setDisplayNameFirst("de Aquino")
                .build();
        member.setProfileText(inProfileText);

        Member otherMember = rule.member();
        ProfileText outProfileText = otherMember.getProfileText(member.memberId());
        assertThat(inProfileText).isEqualTo(outProfileText);
    }
}
