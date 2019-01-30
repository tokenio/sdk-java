package io.token.sample;

import static io.grpc.Status.Code.FAILED_PRECONDITION;
import static io.token.proto.common.security.SecurityProtos.Key.Level.LOW;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;

import io.grpc.StatusRuntimeException;
import io.token.Account;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.tpp.Member;
import io.token.tpp.Representable;

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

    /**
     * Redeems an information access token. Does not assume token has "allAccounts" access.
     *
     * @param grantee grantee Token member
     * @param tokenId ID of the access token to redeem
     * @return balance of one of grantor's accounts (or empty Money proto if no account found)
     */
    public static Money carefullyUseAccessToken(Member grantee, String tokenId) {
        // Specifies whether the request originated from a customer
        boolean customerInitiated = true;

        Token accessToken = grantee.getTokenBlocking(tokenId);
        while (!accessToken.getReplacedByTokenId().isEmpty()) {
            accessToken = grantee.getTokenBlocking(accessToken.getReplacedByTokenId());
        }
        Set<String> accountIds = new HashSet<>();
        List<Resource> resources = accessToken.getPayload().getAccess().getResourcesList();
        boolean haveAllBalancesAccess = false;
        boolean haveAllAccountsAccess = false;
        for (int i = 0; i < resources.size(); i++) {
            Resource resource = resources.get(i);
            // TODO(Luke) fix
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
        Representable grantor = grantee.forAccessToken(accessToken.getId(), customerInitiated);
        if (haveAllAccountsAccess && haveAllBalancesAccess) {
            List<Account> accounts = grantor.getAccountsBlocking();
            for (int i = 0; i < accounts.size(); i++) {
                accountIds.add(accounts.get(i).id());
            }
        }
        // We don't have access to any accounts, so return empty balance.
        if (accountIds.size() < 1) {
            return Money.getDefaultInstance();
        }
        for (String accountId : accountIds) {
            try {
                Money balance = grantor.getBalanceBlocking(accountId, LOW).getAvailable();
                return balance;
            } catch (StatusRuntimeException ex) {
                // If grantor previously un-linked an account, then grantee can't get its balance.
                if (ex.getStatus().getCode() == FAILED_PRECONDITION) {
                    continue;
                }
                throw ex;
            }
        }
        // We don't have access to any accounts, so return empty balance.
        return Money.getDefaultInstance();
    }
}