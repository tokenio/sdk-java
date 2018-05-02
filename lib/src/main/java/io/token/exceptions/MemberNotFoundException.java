package io.token.exceptions;

import static java.lang.String.format;

import io.token.proto.ProtoJson;
import io.token.proto.common.alias.AliasProtos.Alias;

public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(Alias alias) {
        super(format("Member could not be resolved for alias %s", ProtoJson.toJson(alias)));
    }
}
