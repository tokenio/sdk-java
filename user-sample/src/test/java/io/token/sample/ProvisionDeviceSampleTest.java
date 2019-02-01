package io.token.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.security.SecurityProtos.Key;
import io.token.user.Member;
import io.token.user.TokenClient;

import org.junit.Test;

public class ProvisionDeviceSampleTest {
    @Test
    public void provisionDevice() {
        try (TokenClient remoteDevice = TestUtil.createClient()) {
            Alias alias = TestUtil.randomAlias();
            Member remoteMember = remoteDevice.createMemberBlocking(alias);
            remoteMember.subscribeToNotifications("iron");

            TokenClient localDeviceClient = TestUtil.createClient();
            Key key = ProvisionDeviceSample.provisionDevice(localDeviceClient, alias);
            remoteMember.approveKeyBlocking(key);

            Member local = ProvisionDeviceSample.useProvisionedDevice(localDeviceClient, alias);

            assertThat(local.memberId()).isEqualTo(remoteMember.memberId());
        }
    }
}
