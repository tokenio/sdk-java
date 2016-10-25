package io.token.testing.sample;

import io.token.proto.common.address.AddressProtos.Address;
import org.apache.commons.lang3.RandomStringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Supplier;

public interface Sample {
    /**
     * Returns a random alphanumeric string of a given length
     *
     * @return a random string
     */
    static String string() {
        return string(20);
    }

    /**
     * Returns a random alphanumeric string of a given length
     *
     * @param length the length of the string
     * @return a random string
     */
    static String string(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }

    /**
     * Returns a list of supplied values
     * Example:
     * List<String> list = Sample.list(Sample::string)
     *
     * @param sample the sample of values
     * @param <T> the type of the sample
     * @return a sample list
     */
    static <T> List<T> list(Supplier<T> sample) {
        List<T> list = new LinkedList<>();
        for (int i = 0; i < 5; i++) {
            list.add(sample.get());
        }
        return list;
    }

    /**
     * Returns a map of sample key/value pairs
     * Example:
     * Map<String,Integer> map = Sample.map(Sample::string, Sample::integer)
     *
     * @param keySample the sample of key values
     * @param valueSample the sample of values
     * @param <K> the type of keys
     * @param <V> the type of values
     * @return a sample map
     */
    static <K, V> Map<K, V> map(Supplier<K> keySample, Supplier<V> valueSample) {
        Map<K, V> map = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            map.put(keySample.get(), valueSample.get());
        }
        return map;
    }

    /**
     * Returns a random optional value of the provided sample
     * Example:
     * Optional<String> maybeString = Sample.optional(Sample::string)
     *
     * @param sample the sample source
     * @param <T> the type of the sample
     * @return a random choice of a generated sample or empty value
     */
    static <T> Optional<T> optional(Supplier<T> sample) {
        return (Math.random() < 0.5) ? Optional.of(sample.get()) : Optional.empty();
    }

    /**
     * Picks a random value from the provided list
     *
     * @param list the source sample list
     * @param <T> the type of the sample
     * @return a randomly picked value from the list
     */
    static <T> T pick(List<T> list) {
        return list.get(integer(0, list.size() - 1));
    }

    /**
     * Returns a random value from an Enum type
     * Example:
     * DayOfWeek randomDayOfWeek = Sample.pick(DayOfWeek.class)
     *
     * @param cls the Enum class to be used as the sample source
     * @param <T> the type of the sample
     * @return a randomly picked enum entry
     */
    static <T extends Enum<T>> T pick(Class<T> cls) {
        List<T> list = Arrays.asList(cls.getEnumConstants());
        return list.get(integer(0, list.size() - 1));
    }

    /**
     * Returns a random integer value
     *
     * @return a random integer
     */
    static int integer() {
        return integer(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Returns a random integer within specified boundaries
     * where integer i :: min >= i <= max
     *
     * @param min the minimum integer value
     * @param max the maximum integer value
     * @return a random integer
     */
    static int integer(int min, int max) {
        return (int) (Math.random() * (max - min)) + min;
    }

    /**
     * Returns a random double
     *
     * @return a random double
     */
    static double decimal() {
        return decimal(Double.MIN_VALUE, Double.MAX_VALUE);
    }

    /**
     * Returns a random double withing the specified boundaries
     *
     * @param min the minimum value
     * @param max the maximum value
     * @return a random double
     */
    static double decimal(double min, double max) {
        return min + (max - min) * Math.random();
    }

    /**
     * Returns a random double withing the specified boundaries
     *
     * @param min the minimum value
     * @param max the maximum value
     * @param decimals the maximum number of decimals
     * @return a random double
     */
    static double decimal(double min, double max, int decimals) {
        return BigDecimal
                .valueOf(decimal(min, max))
                .setScale(decimals, RoundingMode.HALF_EVEN)
                .doubleValue();
    }

    /**
     * Returns a random BigDecimal
     *
     * @return a random BigDecimal
     */
    static BigDecimal bigDecimal() {
        return bigDecimal(Double.MIN_VALUE, Double.MAX_VALUE);
    }

    /**
     * Returns a random BigDecimal withing the specified boundaries
     *
     * @param min the minimum value
     * @param max the maximum value
     * @return a random BigDecimal
     */
    static BigDecimal bigDecimal(double min, double max) {
        double value = decimal(min, max);
        return BigDecimal.valueOf(value);
    }

    /**
     * Returns a random BigDecimal withing the specified boundaries
     *
     * @param min the minimum value
     * @param max the maximum value
     * @param scale the scale
     * @return a random BigDecimal
     */
    static BigDecimal bigDecimal(double min, double max, int scale) {
        double value = decimal(min, max, scale);
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_EVEN);
    }

    static BigDecimal amount() {
        double d = decimal(0.01, 1000000, 2);
        return BigDecimal.valueOf(d);
    }

    static Address address() {
        return Address.newBuilder()
                .setHouseNumber("425")
                .setStreet("Broadway")
                .setCity("Redwood City")
                .setPostCode("94063")
                .setCountry("US")
                .build();
    }
}
