package io.token.sample;

import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static java.util.stream.Collectors.toList;

import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.transaction.TransactionProtos.Balance;
import io.token.user.Account;
import io.token.user.Member;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Two ways to get balances of a member's bank accounts.
 */
public final class GetBalanceSample {
    /**
     * Get a member's balances.
     *
     * @param member Member.
     * @return map currency: total
     */
    public static Map<String, Double> memberGetBalanceSample(Member member) {
        Map<String, Double> sums = new HashMap<>();

        List<Account> accounts = member.getAccountsBlocking();
        for (Account account : accounts) {
            Money balance = member.getCurrentBalanceBlocking(account.id(), STANDARD);
            sums.put(
                    balance.getCurrency(),
                    Double.parseDouble(balance.getValue())
                            + sums.getOrDefault(
                            balance.getCurrency(), 0.0));
        }

        return sums;
    }

    /**
     * Get a member's balances.
     *
     * @param member Member.
     * @return map currency: total
     */
    public static Map<String, Double> accountGetBalanceSample(Member member) {
        Map<String, Double> sums = new HashMap<>();

        List<Account> accounts = member.getAccountsBlocking();
        for (Account account : accounts) {
            Money balance = account.getCurrentBalanceBlocking(STANDARD);
            sums.put(
                    balance.getCurrency(),
                    Double.parseDouble(balance.getValue())
                            + sums.getOrDefault(
                            balance.getCurrency(), 0.0));
        }

        return sums;
    }

    /**
     * Get a member's list of balances.
     *
     * @param member Member.
     * @return list of balances
     */
    public static List<Balance> memberGetBalanceListSample(Member member) {
        List<String> accountIds = member
                .getAccountsBlocking()
                .stream()
                .map(Account::id)
                .collect(toList());

        List<Balance> balances = member.getBalancesBlocking(accountIds, STANDARD);

        return balances;
    }
}
