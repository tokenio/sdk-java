package io.token;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.member.MemberProtos.AddressRecord;
import io.token.testing.sample.Sample;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.token.testing.sample.Sample.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionThrownBy;

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
        Map<String, Address> addressMap = map(Sample::string, Sample::address);
        addressMap.forEach((name, data) -> member.addAddress(name, data));
        List<AddressRecord> addresses = member.getAddresses();

        List<String> names = addresses.stream()
                .map(AddressRecord::getName)
                .collect(Collectors.toList());

        List<Address> values = addresses.stream()
                .map(AddressRecord::getAddress)
                .collect(Collectors.toList());

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
        String fakeAddressId = string();
        assertThatExceptionThrownBy(() -> member.getAddress(fakeAddressId))
                .isInstanceOf(StatusRuntimeException.class)
                .matches(ex ->
                        ((StatusRuntimeException) ex).getStatus().getCode() == Status.Code.NOT_FOUND);
    }

    @Test
    public void deleteAddress() {
        String name = string();
        Address payload = address();
        AddressRecord address = member.addAddress(name, payload);
        member.getAddress(address.getId());

        member.deleteAddress(address.getId());

        assertThatExceptionThrownBy(() -> member.getAddress(address.getId()))
                .isInstanceOf(StatusRuntimeException.class)
                .matches(ex ->
                        ((StatusRuntimeException) ex).getStatus().getCode() == Status.Code.NOT_FOUND);
    }

    @Test
    public void deleteAddress_NotFound() {
        String fakeAddressId = string();
        assertThatExceptionThrownBy(() -> member.getAddress(fakeAddressId))
                .isInstanceOf(StatusRuntimeException.class)
                .matches(ex ->
                        ((StatusRuntimeException) ex).getStatus().getCode() == Status.Code.NOT_FOUND);
    }
}
