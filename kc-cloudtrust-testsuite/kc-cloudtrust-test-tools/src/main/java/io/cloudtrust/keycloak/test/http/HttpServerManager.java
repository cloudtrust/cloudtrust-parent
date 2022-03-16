package io.cloudtrust.keycloak.test.http;

import io.cloudtrust.keycloak.test.util.ConsumerExcept;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HttpServerManager {
    private static final Logger LOG = Logger.getLogger(HttpServerManager.class);
    private static final HttpServerManager defaultInstance = new HttpServerManager();
    private static final int DEFAULT_LISTEN_PORT = 9995;

    private final Map<Integer, Undertow> httpServers = new ConcurrentHashMap<>();

    public static HttpServerManager getDefault() {
        return defaultInstance;
    }

    public Set<Integer> getActiveServerPorts() {
        return httpServers.keySet();
    }

    public void start(ConsumerExcept<HttpRequestProcessor, Exception> handler) {
        this.start(DEFAULT_LISTEN_PORT, handler);
    }

    public void start(int listenPort, ConsumerExcept<HttpRequestProcessor, Exception> handler) {
        HttpHandler effectiveHandler = exchange -> {
            try {
                handler.accept(new HttpRequestProcessorImpl(exchange));
            } catch (Exception e) {
                LOG.error("Failed to process HTTP request", e);
            }
        };
        startHttpServer(listenPort, effectiveHandler);
    }

    public void startHttpServer(HttpHandler handler) {
        this.startHttpServer(DEFAULT_LISTEN_PORT, handler);
    }

    public void startHttpServer(int listenPort, HttpHandler handler) {
        LOG.infof("Starting server listening on port %d...", listenPort);
        stop(listenPort);

        Undertow server = Undertow.builder()
                .addHttpListener(listenPort, "0.0.0.0")
                .setHandler(handler)
                .build();
        server.start();
        httpServers.put(listenPort, server);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(listenPort)));
        LOG.infof("Started listening on port %d", listenPort);
    }

    public void stop() {
        this.stop(DEFAULT_LISTEN_PORT);
    }

    public void stop(int listenPort) {
        httpServers.compute(listenPort, (port, server) -> {
            LOG.infof("Stopping server listening on port %d...", listenPort);
            if (server != null) {
                server.stop();
                LOG.infof("Server listening on port %d stopped", listenPort);
            } else {
                LOG.infof("No server found listening on port %d", listenPort);
            }
            return null;
        });
    }
}
