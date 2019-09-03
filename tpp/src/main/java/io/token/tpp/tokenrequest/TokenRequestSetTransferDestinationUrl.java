package io.token.tpp.tokenrequest;

import com.google.auto.value.AutoValue;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.PaymentSystem;

import java.util.List;

@AutoValue
public abstract class TokenRequestSetTransferDestinationUrl {

    public static TokenRequestSetTransferDestinationUrl create(
            String region,
            String country,
            String bank,
            List<PaymentSystem> supportedPayments) {
        return new AutoValue_TokenRequestSetTransferDestinationUrl(
                region,
                country,
                bank,
                supportedPayments);
    }

    /**
     * Get the economic region to which the selected country belongs.
     *
     * @return economic region
     */
    public abstract String getRegion();

    /**
     * Get the Country code selected by the user.
     *
     * @return country
     */
    public abstract String getCountry();

    /**
     * Get the bank selected by the user.
     *
     * @return bank
     */
    public abstract String getBank();

    /**
     * Get a list of supported payments by the bank.
     *
     * @return list of supported payments
     */
    public abstract List<PaymentSystem> getSupportedPayments();
}
