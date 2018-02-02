package io.token.sample;

import static io.grpc.Status.Code.FAILED_PRECONDITION;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;

import io.grpc.StatusRuntimeException;
import io.token.Account;
import io.token.Member;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource;
import io.token.proto.common.token.TokenProtos.Token;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Redeems an information access token. Assumes token has "allAccounts" access when using it.
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
        // Access grantor's account list by applying
        // access token to the grantee client.
        grantee.useAccessToken(tokenId);
        List<Account> grantorAccounts = grantee.getAccounts();

        // Get the data we want
        Money balance0 = grantorAccounts.get(0).getCurrentBalance();
        // When done using access, clear token from grantee client.
        grantee.clearAccessToken();
        return balance0;
    }

    /**
     * Redeems an information access token. Does not assume token has "allAccounts" access.
     *
     * @param grantee grantee Token member
     * @param tokenId ID of the access token to redeem
     * @return balance of one of grantor's accounts (or empty Money proto if no account found)
     */
    public static Money carefullyUseAccessToken(Member grantee, String tokenId) {
        Token accessToken = grantee.getToken(tokenId);
        while (!accessToken.getReplacedByTokenId().isEmpty()) {
            accessToken = grantee.getToken(accessToken.getReplacedByTokenId());
        }
        Set<String> accountIds = new HashSet<>();
        List<Resource> resources = accessToken.getPayload().getAccess().getResourcesList();
        boolean haveAllBalancesAccess = false;
        boolean haveAllAccountsAccess = false;
        for (int i = 0; i < resources.size(); i++) {
            Resource resource = resources.get(i);
            switch (resource.getResourceCase()) {
                case ALL_BALANCES:
                    haveAllBalancesAccess = true;
                    break;
                case ALL_ACCOUNTS:
                    haveAllAccountsAccess = true;
                    break;
                case BALANCE:
                    accountIds.add(resource.getBalance().getAccountId());
                    break;
                default:
                    break;
            }
        }
        grantee.useAccessToken(accessToken.getId());
        if (haveAllAccountsAccess && haveAllBalancesAccess) {
            List<Account> grantorAccounts = grantee.getAccounts();
            for (int i = 0; i < grantorAccounts.size(); i++) {
                accountIds.add(grantorAccounts.get(i).id());
            }
        }
        // We don't have access to any accounts, so return empty balance.
        if (accountIds.size() < 1) {
            return Money.getDefaultInstance();
        }
        for (String accountId : accountIds) {
            try {
                Money balance = grantee.getAvailableBalance(accountId);
                grantee.clearAccessToken(); // stop using access token
                return balance;
            } catch (StatusRuntimeException ex) {
                // If grantor previously un-linked an account, then grantee can't get its balance.
                if (ex.getStatus().getCode() == FAILED_PRECONDITION) {
                    continue;
                }
                throw ex;
            }
        }
        grantee.clearAccessToken(); // stop using access token
        // We don't have access to any accounts, so return empty balance.
        return Money.getDefaultInstance();
    }
}
