package io.token;

import static io.token.testing.sample.Sample.address;
import static io.token.testing.sample.Sample.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.member.MemberProtos.AddressRecord;
import io.token.testing.sample.Sample;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.ThrowableAssert;
import org.junit.Rule;
import org.junit.Test;

public class AddressTest {
    @Rule
    public TokenRule rule = new TokenRule();
    private Member member = rule.member();

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
        assertThatThrownBy(
                new ThrowableAssert.ThrowingCallable() {
                    public void call() throws Throwable {
                        member.getAddress(fakeAddressId);
                    }
                })
                .isInstanceOf(StatusRuntimeException.class)
                .extracting("status")
                .extracting("code", Status.Code.class)
                .extractingResultOf("value", int.class)
                .contains(Status.Code.NOT_FOUND.value());
    }

    @Test
    public void deleteAddress() {
        String name = string();
        Address payload = address();
        final AddressRecord address = member.addAddress(name, payload);
        member.getAddress(address.getId());

        member.deleteAddress(address.getId());

        assertThatThrownBy(
                new ThrowableAssert.ThrowingCallable() {
                    public void call() throws Throwable {
                        member.getAddress(address.getId());
                    }
                })
                .isInstanceOf(StatusRuntimeException.class)
                .extracting("status")
                .extracting("code", Status.Code.class)
                .extractingResultOf("value", int.class)
                .contains(Status.Code.NOT_FOUND.value());
    }

    @Test
    public void deleteAddress_NotFound() {
        final String fakeAddressId = string();
        assertThatThrownBy(
                new ThrowableAssert.ThrowingCallable() {
                    public void call() throws Throwable {
                        member.getAddress(fakeAddressId);
                    }
                })
                .isInstanceOf(StatusRuntimeException.class)
                .extracting("status")
                .extracting("code", Status.Code.class)
                .extractingResultOf("value", int.class)
                .contains(Status.Code.NOT_FOUND.value());
    }
}
