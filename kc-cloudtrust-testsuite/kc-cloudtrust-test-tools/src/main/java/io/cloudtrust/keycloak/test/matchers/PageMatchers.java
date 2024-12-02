package io.cloudtrust.keycloak.test.matchers;

import io.cloudtrust.keycloak.test.pages.AbstractPage;
import org.hamcrest.BaseMatcher;

import java.util.function.Function;
import java.util.function.Predicate;

public class PageMatchers extends AbstractMatchers<AbstractPage> {
    protected PageMatchers(Predicate<AbstractPage> predicate, Function<AbstractPage, String> describer) {
        super(predicate, describer);
    }

    protected PageMatchers(Predicate<AbstractPage> predicate, Function<AbstractPage, String> describer, boolean nullResponse) {
        super(predicate, describer, nullResponse);
    }

    @Override
    protected AbstractPage convert(Object item) {
        return item instanceof AbstractPage res ? res : null;
    }

    public static BaseMatcher<AbstractPage> isCurrent() {
        return new PageMatchers(
                AbstractPage::isCurrent,
                p -> String.format("Current page is %s", p.getClass().getName())
        );
    }

    public static BaseMatcher<AbstractPage> isNotCurrent() {
        return new PageMatchers(
                p -> !p.isCurrent(),
                p -> String.format("Current page is %s", p.getClass().getName()),
                true
        );
    }

    public static BaseMatcher<AbstractPage> pageContains(String text) {
        return new PageMatchers(
                p -> p.pageContains(text),
                p -> String.format("Current page does not contain %s", text)
        );
    }
}
