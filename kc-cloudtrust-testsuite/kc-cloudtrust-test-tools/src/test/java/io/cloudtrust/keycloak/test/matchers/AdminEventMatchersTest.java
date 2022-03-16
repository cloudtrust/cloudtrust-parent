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
        BaseMatcher<AdminEventRepresentation> matcher = AdminEventMatchers.isRealm(realm);
        assertThat(matcher.matches(event), is(matches));

        if (!matches) {
            // Does not make sens to test describeTo when assertion matches
            checkDescription(matcher, event);
        }
    }

    private static Stream<Arguments> isRealmSamples() {
        AdminEventRepresentation eventInTestRealm = createEvent("test", null);
        AdminEventRepresentation eventInDemoRealm = createEvent("demo", null);

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
        BaseMatcher<AdminEventRepresentation> matcher = AdminEventMatchers.hasNoError();
        assertThat(matcher.matches(event), is(matches));

        if (!matches) {
            // Does not make sens to test describeTo when assertion matches
            checkDescription(matcher, event, "reports an error when no error");
        }
    }

    private static Stream<Arguments> hasNoErrorSamples() {
        AdminEventRepresentation eventNoError = createEvent(null, null);
        AdminEventRepresentation eventError = createEvent(null, "error value");

        return Stream.of(
                Arguments.of(null, false),
                Arguments.of(eventNoError, true),
                Arguments.of(eventError, false)
        );
    }

    @ParameterizedTest
    @MethodSource("hasErrorSamples")
    void hasErrorTest(AdminEventRepresentation event, String expectedError, boolean matches) {
        BaseMatcher<AdminEventRepresentation> matcher = expectedError == null ? AdminEventMatchers.hasError() : AdminEventMatchers.hasError(expectedError);
        assertThat(matcher.matches(event), is(matches));

        if (!matches) {
            // Does not make sens to test describeTo when assertion matches
            checkDescription(matcher, event, " reports no error ");
        }
    }

    private static Stream<Arguments> hasErrorSamples() {
        AdminEventRepresentation eventNoError = createEvent(null, null);
        AdminEventRepresentation eventError = createEvent(null, "error value");

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
        BaseMatcher<AdminEventRepresentation> matcher = AdminEventMatchers.exists();
        assertThat(matcher.matches(event), is(exists));

        if (!exists) {
            // Does not make sens to test describeTo when assertion matches
            checkDescription(matcher, event, "xxx");
        }
    }

    @ParameterizedTest
    @MethodSource("existsSamples")
    void doesNotExistTest(AdminEventRepresentation event, boolean exists) {
        BaseMatcher<AdminEventRepresentation> matcher = AdminEventMatchers.doesNotExist();
        assertThat(matcher.matches(event), is(!exists));

        if (exists) {
            // Does not make sens to test describeTo when assertion matches
            checkDescription(matcher, event, "No event was expected but found ");
        }
    }

    private static Stream<Arguments> existsSamples() {
        AdminEventRepresentation anyEvent = createEvent(null, null);

        return Stream.of(
                Arguments.of(null, false),
                Arguments.of(anyEvent, true)
        );
    }

    private static AdminEventRepresentation createEvent(String realm, String error) {
        AdminEventRepresentation res = new AdminEventRepresentation();
        res.setRealmId(realm);
        res.setError(error);
        return res;
    }
}
