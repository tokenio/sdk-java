//package io.token.sample;
//
//import static io.token.sample.CreateAndEndorseAccessTokenSample.createAccessToken;
//import static io.token.sample.RedeemAccessTokenSample.carefullyUseAccessToken;
//
//import static io.token.usample.RedeemAccessTokenSample.redeemAccessToken;
//import static io.token.sample.TestUtil.createClient;
//import static io.token.sample.TestUtil.createMemberAndLinkAccounts;
//import static io.token.sample.TestUtil.randomAlias;
//import static org.assertj.core.api.Assertions.assertThat;
//
//import io.token.proto.MoneyUtil;
//import io.token.proto.common.alias.AliasProtos.Alias;
//import io.token.proto.common.money.MoneyProtos.Money;
//import io.token.proto.common.token.TokenProtos.Token;
//import io.token.proto.common.token.TokenProtos.TokenOperationResult;
//import io.token.user.AccessTokenBuilder;
//import io.token.user.Member;
//import io.token.user.TokenClient;
//
//import java.math.BigDecimal;
//import java.util.Arrays;
//
//import org.junit.Test;
//
//public class RedeemAccessTokenSampleTest {
//    @Test
//    public void redeemAccessTokenTest() {
//        try (TokenClien tokenClient = createClient()) {
//            Member grantor = createMemberAndLinkAccounts(tokenIO);
//            String accountId = grantor.getAccounts().get(0).id();
//            Alias granteeAlias = randomAlias();
//            Member grantee = tokenIO.createMember(granteeAlias);
//
//            Token token = createAccessToken(grantor, accountId, granteeAlias);
//            Money balance0 = redeemAccessToken(grantee, token.getId());
//            assertThat(MoneyUtil.parseAmount(balance0.getValue())).isGreaterThan(BigDecimal.TEN);
//        }
//    }
//}
