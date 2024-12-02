package io.cloudtrust.keycloak.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtrust.keycloak.test.util.JwtToolbox;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExtensionApi {
    private static final Logger LOG = Logger.getLogger(ExtensionApi.class);

    private final String keycloakURL;
    private final TokenProvider tokenProvider;
    private final ObjectMapper mapper = new ObjectMapper();
    private String token;

    public ExtensionApi(String keycloakURL, KeycloakClientProvider keycloakClientProvider) {
        this.keycloakURL = keycloakURL;
        this.tokenProvider = () -> keycloakClientProvider.getKeycloakAdminClient().tokenManager().getAccessTokenString();
    }

    public ExtensionApi(String keycloakURL, TokenProvider tokenProvider) {
        this.keycloakURL = keycloakURL;
        this.tokenProvider = tokenProvider;
    }

    public void initToken() {
        String accessToken = this.tokenProvider.getToken();
        this.setToken(accessToken);
    }

    public String getToken() {
        if (this.token == null) {
            initToken();
        }
        return this.token;
    }

    public void setToken(String accessToken) {
        this.token = accessToken;
    }

    public <T> T query(Class<T> clazz, String method, String apiPath) throws IOException, URISyntaxException {
        return mapper.readValue(call(method, apiPath, new ArrayList<>()), clazz);
    }

    public <T> T query(Class<T> clazz, String method, String apiPath, List<NameValuePair> nvps) throws IOException, URISyntaxException {
        return mapper.readValue(call(method, apiPath, nvps), clazz);
    }

    public <T> T query(TypeReference<T> typeRef, String method, String apiPath, List<NameValuePair> params) throws IOException, URISyntaxException {
        return mapper.readValue(call(method, apiPath, params), typeRef);
    }

    public String call(String apiPath) throws IOException, URISyntaxException {
        return call("GET", apiPath, new ArrayList<>());
    }

    public String call(String method, String apiPath) throws IOException, URISyntaxException {
        return call(method, apiPath, new ArrayList<>());
    }

    public String call(String method, String apiPath, List<NameValuePair> nvps) throws IOException, URISyntaxException {
        return call(method, apiPath, nvps, null);
    }

    public String callJSON(String method, String apiPath, Object jsonable) throws IOException, URISyntaxException {
        return callJSON(method, apiPath, new ArrayList<>(), jsonable);
    }

    public String callJSON(String method, String apiPath, List<NameValuePair> nvps, Object jsonable) throws IOException, URISyntaxException {
        String json = new ObjectMapper().writeValueAsString(jsonable);
        StringEntity requestEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
        return call(method, apiPath, nvps, requestEntity, new BasicHeader("Content-Type", "application/json"));
    }

    public String call(String method, String apiPath, List<NameValuePair> nvps, HttpEntity entity, Header... headers) throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            String uri = keycloakURL + apiPath;
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameters(nvps);
            URI builtUri = uriBuilder.build();
            HttpRequestBase req = createHttpRequest(method, builtUri, entity);
            if (LOG.isDebugEnabled()) {
                LOG.debugf("Calling API: %s %s", method, builtUri.toString());
                if (headers != null) {
                    Arrays.stream(headers).forEach(h -> LOG.infof("Header> %s %s", h.getName(), h.getValue()));
                }
                LOG.debugf("Auth> " + JwtToolbox.getPayload(getToken(), getToken()));
            }
            req.addHeader("Authorization", "Bearer " + getToken());
            if (headers != null) {
                Arrays.stream(headers).forEach(req::addHeader);
            }

            HttpResponse response = client.execute(req);
            if (response.getStatusLine().getStatusCode() / 100 != 2) {
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), "call to " + uri + " failed: " + response.getStatusLine().getStatusCode());
            }
            if (response.getEntity() != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                    return reader.lines().collect(Collectors.joining());
                }
            }
            return null;
        }
    }

    private HttpRequestBase createHttpRequest(String method, URI uri, HttpEntity entity) throws HttpResponseException {
        return switch (method) {
            case "GET" -> new HttpGet(uri);
            case "PUT" -> addBodyToHttpRequest(new HttpPut(uri), entity);
            case "POST" -> addBodyToHttpRequest(new HttpPost(uri), entity);
            default -> throw new HttpResponseException(405, "Unsupported method " + method);
        };
    }

    private HttpRequestBase addBodyToHttpRequest(HttpEntityEnclosingRequestBase httpRequest, HttpEntity entity) {
        if (entity != null) {
            httpRequest.setEntity(entity);
        }
        return httpRequest;
    }

    /**
     * We assume here that kc-cloudtrust-db-access has been loaded with Keycloak providers
     *
     * @param realm
     * @param userId
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public Map<String, List<String>> getUserAttributesFromDatabase(String realm, String userId) throws IOException, URISyntaxException {
        String path = String.format("/realms/%s/database-provider/users/%s/attributes", realm, userId);
        TypeReference<Map<String, List<String>>> typeRef = new TypeReference<Map<String, List<String>>>() {
        };
        return this.query(typeRef, "GET", path, new ArrayList<>());
    }

    /**
     * We assume here that kc-cloudtrust-db-access has been loaded with Keycloak providers
     *
     * @param realm
     * @param userId
     * @param attrbs
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public String setUserAttributesIntoDatabase(String realm, String userId, Map<String, List<String>> attrbs) throws IOException, URISyntaxException {
        String path = String.format("/realms/%s/database-provider/users/%s/attributes", realm, userId);
        return callJSON("POST", path, new ArrayList<>(), attrbs);
    }
}
