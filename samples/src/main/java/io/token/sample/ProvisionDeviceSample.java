package io.token.sample;

import io.token.DeviceInfo;
import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.notification.NotificationProtos.NotifyStatus;
import io.token.proto.common.security.SecurityProtos.Key;


public class ProvisionDeviceSample {

    /**
     * Illustrate provisioning a new device for an already-existing member.
     * @param tokenIO SDK client
     * @param alias member's alias
     * @return key which we hope member will approve
     */
    public static Key provisionDevice(TokenIO tokenIO, Alias alias) {
        // generate keys, storing (private and public) locally
        DeviceInfo deviceInfo = tokenIO.provisionDevice(alias);
        Key lowKey = deviceInfo.getKeys().stream().filter(k -> {
            return k.getLevel() == Key.Level.LOW;
        }).findFirst().orElse(null);
        // ask user (on "regular" device) to approve one of our keys
        NotifyStatus status = tokenIO.notifyAddKey(alias, "SDK Sample", lowKey);
        return lowKey;
    }

    /**
     * Log in on provisioned device (assuming "remote" member approved key).
     * @param tokenIO SDK client
     * @param alias member's alias
     * @return Member , logged in
     */
    public static Member useProvisionedDevice(TokenIO tokenIO, Alias alias) {
        String memberId = tokenIO.getMemberId(alias);
        // Uses the key that remote member approved (we hope)
        Member localLoggedIn = tokenIO.useMember(memberId);
        return localLoggedIn;
    }
}
