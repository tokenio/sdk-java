package io.token;

import com.google.common.base.Strings;
import io.token.proto.common.token.TokenProtos.AccessBody;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource.AccountBalance;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource.AccountTransactions;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource.Address;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource.AllAccountBalances;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource.AllAccountTransactions;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource.AllAccounts;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource.AllAddresses;
import io.token.proto.common.token.TokenProtos.TokenMember;
import io.token.proto.common.token.TokenProtos.TokenPayload;

import static io.token.util.Util.generateNonce;

/**
 * Helps building an access token payload.
 */
public final class AccessTokenBuilder {
    private TokenPayload.Builder payload = TokenPayload.newBuilder()
            .setVersion("1.0")
            .setNonce(generateNonce())
            .setAccess(AccessBody.getDefaultInstance());

    private AccessTokenBuilder() {
    }

    private AccessTokenBuilder(TokenPayload.Builder payload) {
        this.payload = payload;
    }

    /**
     * Creates an instance of {@link AccessTokenBuilder}.
     *
     * @param redeemerUsername redeemer username
     * @return instance of {@link AccessTokenBuilder}
     */
    public static AccessTokenBuilder create(String redeemerUsername) {
        return new AccessTokenBuilder().to(redeemerUsername);
    }

    /**
     * Creates an instance of {@link AccessTokenBuilder} from an existing token payload.
     *
     * @param payload payload to initialize from
     * @return instance of {@link AccessTokenBuilder}
     */
    public static AccessTokenBuilder from(TokenPayload payload) {
        TokenPayload.Builder builder = payload.toBuilder().clearAccess();
        return new AccessTokenBuilder(builder);
    }

    /**
     * Grants access to all addresses.
     *
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forAllAddresses() {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setAllAddresses(AllAddresses.getDefaultInstance()));
        return this;
    }

    /**
     * Grants access to a given {@code addressId}.
     *
     * @param addressId address ID to grant access to
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forAddress(String addressId) {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setAddress(Address.newBuilder().setAddressId(addressId)));
        return this;
    }

    /**
     * Grants access to all accounts.
     *
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forAllAccounts() {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setAllAccounts(AllAccounts.getDefaultInstance()));
        return this;
    }

    /**
     * Grants access to a given {@code accountId}.
     *
     * @param accountId account ID to grant access to
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forAccount(String accountId) {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setAccount(Resource.Account.newBuilder().setAccountId(accountId)));
        return this;
    }

    /**
     * Grants access to all transactions.
     *
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forAllTransactions() {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setAllTransactions(AllAccountTransactions.getDefaultInstance()));
        return this;
    }

    /**
     * Grants access to a given account transactions.
     *
     * @param accountId account ID to grant access to transactions
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forAccountTransactions(String accountId) {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setTransactions(AccountTransactions.newBuilder().setAccountId(accountId)));
        return this;
    }

    /**
     * Grants access to all balances.
     *
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forAllBalances() {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setAllBalances(AllAccountBalances.getDefaultInstance()));
        return this;
    }

    /**
     * Grants access to a given account balances.
     *
     * @param accountId account ID to grant access to balances
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forAccountBalances(String accountId) {
        payload
                .getAccessBuilder()
                .addResources(Resource.newBuilder()
                        .setBalance(AccountBalance.newBuilder().setAccountId(accountId)));
        return this;
    }

    /**
     * Grants access to ALL resources (aka wildcard permissions).
     *
     * @return {@link AccessTokenBuilder}
     */
    public AccessTokenBuilder forAll() {
        return forAllAccounts().forAllAddresses().forAllBalances().forAllTransactions();
    }

    /**
     * Sets "from" field on the payload.
     *
     * @param memberId token member ID to set
     * @return {@link AccessTokenBuilder}
     */
    AccessTokenBuilder from(String memberId) {
        payload.setFrom(TokenMember.newBuilder().setId(memberId));
        return this;
    }

    /**
     * Sets "to" field on the payload.
     *
     * @param redeemerUsername redeemer username
     * @return {@link AccessTokenBuilder}
     */
    AccessTokenBuilder to(String redeemerUsername) {
        payload.setTo(TokenMember.newBuilder().setUsername(redeemerUsername));
        return this;
    }

    /**
     * Builds the {@link TokenPayload} with all specified settings.
     *
     * @return {@link AccessTokenBuilder}
     */
    TokenPayload build() {
        if (payload.getFrom() == null || Strings.isNullOrEmpty(payload.getFrom().getId())) {
            throw new IllegalArgumentException("Missing 'payload.from' value");
        }

        if (payload.getAccess().getResourcesList().isEmpty()) {
            throw new IllegalArgumentException("At least one access resource must be set");
        }

        return payload.build();
    }
}
