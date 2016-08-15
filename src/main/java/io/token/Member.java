package io.token;

import io.token.security.KeyPair;

import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;

public final class Member {
    private final String memberName;
    private final String memberId;
    private final KeyPair keyPair;
    private final String keyId;

    Member(String memberName, String memberId, KeyPair keyPair, String keyId) {
        this.memberName = memberName;
        this.memberId = memberId;
        this.keyPair = keyPair;
        this.keyId = keyId;
    }

    public String getMemberName() {
        return memberName;
    }

    public String getMemberId() {
        return memberId;
    }

    @Override
    public String toString() {
        return reflectionToString(this);
    }
}
