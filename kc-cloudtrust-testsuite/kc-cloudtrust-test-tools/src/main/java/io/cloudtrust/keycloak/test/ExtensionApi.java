package io.cloudtrust.keycloak.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.keycloak.admin.client.Keycloak;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ExtensionApi {
    private final String keycloakURL;
    private final Keycloak keycloakAdminClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private String token;

    public ExtensionApi(String keycloakURL, Keycloak keycloakAdminClient) {
        this.keycloakURL = keycloakURL;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public void initToken() {
        String accessToken = this.keycloakAdminClient.tokenManager().getAccessTokenString();
        this.setToken(accessToken);
    }

    public String getToken() {
        assertThat(this.token, is(notNullValue()));
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
        return call(method, apiPath, nvps, requestEntity);
    }

    public String call(String method, String apiPath, List<NameValuePair> nvps, HttpEntity entity) throws IOException, URISyntaxException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            String uri = keycloakURL + apiPath;
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameters(nvps);
            HttpRequestBase get = createHttpRequest(method, uriBuilder.build(), entity);
            get.addHeader("Authorization", "Bearer " + getToken());

            HttpResponse response = client.execute(get);
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
        switch (method) {
            case "GET":
                return new HttpGet(uri);
            case "PUT":
                return addBodyToHttpRequest(new HttpPut(uri), entity);
            default:
                throw new HttpResponseException(405, "Unsupported method " + method);
        }
    }

    private HttpRequestBase addBodyToHttpRequest(HttpEntityEnclosingRequestBase httpRequest, HttpEntity entity) {
        if (entity != null) {
            httpRequest.setEntity(entity);
        }
        return httpRequest;
    }
}
