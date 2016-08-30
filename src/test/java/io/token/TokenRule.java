package io.token;

import io.token.proto.ProtoJson;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.account.AccountProtos.AccountLinkPayload.NamedBankAccount;
import io.token.util.Util;
import org.junit.rules.ExternalResource;

public class TokenRule extends ExternalResource {
    private final Token token = Token.builder()
            .hostName("localhost")
            .port(9000)
            .build();

    public Token token() {
       return token;
    }

    public Member member() {
        String alias = "alexey-" + Util.generateNonce();
        return token.createMember(alias);
    }

    public Account account() {
        Member member = member();

        String alias = member.getAliases().get(0);
        String bankId = "bank-id";
        String bankAccountName = "Checking";
        String bankAccountNumber = "iban:123456789";

        return member.linkAccount(
                bankId,
                ProtoJson.toJson(AccountProtos.AccountLinkPayload.newBuilder()
                        .setAlias(alias)
                        .setBankId(bankId)
                        .addAccounts(NamedBankAccount.newBuilder()
                                .setName(bankAccountName)
                                .setAccountNumber(bankAccountNumber))
                        .build()).getBytes()).get(0);
    }

    @Override
    protected void before() throws Throwable {
        super.before();
    }
}
