package io.token.tpp.exceptions;

import static io.token.proto.common.eidas.EidasProtos.EidasVerificationStatus.EIDAS_STATUS_ERROR;

import io.token.proto.common.eidas.EidasProtos.EidasVerificationStatus;

public class EidasRegistrationException extends RuntimeException {
    private final EidasVerificationStatus status;

    EidasRegistrationException(EidasVerificationStatus status, String message) {
        super(message);
        this.status = status;
    }

    public EidasVerificationStatus getStatus() {
        return status;
    }

    public static EidasRegistrationException tookTooLong() {
        return new EidasRegistrationException(
                EIDAS_STATUS_ERROR,
                "Eidas verification did not finish within allowed timeframe");
    }

    public static EidasRegistrationException registrationException(
            EidasVerificationStatus status,
            String message) {
        return new EidasRegistrationException(status, message);
    }
}
