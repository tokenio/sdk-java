package io.token;

import static io.grpc.Status.Code.NOT_FOUND;
import static io.token.testing.sample.Sample.address;
import static io.token.testing.sample.Sample.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.grpc.StatusRuntimeException;
import io.token.common.TokenRule;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.member.MemberProtos.AddressRecord;
import io.token.testing.sample.Sample;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AddressTest {
    @Rule
    public TokenRule rule = new TokenRule();
    private Member member = rule.member();

    @Before
    public void before() {
        this.member = rule.member();
    }

    @Test
    public void addAddress() {
        String name = string();
        Address payload = address();
        AddressRecord address = member.addAddress(name, payload);
        assertThat(address.getName()).isEqualTo(name);
        assertThat(address.getAddress()).isEqualTo(payload);
    }

    @Test
    public void addAndGetAddress() {
        String name = string();
        Address payload = address();
        AddressRecord address = member.addAddress(name, payload);
        AddressRecord result = member.getAddress(address.getId());
        assertThat(address).isEqualTo(result);
    }

    @Test
    public void createAndGetAddresses() {
        Map<String, Address> addressMap = new HashMap<>();
        addressMap.put(Sample.string(), Sample.address());
        addressMap.put(Sample.string(), Sample.address());
        addressMap.put(Sample.string(), Sample.address());

        for (Map.Entry<String, Address> entry : addressMap.entrySet()) {
            member.addAddress(entry.getKey(), entry.getValue());
        }

        List<AddressRecord> addresses = member.getAddresses();

        List<String> names = new LinkedList<>();
        for (AddressRecord record : addresses) {
            names.add(record.getName());
        }

        List<Address> values = new LinkedList<>();
        for (AddressRecord record : addresses) {
            values.add(record.getAddress());
        }

        assertThat(addressMap).containsOnlyKeys(names.toArray(new String[0]));
        assertThat(addressMap).containsValues(values.toArray(new Address[0]));
    }

    @Test
    public void getAddresses_NotFound() {
        List<AddressRecord> addresses = member.getAddresses();
        assertThat(addresses).isEmpty();
    }

    @Test
    public void getAddress_NotFound() {
        final String fakeAddressId = string();
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> member.getAddress(fakeAddressId))
                .matches(e -> e.getStatus().getCode() == NOT_FOUND);
    }

    @Test
    public void deleteAddress() {
        String name = string();
        Address payload = address();
        final AddressRecord address = member.addAddress(name, payload);
        member.getAddress(address.getId());

        member.deleteAddress(address.getId());

        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> member.getAddress(address.getId()))
                .matches(e -> e.getStatus().getCode() == NOT_FOUND);
    }

    @Test
    public void deleteAddress_NotFound() {
        final String fakeAddressId = string();
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(() -> member.getAddress(fakeAddressId))
                .matches(e -> e.getStatus().getCode() == NOT_FOUND);
    }
}
