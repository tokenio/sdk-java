package io.token.sample;

import io.token.Account;
import io.token.Member;
import io.token.proto.common.money.MoneyProtos.Money;

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

        List<Account> accounts = member.getAccounts();
        for (Account account : accounts) {
            Money balance = member.getCurrentBalance(account.id());
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

        List<Account> accounts = member.getAccounts();
        for (Account account : accounts) {
            Money balance = account.getCurrentBalance();
            sums.put(
                    balance.getCurrency(),
                    Double.parseDouble(balance.getValue())
                            + sums.getOrDefault(
                            balance.getCurrency(), 0.0));
        }

        return sums;
    }
}
