package io.token;

import io.token.proto.common.security.SecurityProtos.Key;

import java.util.List;

/**
 * Information about a device being provisioned.
 */
public class DeviceInfo {
    private final String memberId;
    private final List<Key> keys;

    /**
     * Creates an instance.
     *
     * @param memberId member id
     * @param keys list of keys
     */
    public DeviceInfo(String memberId, List<Key> keys) {
        this.memberId = memberId;
        this.keys = keys;
    }

    public String getMemberId() {
        return memberId;
    }

    public List<Key> getKeys() {
        return keys;
    }
}
