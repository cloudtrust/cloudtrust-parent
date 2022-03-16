package io.cloudtrust.keycloak.test.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class JsonToolboxTest {
    @ParameterizedTest
    @MethodSource("toStringSamples")
    void toStringTest(Object input, String expected) {
        Assertions.assertEquals(expected, JsonToolbox.toString(input));
    }

    public static Stream<Arguments> toStringSamples() {
        return Stream.of(
                Arguments.of(null, "(null)"),
                Arguments.of(new String[]{"hello", "world"}, "[\"hello\",\"world\"]")
        );
    }
}
