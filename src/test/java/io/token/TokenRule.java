package io.token;

import io.token.proto.ProtoJson;
import io.token.proto.common.account.AccountProtos.AccountLinkPayload;
import io.token.proto.common.account.AccountProtos.AccountLinkPayload.NamedAccount;
import io.token.util.Util;
import org.junit.rules.ExternalResource;

import static org.assertj.core.util.Strings.isNullOrEmpty;

/**
 * One can control what gateway the tests run against by setting system property on the
 * command line. E.g:
 *
 * ./gradlew -DTOKEN_GATEWAY=some-ip test
 */
public class TokenRule extends ExternalResource {
    private final Token token;

    public TokenRule() {
        String hostName = "localhost";
        int port = 9000;

        String override = System.getenv("TOKEN_GATEWAY");
        if (isNullOrEmpty(override)) {
            override = System.getProperty("TOKEN_GATEWAY");
        }
        if (!isNullOrEmpty(override)) {
            String[] hostAndPort = override.split(":");
            switch (hostAndPort.length) {
                case 1:
                    hostName = hostAndPort[0];
                    break;
                case 2:
                    hostName = hostAndPort[0];
                    port = Integer.parseInt(hostAndPort[1]);
                    break;
                default:
                    throw new RuntimeException("Invalid TOKEN_GATEWAY format: " + override);
            }
        }

        this.token = Token.builder()
                .hostName(hostName)
                .port(port)
                .build();
    }

    public Token token() {
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
        String bankAccountNumber = "iban:123456789";

        if (alias == null) {
            throw new IllegalStateException("Member doesn't have an alias");
        }

        return member.linkAccount(
                bankId,
                ProtoJson.toJson(AccountLinkPayload.newBuilder()
                        .setAlias(alias)
                        .addAccounts(NamedAccount.newBuilder()
                                .setName(bankAccountName)
                                .setAccountNumber(bankAccountNumber))
                        .build()).getBytes()).get(0);
    }

    @Override
    protected void before() throws Throwable {
        super.before();
    }
}
