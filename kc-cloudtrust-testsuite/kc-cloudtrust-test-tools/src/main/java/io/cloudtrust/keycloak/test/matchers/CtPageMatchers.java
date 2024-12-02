package io.cloudtrust.keycloak.test.matchers;

import io.cloudtrust.keycloak.test.ctpages.AbstractCtPage;
import org.hamcrest.BaseMatcher;

import java.util.function.Function;
import java.util.function.Predicate;

public class CtPageMatchers extends AbstractMatchers<AbstractCtPage> {
    protected CtPageMatchers(Predicate<AbstractCtPage> predicate, Function<AbstractCtPage, String> describer) {
        super(predicate, describer);
    }

    protected CtPageMatchers(Predicate<AbstractCtPage> predicate, Function<AbstractCtPage, String> describer, boolean nullResponse) {
        super(predicate, describer, nullResponse);
    }

    @Override
    protected AbstractCtPage convert(Object item) {
        return item instanceof AbstractCtPage res ? res : null;
    }

    public static BaseMatcher<AbstractCtPage> isCurrent() {
        return new CtPageMatchers(
                AbstractCtPage::isCurrent,
                p -> String.format("Current page is %s", p.getClass().getName())
        );
    }

    public static BaseMatcher<AbstractCtPage> isNotCurrent() {
        return new CtPageMatchers(
                p -> !p.isCurrent(),
                p -> String.format("Current page is %s", p.getClass().getName()),
                true
        );
    }

    public static BaseMatcher<AbstractCtPage> pageContains(String text) {
        return new CtPageMatchers(
                p -> p.pageContains(text),
                p -> String.format("Current page does not contain %s", text)
        );
    }
}
