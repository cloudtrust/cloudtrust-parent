package io.cloudtrust.keycloak.test.matchers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.BaseMatcher;
import org.keycloak.representations.idm.AdminEventRepresentation;

import java.util.function.Function;
import java.util.function.Predicate;

public class AdminEventMatchers extends AbstractMatchers<AdminEventRepresentation> {
    protected static final String CT_EVENT_TYPE = "ct_event_type";

    private AdminEventMatchers(Predicate<AdminEventRepresentation> predicate, Function<AdminEventRepresentation, String> describer) {
        super(predicate, describer, false);
    }

    private AdminEventMatchers(Predicate<AdminEventRepresentation> predicate, Function<AdminEventRepresentation, String> describer, boolean nullResponse) {
        super(predicate, describer, nullResponse);
    }

    @Override
    protected AdminEventRepresentation convert(Object item) {
        return item instanceof AdminEventRepresentation ? (AdminEventRepresentation) item : null;
    }

    public static BaseMatcher<AdminEventRepresentation> isRealm(String expectedRealm) {
        return new AdminEventMatchers(
                e -> expectedRealm.equals(e.getRealmId()),
                e -> String.format("Event realm is %s when expected value is %s", e.getRealmId(), expectedRealm)
        );
    }

    public static BaseMatcher<AdminEventRepresentation> hasNoError() {
        return new AdminEventMatchers(
                e -> StringUtils.isEmpty(e.getError()),
                e -> "Event type reports an error when no error is expected"
        );
    }

    public static BaseMatcher<AdminEventRepresentation> hasError() {
        return new AdminEventMatchers(
                e -> e.getError() != null,
                e -> "Event type reports no error when an error is expected"
        );
    }

    public static BaseMatcher<AdminEventRepresentation> hasError(String error) {
        return new AdminEventMatchers(
                e -> error != null && error.equals(e.getError()),
                e -> String.format("Event type reports no error when error '%s' is expected", error)
        );
    }

    public static BaseMatcher<AdminEventRepresentation> exists() {
        return new AdminEventMatchers(e -> true, null);
    }

    public static BaseMatcher<AdminEventRepresentation> doesNotExist() {
        return new AdminEventMatchers(
                e -> false,
                e -> {
                    try {
                        return "No event was expected but found " + new ObjectMapper().writeValueAsString(e);
                    } catch (JsonProcessingException e1) {
                        return "No event was expected but found one";
                    }
                }, true);
    }
}
