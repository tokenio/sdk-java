package io.token.testing.sample;

import io.token.proto.common.address.AddressProtos.Address;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;

public abstract class Sample {
    private Sample() {}

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
     * Picks a random value from the provided list.
     *
     * @param list the source sample list
     * @param <T> the type of the sample
     * @return a randomly picked value from the list
     */
    public static <T> T pick(List<T> list) {
        return list.get(integer(0, list.size() - 1));
    }

    /**
     * Returns a random value from an Enum type.
     *
     * @param cls the Enum class to be used as the sample source
     * @param <T> the type of the sample
     * @return a randomly picked enum entry
     */
    public static <T extends Enum<T>> T pick(Class<T> cls) {
        List<T> list = Arrays.asList(cls.getEnumConstants());
        return list.get(integer(0, list.size() - 1));
    }

    /**
     * Returns a random integer value.
     *
     * @return a random integer
     */
    public static int integer() {
        return integer(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Returns a random integer within specified boundaries.
     *
     * @param min the minimum integer value
     * @param max the maximum integer value
     * @return a random integer
     */
    public static int integer(int min, int max) {
        return (int) (Math.random() * (max - min)) + min;
    }

    /**
     * Returns a random double.
     *
     * @return a random double
     */
    public static double decimal() {
        return decimal(Double.MIN_VALUE, Double.MAX_VALUE);
    }

    /**
     * Returns a random double withing the specified boundaries.
     *
     * @param min the minimum value
     * @param max the maximum value
     * @return a random double
     */
    public static double decimal(double min, double max) {
        return min + (max - min) * Math.random();
    }

    /**
     * Returns a random double withing the specified boundaries.
     *
     * @param min the minimum value
     * @param max the maximum value
     * @param decimals the maximum number of decimals
     * @return a random double
     */
    public static double decimal(double min, double max, int decimals) {
        return BigDecimal
                .valueOf(decimal(min, max))
                .setScale(decimals, RoundingMode.HALF_EVEN)
                .doubleValue();
    }

    /**
     * Returns a random BigDecimal.
     *
     * @return a random BigDecimal
     */
    public static BigDecimal bigDecimal() {
        return bigDecimal(Double.MIN_VALUE, Double.MAX_VALUE);
    }

    /**
     * Returns a random BigDecimal withing the specified boundaries.
     *
     * @param min the minimum value
     * @param max the maximum value
     * @return a random BigDecimal
     */
    public static BigDecimal bigDecimal(double min, double max) {
        double value = decimal(min, max);
        return BigDecimal.valueOf(value);
    }

    /**
     * Returns a random BigDecimal withing the specified boundaries.
     *
     * @param min the minimum value
     * @param max the maximum value
     * @param scale the scale
     * @return a random BigDecimal
     */
    public static BigDecimal bigDecimal(double min, double max, int scale) {
        double value = decimal(min, max, scale);
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal amount() {
        return BigDecimal.valueOf(decimal(0.01, 1000000, 2));
    }

    public static Address address() {
        return Address.newBuilder()
                .setHouseNumber("425")
                .setStreet("Broadway")
                .setCity("Redwood City")
                .setPostCode("94063")
                .setCountry("US")
                .build();
    }
}
