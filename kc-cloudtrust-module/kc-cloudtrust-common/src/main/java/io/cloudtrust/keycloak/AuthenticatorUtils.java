package io.cloudtrust.keycloak;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.AuthenticationFlowContext;

import java.util.List;
import org.keycloak.http.HttpRequest;

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
        return params == null || params.isEmpty() ? null : params.get(0);
    }
}
