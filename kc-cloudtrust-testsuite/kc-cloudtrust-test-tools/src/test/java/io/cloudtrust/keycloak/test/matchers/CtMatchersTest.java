package io.cloudtrust.keycloak.test.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

class CtMatchersTest {
    @Test
    void andNullListTest() {
        List<Integer> numbers = Arrays.asList(1, 3, 5, 7, 9);
        assertThat(numbers, CtMatchers.and());
    }

    @Test
    void andFailsTest() {
        List<Integer> numbers = Arrays.asList(1, 3, 5, 7, 9);

        assertThat(numbers, hasItems(1, 5, 7));
        assertThat(numbers, CtMatchers.and(hasItem(1), hasItem(5), hasItem(7)));

        BaseMatcher<Iterable<? super Integer>> matcher = CtMatchers.and(hasItem(1), hasItem(4), hasItem(7));
        assertThat(matcher.matches(numbers), is(false));
        matcher.describeTo(new Description.NullDescription()); // Pure coverage
    }

    @Test
    void inAnyOrderAssertThatSuccessTest() {
        Assertions.assertDoesNotThrow(
                () -> CtMatchers.inAnyOrderAssertThat(Arrays.asList(null, null, ""), nullValue(), nullValue(), notNullValue())
        );
    }

    @Test
    void inAnyOrderAssertThatDifferentSizeTest() {
        Assertions.assertThrows(AssertionFailedError.class,
                () -> CtMatchers.inAnyOrderAssertThat(Arrays.asList(null, null, ""), nullValue(), nullValue())
        );
    }

    @Test
    void inAnyOrderAssertThatFailureTest() {
        Assertions.assertThrows(AssertionFailedError.class,
                () -> CtMatchers.inAnyOrderAssertThat(Arrays.asList(null, ""), nullValue(), nullValue())
        );
    }
}