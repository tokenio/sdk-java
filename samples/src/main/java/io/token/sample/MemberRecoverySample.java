package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.PRIVILEGED;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation;
import io.token.proto.common.member.MemberProtos.MemberRecoveryOperation.Authorization;
import io.token.proto.common.member.MemberProtos.RecoveryRule;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.security.CryptoEngine;
import io.token.security.InMemoryKeyStore;
import io.token.security.TokenCryptoEngine;

import java.util.Arrays;

public class MemberRecoverySample {
    public Member agentMember; /* used by complex recovery rule sample */

    public void setUpDefaultRecoveryRule(Member member) {
        member.useDefaultRecoveryRule();
    }

    /**
     * Recover previously-created member, assuming they were
     * configured with a "normal consumer" recovery rule.
     *
     * @param tokenIO SDK client
     * @param alias alias of member to recoverWithDefaultRule
     * @return recovered member
     */
    public Member recoverWithDefaultRule(TokenIO tokenIO, Alias alias) {
        String verificationId = tokenIO.beginRecovery(alias);

        String memberId = tokenIO.getMemberId(alias);

        // In the real world, we'd prompt the user to enter the code emailed to them.
        // Since our test member uses an auto-verify email address, any string will work,
        // so we use "1thru6".
        Member recoveredMember = tokenIO.completeRecoveryWithDefaultRule(
                memberId,
                verificationId,
                "1thru6");
        // We can use the same verification code to re-claim this alias.
        recoveredMember.verifyAlias(verificationId, "1thru6");

        return recoveredMember;
    }

    private void tellRecoveryAgentMemberId(String memberId) {} /* this simple sample uses a no op */

    /**
     * Illustrate setting up a recovery rule more complex than "normal consumer"
     * mode, without the "normal consumer" shortcuts.
     * @param newMember newly-created member we are setting up
     * @param tokenIO SDK client
     * @param agentAlias Alias of recovery agent.
     */
    public void setUpComplexRecoveryRule(
            Member newMember,
            TokenIO tokenIO,
            Alias agentAlias) {
        // Someday in the future, this user might ask the recovery agent
        // "Please tell Token that I am the member with ID m:12345678 ."
        // While we're setting up this new member, we need to tell the
        // recovery agent the new member ID so the agent can "remember" later.
        tellRecoveryAgentMemberId(newMember.memberId());

        String agentId = tokenIO.getMemberId(agentAlias);
        RecoveryRule recoveryRule = RecoveryRule.newBuilder()
                .setPrimaryAgent(agentId)
                .build();
        newMember.addRecoveryRule(recoveryRule);
    }

    /* this simple sample approves everybody */
    private boolean checkMemberId(String memberId) {
        return true;
    }

    /**
     * Illustrate how a recovery agent signs an authorization.
     * @param authorization client's claim to be some member
     * @return if authorization seems legitimate, return signature; else error
     */
    public Signature getRecoveryAgentSignature(Authorization authorization) {
        // "Remember" whether this person who claims to be member with
        // the ID m:12345678 really is:
        boolean isCorrect = checkMemberId(authorization.getMemberId());
        if (isCorrect) {
            return agentMember.authorizeRecovery(authorization);
        }
        throw new RuntimeException("I don't authorize this");
    }

    /**
     * Illustrate recovery using a not-normal-"consumer mode" recovery agent.
     * @param tokenIO SDK client
     * @param alias Alias of member to recover
     * @return recovered member
     */
    public Member recoverWithComplexRule(
            TokenIO tokenIO,
            Alias alias) {
        // complexRecovery begin snippet to include in docs
        String memberId = tokenIO.getMemberId(alias);

        CryptoEngine cryptoEngine = new TokenCryptoEngine(memberId, new InMemoryKeyStore());
        Key newKey = cryptoEngine.generateKey(PRIVILEGED);

        String verificationId = tokenIO.beginRecovery(alias);
        Authorization authorization = tokenIO.createRecoveryAuthorization(memberId, newKey);

        // ask recovery agent to verify that I really am this member
        Signature agentSignature = getRecoveryAgentSignature(authorization);

        // We have all the signed authorizations we need.
        // (In this example, "all" is just one.)
        MemberRecoveryOperation mro = MemberRecoveryOperation.newBuilder()
                .setAuthorization(authorization)
                .setAgentSignature(agentSignature)
                .build();
        Member recoveredMember = tokenIO.completeRecovery(
                memberId,
                Arrays.asList(mro),
                newKey,
                cryptoEngine);
        // after recovery, aliases aren't verified

        // In the real world, we'd prompt the user to enter the code emailed to them.
        // Since our test member uses an auto-verify email address, any string will work,
        // so we use "1thru6".
        recoveredMember.verifyAlias(verificationId, "1thru6");
        // complexRecovery end snippet to include in docs

        return recoveredMember;
    }
}
