package io.cloudtrust.keycloak.test.matchers;

import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.EventRepresentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventMatcher extends BaseMatcher<EventRepresentation> {
	private static final String CT_EVENT_TYPE = "ct_event_type";

	private final Predicate<EventRepresentation> predicate;
	private final Function<EventRepresentation, String> describer;
	private final boolean nullResponse;
	private EventRepresentation event;

	private EventMatcher(Predicate<EventRepresentation> predicate, Function<EventRepresentation, String> describer) {
		this(predicate, describer, false);
	}

	private EventMatcher(Predicate<EventRepresentation> predicate, Function<EventRepresentation, String> describer, boolean nullResponse) {
		this.predicate = predicate;
		this.describer = describer;
		this.nullResponse = nullResponse;
	}

	@Override
	public boolean matches(Object item) {
		if (!(item instanceof EventRepresentation)) {
			return nullResponse;
		}
		this.event = (EventRepresentation)item;
		return this.predicate.test(this.event);
	}

	@Override
	public void describeTo(Description description) {
		if (this.event==null && !this.nullResponse) {
			description.appendText("Event is null");
		} else {
			description.appendText(describer.apply(this.event));
		}
	}


	public static BaseMatcher<EventRepresentation> isRealm(String expectedRealm) {
		return new EventMatcher(
				e -> expectedRealm.equals(e.getRealmId()),
				e -> "Event realm is "+e.getRealmId()+" when expected value is "+expectedRealm
				);
	}

	public static BaseMatcher<EventRepresentation> isKeycloakType(EventType expectedType) {
		return new EventMatcher(
				e -> expectedType.name().equals(e.getType()),
				e -> "Event type is "+e.getType()+" when expected value is "+expectedType.name()
				);
	}

	public static BaseMatcher<EventRepresentation> isCloudtrustType(String expectedType) {
		return new EventMatcher(
				e -> e.getDetails()!=null && e.getDetails().containsKey(CT_EVENT_TYPE) && expectedType.equals(e.getDetails().get(CT_EVENT_TYPE)),
				e -> {
					if (e.getDetails()==null || !e.getDetails().containsKey(CT_EVENT_TYPE)) {
						return "Cloudtrust event type is missing in event details";
					}
					return "Cloudtrust event type is "+e.getDetails().get("ct_event_type")+" when expected value is "+expectedType;
				});
	}

	public static BaseMatcher<EventRepresentation> hasDetail(String detailName, String expectedValue) {
		return new EventMatcher(
				e -> e.getDetails()!=null && e.getDetails().containsKey(detailName) && expectedValue.equals(e.getDetails().get(detailName)),
				e -> {
					if (e.getDetails()==null || !e.getDetails().containsKey(detailName)) {
						return "Event detail "+detailName+" is missing in event details";
					}
					return "Event detail "+detailName+" is "+e.getDetails().get(detailName)+" when expected value is "+expectedValue;
				});
	}

	public static BaseMatcher<EventRepresentation> hasNonEmptyDetail(String detailName) {
		return new EventMatcher(
				e -> e.getDetails()!=null && e.getDetails().containsKey(detailName) && StringUtils.isNotBlank(e.getDetails().get(detailName)),
				e -> {
					if (e.getDetails()==null || !e.getDetails().containsKey(detailName)) {
						return "Event detail "+detailName+" is missing in event details";
					}
					return "Event detail "+detailName+" is missing, null or empty when it is supposed to be non empty";
				});
	}

	public static BaseMatcher<EventRepresentation> hasNoError() {
		return new EventMatcher(
				e -> StringUtils.isEmpty(e.getError()),
				e -> "Event type reports an error when no error is expected"
				);
	}

	public static BaseMatcher<EventRepresentation> hasError() {
		return new EventMatcher(
				e -> e.getError()!=null,
				e -> "Event type reports no error when an error is expected"
				);
	}

	public static BaseMatcher<EventRepresentation> hasError(String error) {
		return new EventMatcher(
				e -> error!=null && error.equals(e.getError()),
				e -> "Event type reports no error when error '"+error+"' is expected"
				);
	}

	public static BaseMatcher<EventRepresentation> exists() {
		return new EventMatcher(
				e -> false,
				e -> {
					try {
						return "No event was expected but found "+new ObjectMapper().writeValueAsString(e);
					} catch (JsonProcessingException e1) {
						return "No event was expected but found one";
					}
				});
	}

	public static BaseMatcher<EventRepresentation> doesNotExist() {
		return new EventMatcher(
				e -> false,
				e -> {
					try {
						return "No event was expected but found "+new ObjectMapper().writeValueAsString(e);
					} catch (JsonProcessingException e1) {
						return "No event was expected but found one";
					}
				}, true);
	}
}
