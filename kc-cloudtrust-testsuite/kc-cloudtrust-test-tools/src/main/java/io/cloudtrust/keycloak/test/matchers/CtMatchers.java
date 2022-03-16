package io.cloudtrust.keycloak.test.matchers;

import io.cloudtrust.keycloak.test.util.JsonToolbox;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Assertions;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class CtMatchers<T> extends BaseMatcher<T> {
    @SafeVarargs
    public static <T> BaseMatcher<T> and(Matcher<T>... list) {
        return new BaseMatcher<>() {
            private Matcher<T> failingMatcher = null;

            @Override
            public boolean matches(Object item) {
                if (list != null) {
                    for (Matcher<T> m : list) {
                        if (!m.matches(item)) {
                            this.failingMatcher = m;
                            return false;
                        }
                    }
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
                failingMatcher.describeTo(description);
            }
        };
    }

    @SafeVarargs
    public static <T> void inAnyOrderAssertThat(Collection<T> inputValues, Matcher<T>... matchers) {
        int inputSize = inputValues == null ? 0 : inputValues.size();
        int matchersSize = matchers == null ? 0 : matchers.length;
        if (inputSize != matchersSize) {
            Assertions.fail(String.format("inputValues and matchers have not the same size (%d/%d)", inputSize, matchersSize));
        }
        if (inputSize == 0) {
            return;
        }
        List<Matcher<T>> allMatchers = Stream.of(matchers).collect(Collectors.toList());
        inputValues.forEach(input -> {
                    for (int i = 0; i < allMatchers.size(); i++) {
                        if (allMatchers.get(i).matches(input)) {
                            allMatchers.remove(i);
                            return;
                        }
                    }
                    Assertions.fail(String.format("Input %s has no matching matcher", JsonToolbox.toString(input)));
                }
        );
    }
}
