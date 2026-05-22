package io.cloudtrust.keycloak.test.matchers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.BaseMatcher;
import org.keycloak.events.admin.OperationType;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.testframework.realm.ManagedRealm;

import java.util.function.Function;
import java.util.function.Predicate;

// Use keycloak test framework with AdminEventMatchers. AdminEventMatchersLegacy will be removed when KeycloakDeploy won't be used anymore
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

    public static BaseMatcher<AdminEventRepresentation> isRealm(ManagedRealm expectedRealm) {
        return isRealm(expectedRealm.getId());
    }

    public static BaseMatcher<AdminEventRepresentation> isRealm(String expectedRealm) {
        return new AdminEventMatchers(
                e -> expectedRealm.equals(e.getRealmId()),
                e -> String.format("Event realm is %s when expected value is %s", e.getRealmId(), expectedRealm)
        );
    }

    public static BaseMatcher<AdminEventRepresentation> isOperationType(OperationType operationType) {
        return isOperationType(operationType.toString());
    }

    public static BaseMatcher<AdminEventRepresentation> isOperationType(String operationType) {
        return new AdminEventMatchers(
                e -> e.getOperationType()!=null && operationType.equals(e.getOperationType()),
                e -> String.format("Operation type is %s when %s is expected", e.getOperationType(), operationType)
        );
    }

    public static BaseMatcher<AdminEventRepresentation> isAuthDetailsUserId(String userId) {
        return new AdminEventMatchers(
                e-> e.getAuthDetails()!=null && userId.equals(e.getAuthDetails().getUserId()),
                null
        );
    }

    public static BaseMatcher<AdminEventRepresentation> isResourcePath(String path) {
        return new AdminEventMatchers(
                e-> path.equals(e.getResourcePath()),
                e -> String.format("Resource path is %s when %s is expected", path, e.getResourcePath())
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
