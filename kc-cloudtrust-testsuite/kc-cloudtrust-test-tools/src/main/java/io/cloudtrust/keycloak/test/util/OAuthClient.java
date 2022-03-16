package io.cloudtrust.keycloak.test.util;

import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;

import javax.ws.rs.core.UriBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OAuthClient {
    private static final String POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";
    private static final String ID_TOKEN_HINT = "id_token_hint";
    private static final String INITIATING_IDP_PARAM = "initiating_idp";

    private final String baseUrl;
    private String realm = "test";
    private String clientId = "test-app"; // ex: account-console
    private String redirectUri; // http://keycloak.local:8080/realms/test/account
    private String state = UUID.randomUUID().toString();
    private String scope = "openid";
    private String maxAge;
    private String responseType = OAuth2Constants.CODE;
    private String responseMode; // ex: fragment
    private String nonce;
    private String codeChallenge; // ex: Y7vZn1cm4FoNCmrfDYW-OONnF5-Xv3oF9evFj0THDVk
    private String codeChallengeMethod; // ex: S256
    private final Map<String, String> additionalParameters = new HashMap<>();
    private String postLogoutRedirectUri;
    private String idTokenHint;
    private String initiatingIDP;

    public OAuthClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.redirectUri = baseUrl + "/home";
    }

    public String getAuthServerRoot() {
        // Don't append /auth ... Will need to add it if configuration requires it.
        return this.baseUrl;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public String getIdTokenHint() {
        return idTokenHint;
    }

    public void setIdTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
    }

    public String getInitiatingIDP() {
        return initiatingIDP;
    }

    public void setInitiatingIDP(String initiatingIDP) {
        this.initiatingIDP = initiatingIDP;
    }

    public String getLoginFormUrl() {
        //http://keycloak.local:8080/realms/test/protocol/openid-connect/auth
        UriBuilder b = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(baseUrl));
        whenNotNull(responseType, () -> b.queryParam(OAuth2Constants.RESPONSE_TYPE, responseType));
        whenNotNull(responseMode, () -> b.queryParam(OIDCLoginProtocol.RESPONSE_MODE_PARAM, responseMode));
        whenNotNull(clientId, () -> b.queryParam(OAuth2Constants.CLIENT_ID, clientId));
        whenNotNull(redirectUri, () -> b.queryParam(OAuth2Constants.REDIRECT_URI, redirectUri));
        whenNotNull(state, () -> b.queryParam(OAuth2Constants.STATE, state));
        whenNotNull(nonce, () -> b.queryParam(OIDCLoginProtocol.NONCE_PARAM, nonce));
        whenNotNull(scope, () -> b.queryParam(OAuth2Constants.SCOPE, scope));
        whenNotNull(maxAge, () -> b.queryParam(OIDCLoginProtocol.MAX_AGE_PARAM, maxAge));
        // https://tools.ietf.org/html/rfc7636#section-4.3
        whenNotNull(codeChallenge, () -> b.queryParam(OAuth2Constants.CODE_CHALLENGE, codeChallenge));
        whenNotNull(codeChallengeMethod, () -> b.queryParam(OAuth2Constants.CODE_CHALLENGE_METHOD, codeChallengeMethod));
        additionalParameters.forEach(b::queryParam);

        return b.build(realm).toString();
    }

    public void openLoginForm() {
        WebDriverFactory.provide().navigate().to(getLoginFormUrl());
    }

    public String getLogoutFormUrl() {
        UriBuilder b = OIDCLoginProtocolService.logoutUrl(UriBuilder.fromUri(baseUrl));
        whenNotNull(this.postLogoutRedirectUri, () -> b.queryParam(POST_LOGOUT_REDIRECT_URI, getPostLogoutRedirectUri()));
        whenNotNull(this.idTokenHint, () -> b.queryParam(ID_TOKEN_HINT, getIdTokenHint()));
        whenNotNull(this.initiatingIDP, () -> b.queryParam(INITIATING_IDP_PARAM, getInitiatingIDP()));

        return b.build(realm).toString();
    }

    public void openLogout() {
        WebDriverFactory.provide().navigate().to(getLogoutFormUrl());
    }

    private <T> void whenNotNull(T value, Runnable runnable) {
        if (value != null) {
            runnable.run();
        }
    }

    public OAuthClient clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public OAuthClient redirectUri(String redirectUri) {
        if (redirectUri.startsWith("/")) {
            this.redirectUri = this.baseUrl + redirectUri;
        } else {
            this.redirectUri = redirectUri;
        }
        return this;
    }
}
