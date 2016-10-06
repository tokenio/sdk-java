package io.token;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.token.proto.common.member.MemberProtos.Address;
import io.token.testing.sample.Sample;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.token.testing.sample.Sample.map;
import static io.token.testing.sample.Sample.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionThrownBy;

public class AddressTest {
    @Rule
    public TokenRule rule = new TokenRule();
    private Member member = rule.member();

    @Test
    public void addAddress() {
        String name = string();
        String data = string();
        Address address = member.addAddress(name, data);
        assertThat(address.getName()).isEqualTo(name);
        assertThat(address.getData()).isEqualTo(data);
    }

    @Test
    public void addAndGetAddress() {
        String name = string();
        String data = string();
        Address address = member.addAddress(name, data);
        Address result = member.getAddress(address.getId());
        assertThat(address).isEqualTo(result);
    }

    @Test
    public void createAndGetAddresses() {
        Map<String,String> addressMap = map(Sample::string, Sample::string);
        addressMap.forEach((name, data) ->
                member.addAddress(name, data));
        List<Address> addresses = member.getAddresses();

        List<String> names = addresses.stream()
                .map(Address::getName)
                .collect(Collectors.toList());

        List<String> values = addresses.stream()
                .map(Address::getData)
                .collect(Collectors.toList());

        assertThat(addressMap).containsOnlyKeys(names.toArray(new String[0]));
        assertThat(addressMap).containsValues(values.toArray(new String[0]));
    }

    @Test
    public void getAddresses_NotFound() {
        List<Address> addresses = member.getAddresses();
        assertThat(addresses).isEmpty();
    }

    @Test
    public void getAddress_NotFound() {
        String fakeAddressId = string();
        assertThatExceptionThrownBy(() -> member.getAddress(fakeAddressId))
                .isInstanceOf(StatusRuntimeException.class)
                .matches(ex ->
                        ((StatusRuntimeException)ex).getStatus().getCode() == Status.Code.NOT_FOUND);
    }

    @Test
    public void deleteAddress() {
        String name = string();
        String data = string();
        Address address = member.addAddress(name, data);
        member.getAddress(address.getId());

        member.deleteAddress(address.getId());

        assertThatExceptionThrownBy(() -> member.getAddress(address.getId()))
                .isInstanceOf(StatusRuntimeException.class)
                .matches(ex ->
                        ((StatusRuntimeException)ex).getStatus().getCode() == Status.Code.NOT_FOUND);
    }

    @Test
    public void deleteAddress_NotFound() {
        String fakeAddressId = string();
        assertThatExceptionThrownBy(() -> member.getAddress(fakeAddressId))
                .isInstanceOf(StatusRuntimeException.class)
                .matches(ex ->
                        ((StatusRuntimeException)ex).getStatus().getCode() == Status.Code.NOT_FOUND);
    }
}
