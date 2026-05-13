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

    /**
     * Use isActivePage instead
     * @return
     */
    @Deprecated
    public static BaseMatcher<AbstractCtPage> isCurrent() {
        return isActivePage();
    }

    public static BaseMatcher<AbstractCtPage> isActivePage() {
        return new CtPageMatchers(
                AbstractCtPage::isActivePage,
                p -> String.format("Current page is %s (expected %s for class %s)", p.getCurrentPageId(), p.getExpectedPageId(), p.getClass().getName())
        );
    }

    /**
     * Use isNotActivePage instead
     * @return
     */
    @Deprecated
    public static BaseMatcher<AbstractCtPage> isNotCurrent() {
        return isNotActivePage();
    }

    public static BaseMatcher<AbstractCtPage> isNotActivePage() {
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
