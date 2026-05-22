package io.cloudtrust.keycloak.test.util;

import org.jboss.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class NopX509TrustManager implements X509TrustManager {
    private static final Logger log = Logger.getLogger(NopX509TrustManager.class);

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        // Nop
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
        // Nop
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        // Nop
        return null;
    }

    public static SSLSocketFactory createInsecureSslSocketFactory() throws IOException {
        log.info("createInsecureSslSocketFactory()");
        TrustManager[] trustAllCerts = new TrustManager[]{new NopX509TrustManager()};
        SSLContext sslContext;
        SSLSocketFactory socketFactory;

        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            socketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException("Can't create unsecure trust manager");
        }
        return socketFactory;
    }
}
