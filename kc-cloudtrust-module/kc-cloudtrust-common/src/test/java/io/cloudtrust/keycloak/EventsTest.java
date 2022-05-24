package io.cloudtrust.keycloak;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.mockito.Mockito;

import java.util.stream.Stream;

class EventsTest {
    @Test
    void basicTestFromContext() {
        String realmId = "realm-id";
        String ipAddr = "127.0.0.1";
        String newAuthType = "new-auth-type";
        String newUsername = "new-username";
        String newCtEventType = "new-event-type";
        String newCredentialId = "new-cred-id";

        AuthenticationFlowContext ctx = Mockito.mock(AuthenticationFlowContext.class);
        RealmModel realm = Mockito.mock(RealmModel.class);
        KeycloakSession kcSession = Mockito.mock(KeycloakSession.class);
        ClientConnection clientConnection = Mockito.mock(ClientConnection.class);

        Mockito.when(realm.getId()).thenReturn(realmId);
        Mockito.when(realm.isEventsEnabled()).thenReturn(false);
        Mockito.when(realm.getEventsListenersStream()).thenReturn(Stream.of());
        Mockito.when(clientConnection.getRemoteAddr()).thenReturn(ipAddr);

        EventBuilder event = new EventBuilder(realm, kcSession, clientConnection);
        Mockito.when(ctx.getEvent()).thenReturn(event);

        UserModel newUser = Mockito.mock(UserModel.class);
        Mockito.when(newUser.isEnabled()).thenReturn(true);
        Mockito.when(newUser.getUsername()).thenReturn(newUsername);

        EventBuilder cloned = Events.cloneEvent(ctx, newAuthType, newUser, newCtEventType, newCredentialId);
        Assertions.assertNotEquals(event.getEvent().getType(), cloned.getEvent().getType());
        Assertions.assertEquals(EventType.CUSTOM_REQUIRED_ACTION, cloned.getEvent().getType());
        Assertions.assertEquals(newCtEventType, cloned.getEvent().getDetails().get(Events.CT_EVENT_TYPE));
        Assertions.assertEquals(newAuthType, cloned.getEvent().getDetails().get(Details.AUTH_TYPE));
        Assertions.assertEquals(newUsername, cloned.getEvent().getDetails().get(Events.CT_EVENT_USERNAME));
    }
}