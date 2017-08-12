package io.token.sample;

import io.token.Account;
import io.token.Member;
import io.token.proto.banklink.Banklink;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.security.SecurityProtos.SealedMessage;

import java.util.List;

/**
 * Links a Token member and a bank.
 */
public final class LinkMemberAndBankSample {
    /**
     * Links a Token member and a bank.
     *
     * <p>The bank linking is currently only supported by the Token PSD2 IOS mobile app.
     * This sample is implemented for a very high level illustration of the bank linking concept
     * and serves as an integral building block connecting other samples. The desktop version of
     * the bank linking process is in the development. Until it's ready, please use Token PSD2 IOS
     * mobile app to link Token members and banks.
     *
     * @param member Token member to link to a bank
     * @return linked token accounts
     */
    public static List<Account> linkBankAccounts(Member member) {
        // User opens a bank web site and completes the Token linking process. The following is a
        // high level description of how that happens in the Token PSD2 IOS mobile app:
        // 1. App displays a list of banks supported by Token
        // 2. User selects a bank, the app pops up a web view and navigates to the bank linking page
        // 3. User enters bank credentials and selects accounts to link
        // 4. The bank linking flow completes and the app extracts encrypted bank authorization
        //    from the internal service redirected to by the linking flow.

        // For the purpose of this sample, we simulate
        // the entire linking flow described above
        // by generating it with a fake bank below.
        BankAuthorization encryptedBankAuthorization =
                member.createTestBankAccount(1000.0, "EUR");

        // Finish account linking flow initiated by the user.
        return member.linkAccounts(encryptedBankAuthorization);
    }
}
