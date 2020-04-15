/**
 * Copyright (c) 2019 Token, Inc.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.token.tokenrequest;

import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.AccountResourceType.ACCOUNT_FUNDS_CONFIRMATION;

import com.google.auto.value.AutoValue;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.providerspecific.ProviderSpecific.ProviderTransferMetadata;
import io.token.proto.common.token.TokenProtos.ActingAs;
import io.token.proto.common.token.TokenProtos.BulkTransferBody;
import io.token.proto.common.token.TokenProtos.StandingOrderBody;
import io.token.proto.common.token.TokenProtos.TokenRequestOptions;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.AccountResourceList;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.AccountResourceList.AccountResource;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.ResourceType;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.ResourceTypeList;
import io.token.proto.common.token.TokenProtos.TokenRequestPayload.TransferBody;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.ChargeBearer;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.CustomerData;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferDestination;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferInstructions;
import io.token.util.Util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@AutoValue
public abstract class TokenRequest {
    public abstract TokenRequestPayload getTokenRequestPayload();

    public abstract TokenRequestOptions getTokenRequestOptions();

    /**
     * Create a new Builder instance for an access token request.
     *
     * @param resources access token resources
     * @return Builder instance
     */
    public static AccessBuilder accessTokenRequestBuilder(ResourceType... resources) {
        return new AccessBuilder(resources);
    }

    /**
     * Create a new Builder instance for an access token request with account-specific resources.
     *
     * @param list list of account-specific access token resources
     * @return Builder instance
     */
    public static AccessBuilder accessTokenRequestBuilder(AccountResourceList list) {
        return new AccessBuilder(list);
    }

    /**
     * Create a Builder instance for a funds confirmation request.
     *
     * @param bankId bank ID
     * @param account the user's account
     * @return Builder instance
     */
    public static AccessBuilder fundsConfirmationRequestBuilder(
            String bankId,
            BankAccount account) {
        return fundsConfirmationRequestBuilder(bankId, account, Optional.empty());
    }

    /**
     * Create a Builder instance for a funds confirmation request.
     *
     * @param bankId bank ID
     * @param account the user's account
     * @param data customer data
     * @return Builder instance
     */
    public static AccessBuilder fundsConfirmationRequestBuilder(
            String bankId,
            BankAccount account,
            Optional<CustomerData> data) {
        AccountResource.Builder builder = AccountResource.newBuilder()
                .setBankAccount(account)
                .setType(ACCOUNT_FUNDS_CONFIRMATION);
        data.ifPresent(builder::setCustomerData);
        return new AccessBuilder(AccountResourceList.newBuilder()
                .addResources(builder.build())
                .build())
                .setBankId(bankId);
    }

    /**
     * Create a new Builder instance for a transfer token request.
     *
     * @param amount lifetime amount of the token request
     * @param currency currency of the token request
     * @return Builder instance
     */
    public static TransferBuilder transferTokenRequestBuilder(double amount, String currency) {
        return new TransferBuilder(amount, currency);
    }

    /**
     * Create a new Builder instance for a standing order token request.
     *
     * @param amount amount per charge
     * @param currency currency per charge
     * @param frequency frequency of the standing order. ISO 20022: DAIL, WEEK, TOWK,
     *                  MNTH, TOMN, QUTR, SEMI, YEAR
     * @param startDate start date of the standing order. ISO 8601: YYYY-MM-DD or YYYYMMDD.
     * @param endDate end date of the standing order. ISO 8601: YYYY-MM-DD or YYYYMMDD.
     * @param destinations destination account of the standing order
     * @return Builder instance
     */
    public static StandingOrderBuilder standingOrderRequestBuilder(
            double amount,
            String currency,
            String frequency,
            String startDate,
            String endDate,
            List<TransferDestination> destinations) {
        return new StandingOrderBuilder(
                amount,
                currency,
                frequency,
                startDate,
                endDate,
                destinations);
    }

    /**
     * Create a new Builder instance for a bulk transfer token request.
     *
     * @param transfers list of transfers
     * @param totalAmount total amount irrespective of currency. Used for redundancy check.
     * @return Builder instance
     */
    public static BulkTransferBuilder bulkTransferRequestBuilder(
            List<BulkTransferBody.Transfer> transfers,
            double totalAmount) {
        return new BulkTransferBuilder(transfers, totalAmount);
    }

    /**
     * Construct an instance from the TokenRequestPayload and TokenRequestOptions protos.
     *
     * @param payload TokenRequestPayload
     * @param options TokenRequestOptions
     * @return TokenRequest instance
     */
    public static TokenRequest fromProtos(
            TokenRequestPayload payload,
            TokenRequestOptions options) {
        return new AutoValue_TokenRequest(payload, options);
    }

    public static class Builder<T extends Builder<T>> {
        protected TokenRequestPayload.Builder requestPayload;
        protected TokenRequestOptions.Builder requestOptions;
        protected String oauthState;
        protected String csrfToken;

        Builder() {
            this.requestPayload = TokenRequestPayload.newBuilder();
            this.requestOptions = TokenRequestOptions.newBuilder();
        }

        /**
         * Optional. Sets the bank ID in order to bypass the Token bank selection UI.
         *
         * @param bankId bank ID
         * @return builder
         */
        public T setBankId(String bankId) {
            this.requestOptions.setBankId(bankId);
            return (T) this;
        }

        /**
         * Optional. Sets the payer/grantor member ID in order to bypass the Token email input UI.
         *
         * @param fromMemberId payer/grantor member ID
         * @return builder
         */
        public T setFromMemberId(String fromMemberId) {
            this.requestOptions.getFromBuilder().setId(fromMemberId);
            return (T) this;
        }

        /**
         * Optional. Sets the payer/grantor alias in order to bypass the Token email input UI.
         *
         * @param fromAlias payer/grantor alias
         * @return builder
         */
        public T setFromAlias(Alias fromAlias) {
            this.requestOptions.getFromBuilder().setAlias(fromAlias);
            return (T) this;
        }

        /**
         * Optional. Sets the account ID of the source bank account.
         *
         * @param sourceAccountId source bank account ID
         * @return builder
         */
        public T setSourceAccount(String sourceAccountId) {
            this.requestOptions.setSourceAccountId(sourceAccountId);
            return (T) this;
        }

        /**
         * Optional. True if a receipt should be sent to the payee/grantee's default
         * receipt email/SMS/etc.
         *
         * @param receiptRequested receipt requested flag
         * @return builder
         */
        public T setReceiptRequested(boolean receiptRequested) {
            this.requestOptions.setReceiptRequested(receiptRequested);
            return (T) this;
        }

        /**
         * Optional. Sets the ID used to track a member claimed by a TPP.
         *
         * @param userRefId user ref ID
         * @return builder
         */
        public T setUserRefId(String userRefId) {
            this.requestPayload.setUserRefId(userRefId);
            return (T) this;
        }

        /**
         * Optional. Sets the ID used to customize the UI of the web-app.
         *
         * @param customizationId customization ID
         * @return builder
         */
        public T setCustomizationId(String customizationId) {
            this.requestPayload.setCustomizationId(customizationId);
            return (T) this;
        }

        /**
         * Sets the callback URL to the server that will initiate redemption of the token.
         *
         * @param redirectUrl redirect URL
         * @return builder
         */
        public T setRedirectUrl(String redirectUrl) {
            this.requestPayload.setRedirectUrl(redirectUrl);
            return (T) this;
        }

        /**
         * Sets the reference ID of the token.
         *
         * @param refId token ref ID
         * @return builder
         */
        public T setRefId(String refId) {
            this.requestPayload.setRefId(refId);
            return (T) this;
        }

        /**
         * Sets the alias of the payee/grantee.
         *
         * @param toAlias alias
         * @return builder
         */
        public T setToAlias(Alias toAlias) {
            this.requestPayload.getToBuilder()
                    .setAlias(toAlias);
            return (T) this;
        }

        /**
         * Sets the memberId of the payee/grantee.
         *
         * @param toMemberId memberId
         * @return builder
         */
        public T setToMemberId(String toMemberId) {
            this.requestPayload.getToBuilder().setId(toMemberId);
            return (T) this;
        }

        /**
         * Sets acting as on the token.
         *
         * @param actingAs entity the redeemer is acting on behalf of
         * @return builder
         */
        public T setActingAs(ActingAs actingAs) {
            this.requestPayload.setActingAs(actingAs);
            return (T) this;
        }

        /**
         * Sets the description.
         *
         * @param description description
         * @return builder
         */
        public T setDescription(String description) {
            this.requestPayload.setDescription(description);
            return (T) this;
        }

        /**
         * Sets a developer-specified string that allows state to be persisted
         * between the the request and callback phases of the flow.
         *
         * @param state state
         * @return builder
         */
        public T setState(String state) {
            this.oauthState = state;
            return (T) this;
        }

        /**
         * A nonce that will be verified in the callback phase of the flow.
         * Used for CSRF attack mitigation.
         *
         * @param csrfToken CSRF token
         * @return builder
         */
        public T setCsrfToken(String csrfToken) {
            this.csrfToken = csrfToken;
            return (T) this;
        }

        /**
         * Optional. Sets the token expiration in UTC timestamp in ms.
         *
         * @param tokenExpiration token expiration timestamp
         * @return builder
         */
        public T setTokenExpiration(long tokenExpiration) {
            this.requestPayload.setTokenExpiration(tokenExpiration);
            return (T) this;
        }

        /**
         * Builds the token payload.
         *
         * @return TokenRequest instance
         */
        public TokenRequest build() {
            String serializedState = TokenRequestState.create(
                    this.csrfToken == null ? "" : Util.hashString(this.csrfToken),
                    this.oauthState == null ? "" : this.oauthState)
                    .serialize();
            requestPayload.setCallbackState(serializedState);
            return fromProtos(requestPayload.build(), requestOptions.build());
        }
    }

    public static class AccessBuilder extends Builder<AccessBuilder> {
        AccessBuilder(ResourceType... resources) {
            List<ResourceType> resourceTypes = Arrays.asList(resources);
            this.requestPayload.setAccessBody(
                    TokenRequestPayload.AccessBody.newBuilder()
                            .setResourceTypeList(ResourceTypeList.newBuilder()
                                    .addAllResources(resourceTypes))
                            .addAllType(resourceTypes)
                            .build());
        }

        AccessBuilder(AccountResourceList list) {
            this.requestPayload.setAccessBody(
                    TokenRequestPayload.AccessBody.newBuilder()
                            .setAccountResourceList(list)
                            .build());
        }
    }

    public static class TransferBuilder extends Builder<TransferBuilder> {
        TransferBuilder(double amount, String currency) {
            this.requestPayload.setTransferBody(TransferBody.newBuilder()
                    .setLifetimeAmount(Double.toString(amount))
                    .setCurrency(currency)
                    .build());
        }

        /**
         * Optional. Sets the source account to bypass account selection. May be required for
         * some banks.
         *
         * @param source source
         * @return builder
         */
        public TransferBuilder setSource(TransferEndpoint source) {
            this.requestPayload.getTransferBodyBuilder()
                    .getInstructionsBuilder()
                    .setSource(source);
            return this;
        }

        /**
         * Optional. Sets the destination country in order to narrow down
         * the country selection in the web-app UI.
         *
         * @param destinationCountry destination country
         * @return builder
         */
        public TransferBuilder setDestinationCountry(String destinationCountry) {
            this.requestPayload.setDestinationCountry(destinationCountry);
            return this;
        }

        /**
         * Adds a transfer destination to a transfer token request.
         *
         * @param destination destination
         * @return builder
         */
        public TransferBuilder addDestination(TransferDestination destination) {
            this.requestPayload.getTransferBodyBuilder().getInstructionsBuilder()
                    .addTransferDestinations(destination);
            return this;
        }

        /**
         * Adds a transfer destination to a transfer token request.
         *
         * @param destination destination
         * @return builder
         */
        @Deprecated
        public TransferBuilder addDestination(TransferEndpoint destination) {
            this.requestPayload.getTransferBodyBuilder()
                    .addDestinations(destination);
            return this;
        }

        /**
         * Optional. Sets the maximum amount per charge on a transfer token request.
         *
         * @param chargeAmount amount
         * @return builder
         */
        public TransferBuilder setChargeAmount(double chargeAmount) {
            this.requestPayload.getTransferBodyBuilder()
                    .setAmount(Double.toString(chargeAmount))
                    .build();
            return this;
        }

        /**
         * Sets the execution date of the transfer. Used for future-dated payments.
         *
         * @param executionDate execution date
         * @return builder
         */
        public TransferBuilder setExecutionDate(LocalDate executionDate) {
            this.requestPayload.getTransferBodyBuilder()
                    .setExecutionDate(executionDate.format(DateTimeFormatter.ISO_DATE))
                    .build();
            return this;
        }

        /**
         * Optional. Adds metadata for a specific provider.
         *
         * @param metadata provider-specific metadata
         * @return builder
         */
        public TransferBuilder setProviderTransferMetadata(ProviderTransferMetadata metadata) {
            this.requestPayload.getTransferBodyBuilder()
                    .getInstructionsBuilder()
                    .getMetadataBuilder()
                    .setProviderTransferMetadata(metadata);
            return this;
        }

        /**
         * Optional. Set the bearer for any Foreign Exchange fees incurred on the transfer.
         *
         * @param chargeBearer Bearer of the charges for any Fees related to the transfer.
         * @return builder
         */
        public TransferBuilder setChargeBearer(ChargeBearer chargeBearer) {
            this.requestPayload.getTransferBodyBuilder()
                    .getInstructionsBuilder()
                    .getMetadataBuilder()
                    .setChargeBearer(chargeBearer);
            return this;
        }

        /**
         * Optional. In the scenario where TPP wishes to know the user's selection of country and
         * bank, TPP should provide this url so that Token can make a call with relevant
         * information as parameters. TPP can use that information to set transfer destination.
         *
         * @param url URL
         * @return builder
         */
        public TransferBuilder setSetTransferDestinationsUrl(String url) {
            this.requestPayload.getTransferBodyBuilder()
                    .setSetTransferDestinationsUrl(url);
            return this;
        }
          
        /**
         * Optional. Sets the ultimate party to which the money is due.
         *
         * @param ultimateCreditor the ultimate creditor
         * @return builder
         */
        public TransferBuilder setUltimateCreditor(String ultimateCreditor) {
            this.requestPayload.getTransferBodyBuilder()
                    .getInstructionsBuilder()
                    .getMetadataBuilder()
                    .setUltimateCreditor(ultimateCreditor);
            return this;
        }

        /**
         * Optional. Sets ultimate party that owes the money to the (ultimate) creditor.
         *
         * @param ultimateDebtor the ultimate debtor
         * @return builder
         */
        public TransferBuilder setUltimateDebtor(String ultimateDebtor) {
            this.requestPayload.getTransferBodyBuilder()
                    .getInstructionsBuilder()
                    .getMetadataBuilder()
                    .setUltimateCreditor(ultimateDebtor);
            return this;
        }

        /**
         * Optional. Sets the purpose code. Refer to ISO 20022 external code sets.
         *
         * @param purposeCode the purpose code
         * @return builder
         */
        public TransferBuilder setPurposeCode(String purposeCode) {
            this.requestPayload.getTransferBodyBuilder()
                    .getInstructionsBuilder()
                    .getMetadataBuilder()
                    .setPurposeCode(purposeCode);
            return this;
        }

        /**
         * Optional. Sets whether CAF should be attempted before transfer.
         *
         * @param confirmFunds whether to attempt CAF before transfer
         * @return builder
         */
        public TransferBuilder setConfirmFunds(boolean confirmFunds) {
            this.requestPayload.getTransferBodyBuilder()
                    .setConfirmFunds(confirmFunds)
                    .build();
            return this;
        }
    }

    public static class BulkTransferBuilder extends Builder<BulkTransferBuilder> {
        BulkTransferBuilder(List<BulkTransferBody.Transfer> transfers, double totalAmount) {
            this.requestPayload.setBulkTransferBody(BulkTransferBody.newBuilder()
                    .addAllTransfers(transfers)
                    .setTotalAmount(Double.toString(totalAmount)));
        }

        /**
         * Optional. Sets the source account to bypass account selection.
         *
         * @param source source
         * @return builder
         */
        public BulkTransferBuilder setSource(TransferEndpoint source) {
            this.requestPayload.getBulkTransferBodyBuilder()
                    .setSource(source);
            return this;
        }
    }

    public static class StandingOrderBuilder extends Builder<StandingOrderBuilder> {
        StandingOrderBuilder() {
            this.requestPayload.setStandingOrderBody(StandingOrderBody.newBuilder());
        }

        StandingOrderBuilder(
                double amount,
                String currency,
                String frequency,
                String startDate,
                String endDate,
                List<TransferDestination> destinations) {
            this.requestPayload.setStandingOrderBody(StandingOrderBody.newBuilder()
                    .setAmount(Double.toString(amount))
                    .setCurrency(currency)
                    .setFrequency(frequency)
                    .setStartDate(startDate)
                    .setEndDate(endDate)
                    .setInstructions(TransferInstructions.newBuilder()
                            .addAllTransferDestinations(destinations)));
        }

        /**
         * Sets the amount per charge of the standing order.
         *
         * @param amount amount per individual charge
         * @return builder
         */
        public StandingOrderBuilder setAmount(double amount) {
            this.requestPayload.getStandingOrderBodyBuilder()
                    .setAmount(Double.toString(amount));
            return this;
        }

        /**
         * Sets the currency for each charge in the standing order.
         *
         * @param currency currency
         * @return builder
         */
        public StandingOrderBuilder setCurrency(String currency) {
            this.requestPayload.getStandingOrderBodyBuilder()
                    .setCurrency(currency);
            return this;
        }

        /**
         * Sets the frequency of the standing order. ISO 20022: DAIL, WEEK, TOWK,
         * MNTH, TOMN, QUTR, SEMI, YEAR
         *
         * @param frequency frequency of the standing order
         * @return builder
         */
        public StandingOrderBuilder setFrequency(String frequency) {
            this.requestPayload.getStandingOrderBodyBuilder()
                    .setFrequency(frequency);
            return this;
        }

        /**
         * Sets the start date of the standing order. ISO 8601: YYYY-MM-DD or YYYYMMDD.
         *
         * @param startDate start date of the standing order
         * @return builder
         */
        public StandingOrderBuilder setStartDate(String startDate) {
            this.requestPayload.getStandingOrderBodyBuilder()
                    .setStartDate(startDate);
            return this;
        }

        /**
         * Sets the end date of the standing order. ISO 8601: YYYY-MM-DD or YYYYMMDD.
         * If not specified, the standing order will occur indefinitely.
         *
         * @param endDate end date of the standing order
         * @return builder
         */
        public StandingOrderBuilder setEndDate(String endDate) {
            this.requestPayload.getStandingOrderBodyBuilder()
                    .setEndDate(endDate);
            return this;
        }

        /**
         * Adds a destination account to a standing order token request.
         *
         * @param destination destination
         * @return builder
         */
        public StandingOrderBuilder addDestination(TransferDestination destination) {
            this.requestPayload.getStandingOrderBodyBuilder().getInstructionsBuilder()
                    .addTransferDestinations(destination);
            return this;
        }

        /**
         * Optional. Sets the destination country in order to narrow down
         * the country selection in the web-app UI.
         *
         * @param destinationCountry destination country
         * @return builder
         */
        public StandingOrderBuilder setDestinationCountry(String destinationCountry) {
            this.requestPayload.setDestinationCountry(destinationCountry);
            return this;
        }

        /**
         * Optional. Sets the source account to bypass account selection.
         *
         * @param source source
         * @return builder
         */
        public StandingOrderBuilder setSource(TransferEndpoint source) {
            this.requestPayload.getStandingOrderBodyBuilder()
                    .getInstructionsBuilder()
                    .setSource(source);
            return this;
        }

        /**
         * Optional. Adds metadata for a specific provider.
         *
         * @param metadata provider-specific metadata
         * @return builder
         */
        public StandingOrderBuilder setProviderTransferMetadata(ProviderTransferMetadata metadata) {
            this.requestPayload.getStandingOrderBodyBuilder()
                    .getInstructionsBuilder()
                    .getMetadataBuilder()
                    .setProviderTransferMetadata(metadata)
                    .build();
            return this;
        }

        /**
         * Optional. Sets the ultimate party to which the money is due.
         *
         * @param ultimateCreditor the ultimate creditor
         * @return builder
         */
        public StandingOrderBuilder setUltimateCreditor(String ultimateCreditor) {
            this.requestPayload.getStandingOrderBodyBuilder()
                    .getInstructionsBuilder()
                    .getMetadataBuilder()
                    .setUltimateCreditor(ultimateCreditor);
            return this;
        }

        /**
         * Optional. Sets ultimate party that owes the money to the (ultimate) creditor.
         *
         * @param ultimateDebtor the ultimate debtor
         * @return builder
         */
        public StandingOrderBuilder setUltimateDebtor(String ultimateDebtor) {
            this.requestPayload.getStandingOrderBodyBuilder()
                    .getInstructionsBuilder()
                    .getMetadataBuilder()
                    .setUltimateCreditor(ultimateDebtor);
            return this;
        }

        /**
         * Optional. Sets the purpose code. Refer to ISO 20022 external code sets.
         *
         * @param purposeCode the purpose code
         * @return builder
         */
        public StandingOrderBuilder setPurposeCode(String purposeCode) {
            this.requestPayload.getStandingOrderBodyBuilder()
                    .getInstructionsBuilder()
                    .getMetadataBuilder()
                    .setPurposeCode(purposeCode);
            return this;
        }
    }
}
