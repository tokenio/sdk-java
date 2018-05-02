package io.token.exceptions;

import static java.lang.String.format;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class NoAliasesFoundException extends StatusRuntimeException {
    public NoAliasesFoundException(String memberId) {
        super(Status.NOT_FOUND.withDescription(
                format("No aliases found for member : %s", memberId)));
    }
}

