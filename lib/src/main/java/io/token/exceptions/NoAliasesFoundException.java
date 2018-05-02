package io.token.exceptions;

import static java.lang.String.format;

public class NoAliasesFoundException extends RuntimeException {
    public NoAliasesFoundException(String memberId) {
        super(format("Member could not be resolved for alias %s", memberId));
    }
}

