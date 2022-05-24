package io.cloudtrust.keycloak;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;

public class Events {
    public static final String CT_EVENT_TYPE = "ct_event_type";
    public static final String CT_EVENT_USERNAME = "username";
    public static final String CT_EVENT_STATUS = "status";
    public static final String CT_EVENT_CREDENTIAL_ID = "credential_id";
    public static final String CT_EVENT_REASON = "reason";
    public static final String CT_EVENT_ERROR = "error";

    private Events() {
        // Helper does not need to be instantiated
    }

    public static EventBuilder cloneEvent(EventBuilder event, String authType, UserModel user, String eventType, String credentialId) {
        return event.clone()
                .event(EventType.CUSTOM_REQUIRED_ACTION)
                .user(user)
                .detail(CT_EVENT_TYPE, eventType)
                .detail(Details.AUTH_TYPE, authType)
                .detail(CT_EVENT_USERNAME, user.getUsername())
                .detail(CT_EVENT_STATUS, String.valueOf(user.isEnabled()))
                .detail(CT_EVENT_CREDENTIAL_ID, credentialId);
    }

    public static EventBuilder cloneEvent(AuthenticationFlowContext context, String authType, UserModel user, String eventType, String credentialId) {
        return cloneEvent(context.getEvent(), authType, user, eventType, credentialId);
    }

    public static void cloneEventSuccess(EventBuilder event, String authType, UserModel user, String eventType, String credentialId) {
        cloneEvent(event, authType, user, eventType, credentialId).success();
    }

    public static void cloneEventSuccess(AuthenticationFlowContext context, String authType, UserModel user, String eventType, String credentialId) {
        cloneEvent(context.getEvent(), authType, user, eventType, credentialId).success();
    }

    /**
     * How awful! Module specific method should not be in this library.
     * @param context
     * @return
     */
    @Deprecated
    public static EventBuilder updateEvent(AuthenticationFlowContext context) {
        return context.getEvent()
                .user(context.getUser())
                .detail(Details.AUTH_TYPE, "OTP-Push");
    }
}