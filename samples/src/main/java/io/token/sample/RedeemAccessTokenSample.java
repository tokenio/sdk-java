package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;

import io.token.Account;
import io.token.Member;
import io.token.proto.common.money.MoneyProtos.Money;

import java.util.List;

/**
 * Redeems an information access token.
 */
public final class RedeemAccessTokenSample {
    /**
     * Redeems access token to acquire access to the grantor's account balances.
     *
     * @param grantee grantee Token member
     * @param tokenId ID of the access token to redeem
     * @return list of grantor's accounts accessed by the grantee
     */
    public static Money redeemAccessToken(Member grantee, String tokenId) {
        // Access grantor's account list by applying
        // access token to the grantee client.
        grantee.useAccessToken(tokenId);
        List<Account> grantorAccounts = grantee.getAccounts();

        // Get the data we want
        Money balance0 = grantorAccounts.get(0).getBalance(STANDARD).getCurrent();

        // When done using access, clear token from grantee client.
        grantee.clearAccessToken();
        return balance0;
    }
}
