package io.token.sample;

import static io.token.TokenIO.TokenCluster.DEVELOPMENT;
import static io.token.sample.TestUtil.newAlias;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.security.SecurityProtos.Key;

import org.junit.Test;


public class ProvisionDeviceSampleTest {
    @Test
    public void provisionDevice() {
        try (TokenIO remoteDevice = TokenIO.create(DEVELOPMENT)) {
            Alias alias = newAlias();
            Member remoteMember = remoteDevice.createMember(alias);
            remoteMember.subscribeToNotifications("iron");

            TokenIO localDeviceClient = TokenIO.create(DEVELOPMENT);

            Key key = ProvisionDeviceSample.provisionDevice(localDeviceClient, alias);

            remoteMember.approveKey(key);

            Member local = ProvisionDeviceSample.useProvisionedDevice(localDeviceClient, alias);

            assertThat(local.memberId()).isEqualTo(remoteMember.memberId());
        }
    }


}
