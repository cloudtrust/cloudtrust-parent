package io.cloudtrust.keycloak;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.credential.CredentialModel;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.Objects;

public class AuthenticatorUtils {
    private AuthenticatorUtils() {
    }

    public static MultivaluedMap<String, String> getDecodedFormParameters(AuthenticationFlowContext context) {
        HttpRequest httpReq = context.getHttpRequest();
        if (httpReq == null || "GET".equals(httpReq.getHttpMethod())) {
            return new MultivaluedHashMap<>();
        }
        return httpReq.getDecodedFormParameters();
    }

    public static List<String> getDecodedFormParameters(AuthenticationFlowContext context, String paramName) {
        return getDecodedFormParameters(context).get(paramName);
    }

    public static String getFirstDecodedFormParameter(AuthenticationFlowContext context, String paramName) {
        List<String> params = getDecodedFormParameters(context, paramName);
        return params == null || params.isEmpty() ? null : params.getFirst();
    }

    public static String createUniqueCredentialLabel(UserModel user, String credentialName) {
        List<String> existingCredentialLabels = user.credentialManager().getStoredCredentialsStream()
                .map(CredentialModel::getUserLabel)
                .filter(Objects::nonNull)
                .toList();

        if (credentialName == null) {
            return null;
        }

        String candidate = credentialName;
        int suffix = 1;
        while (existingCredentialLabels.contains(candidate)) {
            candidate = credentialName + " (" + suffix + ")";
            suffix++;
        }
        return candidate;
    }
}
