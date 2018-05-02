package io.token.exceptions;

import static java.lang.String.format;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.token.proto.ProtoJson;
import io.token.proto.common.alias.AliasProtos.Alias;

public class MemberNotFoundException extends StatusRuntimeException {
    public MemberNotFoundException(Alias alias) {
        super(Status.NOT_FOUND.withDescription(
                format("Member could not be resolved for alias %s", ProtoJson.toJson(alias))));
    }
}
