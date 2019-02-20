package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;

import io.token.proto.common.money.MoneyProtos.Money;
import io.token.tpp.Account;
import io.token.tpp.Member;
import io.token.tpp.Representable;

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
     * @return balance of one of grantor's acounts
     */
    public static Money redeemAccessToken(Member grantee, String tokenId) {
        // Specifies whether the request originated from a customer
        boolean customerInitiated = true;

        // Access grantor's account list by applying
        // access token to the grantee client.
        // forAccessToken snippet begin
        Representable grantor = grantee.forAccessToken(tokenId, customerInitiated);
        List<Account> accounts = grantor.getAccountsBlocking();

        // Get the data we want
        Money balance0 = accounts.get(0).getBalanceBlocking(STANDARD).getCurrent();
        // forAccessToken snippet end
        return balance0;
    }
}