package io.token.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.security.SecurityProtos.Key;

import org.junit.Test;

public class ProvisionDeviceSampleTest {
    @Test
    public void provisionDevice() {
        try (TokenIO remoteDevice = TestUtil.createClient()) {
            Alias alias = TestUtil.randomAlias();
            Member remoteMember = remoteDevice.createMember(alias);
            remoteMember.subscribeToNotifications("iron");

            TokenIO localDeviceClient = TestUtil.createClient();
            Key key = ProvisionDeviceSample.provisionDevice(localDeviceClient, alias);
            remoteMember.approveKey(key);

            Member local = ProvisionDeviceSample.useProvisionedDevice(localDeviceClient, alias);

            assertThat(local.memberId()).isEqualTo(remoteMember.memberId());
        }
    }
}
