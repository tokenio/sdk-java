package io.token.testing.sample;

import io.token.proto.common.address.AddressProtos.Address;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;

public abstract class Sample {
    private Sample() {
    }

    /**
     * Returns a random alphanumeric string of a given length.
     *
     * @return a random string
     */
    public static String string() {
        return string(20);
    }

    /**
     * Returns a random alphanumeric string of a given length.
     *
     * @param length the length of the string
     * @return a random string
     */
    public static String string(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }

    /**
     * Returns a physical address for testing.
     *
     * @return address object
     */
    public static Address address() {
        return Address.newBuilder()
                .setHouseNumber("425")
                .setStreet("Broadway")
                .setCity("Redwood City")
                .setPostCode("94063")
                .setCountry("US")
                .build();
    }

    /**
     * Returns notification handler instructions.
     *
     * @param target notification target
     * @param platform notification platform
     * @return handler instructions
     */
    public static Map<String, String> handlerInstructions(String target, String platform) {
        Map<String, String> handlerInstructions = new HashMap<>();
        handlerInstructions.put("TARGET", target);
        handlerInstructions.put("PLATFORM", platform);
        return handlerInstructions;
    }
}
