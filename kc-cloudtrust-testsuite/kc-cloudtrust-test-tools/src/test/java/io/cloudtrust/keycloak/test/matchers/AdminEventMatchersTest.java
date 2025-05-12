package io.cloudtrust.keycloak.test.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class AdminEventMatchersTest {
    private void checkDescription(BaseMatcher<AdminEventRepresentation> matcher, AdminEventRepresentation event) {
        checkDescription(matcher, event, " when expected value is ");
    }

    private void checkDescription(BaseMatcher<AdminEventRepresentation> matcher, AdminEventRepresentation event, String expectedText) {
        Description desc = Mockito.mock(Description.class);
        matcher.describeTo(desc);
        if (event == null) {
            Mockito.verify(desc, Mockito.times(1)).appendText("Input is null");
        } else {
            Mockito.verify(desc, Mockito.times(1)).appendText(Mockito.contains(expectedText));
        }
    }

    @ParameterizedTest
    @MethodSource("isRealmSamples")
    void isRealmTest(AdminEventRepresentation event, String realm, boolean matches) {
        var matcher = AdminEventMatchers.isRealm(realm);
        assertThat(matcher.matches(event), is(matches));

        if (!matches) {
            // Does not make sens to test describeTo when assertion matches
            checkDescription(matcher, event);
        }
    }

    private static Stream<Arguments> isRealmSamples() {
        var eventInTestRealm = createEvent("test", null);
        var eventInDemoRealm = createEvent("demo", null);

        return Stream.of(
                Arguments.of(eventInTestRealm, "test", true),
                Arguments.of(eventInDemoRealm, "test", false),
                Arguments.of(eventInTestRealm, "demo", false),
                Arguments.of(eventInDemoRealm, "demo", true),
                Arguments.of(null, "test", false)
        );
    }

    @ParameterizedTest
    @MethodSource("hasNoErrorSamples")
    void hasNoErrorTest(AdminEventRepresentation event, boolean matches) {
        var matcher = AdminEventMatchers.hasNoError();
        assertThat(matcher.matches(event), is(matches));

        if (!matches) {
            // Does not make sens to test describeTo when assertion matches
            checkDescription(matcher, event, "reports an error when no error");
        }
    }

    private static Stream<Arguments> hasNoErrorSamples() {
        var eventNoError = createEvent(null, null);
        var eventError = createEvent(null, "error value");

        return Stream.of(
                Arguments.of(null, false),
                Arguments.of(eventNoError, true),
                Arguments.of(eventError, false)
        );
    }

    @ParameterizedTest
    @MethodSource("hasErrorSamples")
    void hasErrorTest(AdminEventRepresentation event, String expectedError, boolean matches) {
        var matcher = expectedError == null ? AdminEventMatchers.hasError() : AdminEventMatchers.hasError(expectedError);
        assertThat(matcher.matches(event), is(matches));

        if (!matches) {
            // Does not make sens to test describeTo when assertion matches
            checkDescription(matcher, event, " reports no error ");
        }
    }

    private static Stream<Arguments> hasErrorSamples() {
        var eventNoError = createEvent(null, null);
        var eventError = createEvent(null, "error value");

        return Stream.of(
                Arguments.of(null, null, false),
                Arguments.of(eventNoError, null, false),
                Arguments.of(eventError, null, true),
                Arguments.of(eventError, "error value", true),
                Arguments.of(eventError, "another value", false)
        );
    }

    @ParameterizedTest
    @MethodSource("existsSamples")
    void existsTest(AdminEventRepresentation event, boolean exists) {
        var matcher = AdminEventMatchers.exists();
        assertThat(matcher.matches(event), is(exists));

        if (!exists) {
            // Does not make sens to test describeTo when assertion matches
            checkDescription(matcher, event, "xxx");
        }
    }

    @ParameterizedTest
    @MethodSource("existsSamples")
    void doesNotExistTest(AdminEventRepresentation event, boolean exists) {
        var matcher = AdminEventMatchers.doesNotExist();
        assertThat(matcher.matches(event), is(!exists));

        if (exists) {
            // Does not make sens to test describeTo when assertion matches
            checkDescription(matcher, event, "No event was expected but found ");
        }
    }

    private static Stream<Arguments> existsSamples() {
        var anyEvent = createEvent(null, null);

        return Stream.of(
                Arguments.of(null, false),
                Arguments.of(anyEvent, true)
        );
    }

    private static AdminEventRepresentation createEvent(String realm, String error) {
        var res = new AdminEventRepresentation();
        res.setRealmId(realm);
        res.setError(error);
        return res;
    }
}
