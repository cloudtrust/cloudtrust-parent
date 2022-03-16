package io.cloudtrust.keycloak.test.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractMatchers<T> extends BaseMatcher<T> {
    private final Predicate<T> predicate;
    private final Function<T, String> describer;
    private final boolean nullResponse;
    private T item;

    protected AbstractMatchers(Predicate<T> predicate, Function<T, String> describer) {
        this(predicate, describer, false);
    }

    protected AbstractMatchers(Predicate<T> predicate, Function<T, String> describer, boolean nullResponse) {
        this.predicate = predicate;
        this.describer = describer;
        this.nullResponse = nullResponse;
    }

    protected abstract T convert(Object item);

    @Override
    public boolean matches(Object item) {
        this.item = convert(item);
        if (this.item == null) {
            return nullResponse;
        }
        return this.predicate.test(this.item);
    }

    @Override
    public void describeTo(Description description) {
        if (this.item == null && !this.nullResponse) {
            description.appendText("Input is null");
        } else {
            description.appendText(describer.apply(this.item));
        }
    }
}
