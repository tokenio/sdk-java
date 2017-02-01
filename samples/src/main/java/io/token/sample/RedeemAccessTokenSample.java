package io.token.sample;

import io.token.Account;
import io.token.Member;

import java.util.List;

/**
 * Redeems an information access token.
 */
public final class RedeemAccessTokenSample {
    /**
     * Redeems access token to acquire access to the grantor's account names.
     *
     * @param grantee grantee Token member
     * @param tokenId ID of the access token to redeem
     * @return list of grantor's accounts accessed by the grantee
     */
    public static List<Account> redeemToken(Member grantee, String tokenId) {
        // Access grantor's account list.
        grantee.useAccessToken(tokenId);
        return grantee.getAccounts();
    }
}
