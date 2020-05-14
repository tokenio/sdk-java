package io.token.tpp.exceptions;

public class EidasTimeoutException extends RuntimeException {
    private final String memberId;
    private final String verificationId;

    public EidasTimeoutException(String memberId, String verificationId) {
        this.memberId = memberId;
        this.verificationId = verificationId;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getVerificationId() {
        return verificationId;
    }
}
