package io.cloudtrust.keycloak;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.http.HttpRequest;

import java.util.List;

public class RequiredActionUtils {
    private RequiredActionUtils() {
    }

    public static MultivaluedMap<String, String> getDecodedFormParameters(RequiredActionContext context) {
        HttpRequest httpReq = context.getHttpRequest();
        if (httpReq == null || "GET".equals(httpReq.getHttpMethod())) {
            return new MultivaluedHashMap<>();
        }
        return httpReq.getDecodedFormParameters();
    }

    public static List<String> getDecodedFormParameters(RequiredActionContext context, String paramName) {
        return getDecodedFormParameters(context).get(paramName);
    }

    public static String getFirstDecodedFormParameter(RequiredActionContext context, String paramName) {
        List<String> params = getDecodedFormParameters(context, paramName);
        return params == null || params.isEmpty() ? null : params.get(0);
    }
}
