package io.cloudtrust.keycloak.test.util;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.util.BasicAuthHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OidcTokenProvider {
    private String keycloakURL;
    private String oidcAuthPath;
    private String basicAuth;

    public OidcTokenProvider(String keycloakURL, String oidcAuthPath, String username, String password) {
        this.keycloakURL = keycloakURL;
        this.oidcAuthPath = oidcAuthPath;
        this.basicAuth = BasicAuthHelper.createHeader(username, password);
    }

    public HttpResponse createOidcToken(String username, String password, String... paramPairs) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost httpPost = new HttpPost(keycloakURL + oidcAuthPath);
            httpPost.addHeader("Authorization", basicAuth);

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("grant_type", "password"));
            params.add(new BasicNameValuePair("scope", "openid"));
            params.add(new BasicNameValuePair("username", username));
            params.add(new BasicNameValuePair("password", password));
            if (paramPairs != null) {
                for (int i = 0; i < paramPairs.length - 1; i += 2) {
                    params.add(new BasicNameValuePair(paramPairs[i], paramPairs[i + 1]));
                }
            }
            httpPost.setEntity(new UrlEncodedFormEntity(params));

            // call the OIDC interface
            return httpClient.execute(httpPost);
        }
    }
}
