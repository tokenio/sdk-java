package io.token.sample;

import io.token.tpp.Member;

/**
 * Deletes a member.
 */
public class DeleteMemberSample {
    /**
     * Deletes a member.
     *
     * @param member member
     */
    public static void deleteMember(Member member) {
        member.deleteMember();
    }
}
