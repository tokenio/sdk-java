package io.token.sample;

import io.token.user.Account;
import io.token.user.Member;

/**
 * Links a Token member and a bank.
 */
public final class LinkMemberAndBankSample {
    /**
     * Links a Token member and a bank.
     *
     * <p>Bank linking is currently only supported by the Token PSD2 mobile app.
     * This sample shows how to link a test member with a test bank account.
     * Real bank linking works similarly, but the BankAuthorization comes from
     * user interaction with a bank's website.
     *
     * @param member Token member to link to a bank
     * @return linked token accounts
     */
    public static Account linkBankAccounts(Member member) {
        return member.createTestBankAccountBlocking(1000.0, "EUR");
    }
}
