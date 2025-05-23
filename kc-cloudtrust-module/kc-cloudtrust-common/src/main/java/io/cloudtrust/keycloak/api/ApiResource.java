package io.cloudtrust.keycloak.api;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import org.jboss.logging.Logger;
import org.keycloak.http.HttpRequest;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AppAuthManager.BearerTokenAuthenticator;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

public class ApiResource {
    private static final Logger LOG = Logger.getLogger(ApiResource.class);

    protected KeycloakSession session;

    protected AppAuthManager authManager;

    public ApiResource(KeycloakSession session) {
        this.session = session;
        this.authManager = new AppAuthManager();
    }

    protected AdminAuth authenticateRealmAdminRequest() {
        return authenticateRealmAdminRequest(this.session.getContext().getRequestHeaders());
    }

    /**
     * Method copied/pasted from AdminRoot
     *
     * @param headers
     * @return
     */
    protected AdminAuth authenticateRealmAdminRequest(HttpHeaders headers) {
        String tokenString = AppAuthManager.extractAuthorizationHeaderToken(headers);
        if (tokenString == null) {
            throw new NotAuthorizedException("Bearer");
        }
        AccessToken token;
        try {
            JWSInput input = new JWSInput(tokenString);
            token = input.readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            throw new NotAuthorizedException("Bearer token format error");
        }
        String realmName = token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotAuthorizedException("Unknown realm in token");
        }

        session.getContext().setRealm(realm);
        BearerTokenAuthenticator bearerAuthenticator = new AppAuthManager.BearerTokenAuthenticator(session);
        AuthenticationManager.AuthResult authResult = bearerAuthenticator
                .setConnection(session.getContext().getConnection())
                .setHeaders(headers)
                .authenticate();
        if (authResult == null) {
            LOG.debug("Token not valid");
            throw new NotAuthorizedException("Bearer");
        }

        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        if (client == null) {
            throw new NotFoundException("Could not find client for authorization");
        }

        return new AdminAuth(realm, authResult.getToken(), authResult.getUser(), client);
    }

    protected AdminAuth auth() {
        HttpRequest request = session.getContext().getHttpRequest();
        AdminAuth auth = authenticateRealmAdminRequest(request.getHttpHeaders());
        if (auth == null) {
            throw new NotAuthorizedException("Can't get AdminAuth");
        }

        LOG.debugf("authenticated admin access for: %s", auth.getUser().getUsername());
        Cors.builder().allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").exposedHeaders("Location").auth().add();

        return auth;
    }

    protected String getPathParameter(String name) {
        return session.getContext().getUri().getPathParameters().getFirst(name);
    }

    protected RealmModel getRealm(AdminAuth auth, String realmName) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            LOG.infof("Can't find realm %s", realmName);
            throw new NotFoundException("notFound.realm");
        }

        if (!auth.getRealm().equals(realmManager.getKeycloakAdminstrationRealm()) && !auth.getRealm().equals(realm)) {
            throw new ForbiddenException();
        }

        return realm;
    }

    protected UserModel getUser(RealmModel realm, String userId, AdminAuth auth) {
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            LOG.infof("Can't find user %s", userId);
            throw new NotFoundException("notFound.user");
        }

        if (auth != null) {
            AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, auth);
            realmAuth.users().requireManage(user);
        }

        return user;
    }
}
