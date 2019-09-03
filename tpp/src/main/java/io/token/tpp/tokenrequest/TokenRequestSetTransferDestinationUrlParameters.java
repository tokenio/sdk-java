package io.token.tpp.tokenrequest;

import static io.token.tpp.util.Util.urlDecode;

import com.google.auto.value.AutoValue;
import io.token.proto.ProtoJson;
import io.token.proto.common.security.SecurityProtos.Signature;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.PaymentSystem;
import io.token.tpp.exceptions.InvalidTokenRequestQuery;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AutoValue
public abstract class TokenRequestSetTransferDestinationUrlParameters {
    private static final String REGION_FIELD = "region";
    private static final String COUNTRY_FIELD = "country";
    private static final String BANK_FIELD = "bank";
    private static final String SUPPORTED_PAYMENTS = "supportedPayments";
    private static final String STATE_FIELD = "state";
    private static final String SIGNATURE_FIELD = "signature";

    public static TokenRequestSetTransferDestinationUrlParameters create(
            Map<String, String> parameters) {
        if (!parameters.containsKey(REGION_FIELD)
                || !parameters.containsKey(SUPPORTED_PAYMENTS)
                || !parameters.containsKey(STATE_FIELD)
                || !parameters.containsKey(SIGNATURE_FIELD)) {
            throw new InvalidTokenRequestQuery();
        }

        List<PaymentSystem> supportedPayments = Arrays.stream(urlDecode(parameters.get(
                SUPPORTED_PAYMENTS)).split(","))
                .map(paymentSystem -> PaymentSystem.valueOf(paymentSystem))
                .collect(Collectors.toList());

        return new AutoValue_TokenRequestSetTransferDestinationUrlParameters(
                urlDecode(parameters.get(REGION_FIELD)),
                urlDecode(parameters.get(COUNTRY_FIELD)),
                urlDecode(parameters.get(BANK_FIELD)),
                supportedPayments,
                urlDecode(parameters.get(STATE_FIELD)),
                ProtoJson.fromJson(urlDecode(
                        parameters.get(SIGNATURE_FIELD)),
                        Signature.newBuilder()));
    }

    public abstract String getRegion();

    public abstract String getCountry();

    public abstract String getBank();

    public abstract List<PaymentSystem> getSupportedPayments();

    public abstract String getSerializedState();

    public abstract Signature getSignature();
}
