package io.cloudtrust.keycloak.test.matchers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.BaseMatcher;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;

import java.util.function.Function;
import java.util.function.Predicate;

// Use keycloak test framework with EventMatchers. EventMatchersLegacy will be removed when KeycloakDeploy won't be used anymore
public class EventMatchers extends AbstractMatchers<EventRepresentation> {
    protected static final String CT_EVENT_TYPE = "ct_event_type";

    protected EventMatchers(Predicate<EventRepresentation> predicate, Function<EventRepresentation, String> describer) {
        super(predicate, describer, false);
    }

    private EventMatchers(Predicate<EventRepresentation> predicate, Function<EventRepresentation, String> describer, boolean nullResponse) {
        super(predicate, describer, nullResponse);
    }

    @Override
    protected EventRepresentation convert(Object item) {
        return item instanceof EventRepresentation ? (EventRepresentation) item : null;
    }

    public static BaseMatcher<EventRepresentation> isRealm(String expectedRealm) {
        return new EventMatchers(
                e -> expectedRealm.equals(e.getRealmId()),
                e -> String.format("Event realm is %s when expected value is %s", e.getRealmId(), expectedRealm)
        );
    }

    public static BaseMatcher<EventRepresentation> isKeycloakType(EventType expectedType) {
        return new EventMatchers(
                e -> expectedType.name().equals(e.getType()),
                e -> String.format("Event type is %s when expected value is %s", e.getType(), expectedType.name())
        );
    }

    public static BaseMatcher<EventRepresentation> isCloudtrustType(String expectedType) {
        return new EventMatchers(
                e -> e.getDetails() != null && e.getDetails().containsKey(CT_EVENT_TYPE) && expectedType.equals(e.getDetails().get(CT_EVENT_TYPE)),
                e -> {
                    if (e.getDetails() == null || !e.getDetails().containsKey(CT_EVENT_TYPE)) {
                        return "Cloudtrust event type is missing in event details";
                    }
                    return String.format("Cloudtrust event type is %s when expected value is %s", e.getDetails().get(CT_EVENT_TYPE), expectedType);
                });
    }

    public static BaseMatcher<EventRepresentation> hasEmptyDetail(String detailName) {
        return new EventMatchers(
                e -> e.getDetails() != null && e.getDetails().containsKey(detailName) && StringUtils.isBlank(e.getDetails().get(detailName)),
                e -> {
                    if (e.getDetails() == null || !e.getDetails().containsKey(detailName)) {
                        return String.format("Event detail %s is missing or not empty in event details", detailName);
                    }
                    return String.format("Event detail %s is %s when empty value is expected", detailName, e.getDetails().get(detailName));
                });
    }

    public static BaseMatcher<EventRepresentation> hasDetail(String detailName, String expectedValue) {
        return new EventMatchers(
                e -> e.getDetails() != null && e.getDetails().containsKey(detailName) && expectedValue.equals(e.getDetails().get(detailName)),
                e -> {
                    if (e.getDetails() == null || !e.getDetails().containsKey(detailName)) {
                        return String.format("Event detail %s is missing in event details", detailName);
                    }
                    return String.format("Event detail %s is %s when expected value is %s", detailName, e.getDetails().get(detailName), expectedValue);
                });
    }

    public static BaseMatcher<EventRepresentation> hasNonEmptyDetail(String detailName) {
        return new EventMatchers(
                e -> e.getDetails() != null && e.getDetails().containsKey(detailName) && StringUtils.isNotBlank(e.getDetails().get(detailName)),
                e -> {
                    if (e.getDetails() == null || !e.getDetails().containsKey(detailName)) {
                        return String.format("Event detail %s is missing in event details", detailName);
                    }
                    return String.format("Event detail %s is missing, null or empty when it is supposed to be non empty", detailName);
                });
    }

    public static BaseMatcher<EventRepresentation> hasUserId() {
        return new EventMatchers(
                e -> e.getUserId() != null,
                e -> "Event type refers to no user when a user is expected"
        );
    }

    public static BaseMatcher<EventRepresentation> hasUserId(String userId) {
        return new EventMatchers(
                e -> userId != null && userId.equals(e.getUserId()),
                e -> String.format("Event type user is %s when user %s is expected", e.getUserId() == null ? "null" : e.getUserId(), userId)
        );
    }

    public static BaseMatcher<EventRepresentation> hasNoError() {
        return new EventMatchers(
                e -> StringUtils.isEmpty(e.getError()),
                e -> "Event type reports an error when no error is expected"
        );
    }

    public static BaseMatcher<EventRepresentation> hasError() {
        return new EventMatchers(
                e -> e.getError() != null,
                e -> "Event type reports no error when an error is expected"
        );
    }

    public static BaseMatcher<EventRepresentation> hasError(String error) {
        return new EventMatchers(
                e -> error != null && error.equals(e.getError()),
                e -> String.format("Event type error is %s when error '%s' is expected", e.getError() == null ? "null" : e.getError(), error)
        );
    }

    public static BaseMatcher<EventRepresentation> doesNotHaveError(String error) {
        return new EventMatchers(
                e -> error != null && !error.equals(e.getError()),
                e -> String.format("Event type error is %s but is supposed to be different", error)
        );
    }

    public static BaseMatcher<EventRepresentation> exists() {
        return new EventMatchers(e -> true, null);
    }

    public static BaseMatcher<EventRepresentation> doesNotExist() {
        return new EventMatchers(
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
