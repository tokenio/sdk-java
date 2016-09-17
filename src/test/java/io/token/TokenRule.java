package io.token;

import com.google.common.net.HostAndPort;
import io.token.proto.bankapi.Fank;
import io.token.proto.common.money.MoneyProtos;
import io.token.util.Util;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.rules.ExternalResource;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.util.Strings.isNullOrEmpty;

/**
 * One can control what gateway the tests run against by setting system property on the
 * command line. E.g:
 *
 * ./gradlew -DTOKEN_GATEWAY=some-ip -DTOKEN_BANK=some-ip test
 */
public class TokenRule extends ExternalResource {
    private final TokenIO token;
    private final BankClient bankClient;

    public TokenRule() {
        HostAndPort gateway = HostAndPort
                .fromString(getHostPortString("TOKEN_GATEWAY", "localhost"))
                .withDefaultPort(9000);

        HostAndPort bank = HostAndPort
                .fromString(getHostPortString("TOKEN_BANK", "localhost"))
                .withDefaultPort(9100);

        this.bankClient = new BankClient(
                bank.getHostText(),
                bank.getPort());

        this.token = TokenIO.builder()
                .hostName(gateway.getHostText())
                .port(gateway.getPort())
                .build();
    }

    public TokenIO token() {
       return token;
    }

    public Member member() {
        String alias = "alias-" + Util.generateNonce();
        return token.createMember(alias);
    }

    public Account account() {
        Member member = member();

        String alias = member.getFirstAlias();
        String bankId = "bank-id";
        String bankAccountName = "Checking";
        String bankAccountNumber = "iban:" + randomInt(7);

        if (alias == null) {
            throw new IllegalStateException("Member doesn't have an alias");
        }

        Fank.FankMetadata metadata = Fank.FankMetadata.newBuilder()
                .setClient(Fank.FankMetadata.Client.newBuilder()
                        .setFirstName(string())
                        .setLastName(string()))
                .addClientAccounts(Fank.FankMetadata.ClientAccount.newBuilder()
                        .setAccountNumber(bankAccountNumber)
                        .setName(bankAccountName)
                        .setBalance(MoneyProtos.Money.newBuilder()
                                .setCurrency("USD")
                                .setValue(1000000.00)))
                .build();

        byte[] accountLinkingPayload = bankClient.startAccountsLinking(
                alias,
                Optional.empty(),
                Collections.singletonList(bankAccountNumber),
                Optional.of(metadata));

        return member
                .linkAccounts(bankId, accountLinkingPayload)
                .get(0);
    }

    @Override
    protected void before() throws Throwable {
        super.before();
    }

    private static String getHostPortString(String name, String defaultValue) {
        String override = System.getenv(name);
        if (!isNullOrEmpty(override)) {
            return override;
        }

        override = System.getProperty(name);
        if (!isNullOrEmpty(override)) {
            return override;
        }

        return defaultValue;
    }

    private static String string() {
        int length = randomInt(3,7);
        return RandomStringUtils.randomAlphabetic(length);
    }

    private static int randomInt(int digits) {
        return randomInt(
                (int) Math.pow(10, digits),
                (int) Math.pow(10, digits+1)-1);
    }

    private static int randomInt(int min, int max){
        return (int)(Math.random() * (max-min)) + min;
    }
}
