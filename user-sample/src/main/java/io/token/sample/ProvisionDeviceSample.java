package io.token.sample;

import io.token.DeviceInfo;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.notification.NotificationProtos.DeviceMetadata;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.user.Member;
import io.token.user.TokenClient;

import java.util.Collections;

public class ProvisionDeviceSample {

    /**
     * Illustrate provisioning a new device for an already-existing member.
     * @param tokenClient SDK client
     * @param alias member's alias
     * @return key which we hope member will approve
     */
    public static Key provisionDevice(TokenClient tokenClient, Alias alias) {
        // generate keys, storing (private and public) locally
        DeviceInfo deviceInfo = tokenClient.provisionDeviceBlocking(alias);
        Key lowKey = deviceInfo.getKeys()
                .stream()
                .filter(k -> k.getLevel() == Key.Level.LOW)
                .findFirst()
                .orElse(null);
        // ask user (on "regular" device) to approve one of our keys
        NotifyStatus status = tokenClient.notifyAddKeyBlocking(
                alias,
                Collections.singletonList(lowKey),
                DeviceMetadata.newBuilder()
                        .setApplication("SDK Sample")
                        .build());
        return lowKey;
    }

    /**
     * Log in on provisioned device (assuming "remote" member approved key).
     * @param tokenClient SDK client
     * @param alias member's alias
     * @return Member
     */
    public static Member useProvisionedDevice(TokenClient tokenClient, Alias alias) {
        String memberId = tokenClient.getMemberIdBlocking(alias);
        // Uses the key that remote member approved (we hope)
        Member member = tokenClient.getMemberBlocking(memberId);
        return member;
    }
}
