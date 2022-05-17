package io.cloudtrust.keycloak.test.container;

import io.cloudtrust.exception.CloudtrustRuntimeException;
import io.cloudtrust.keycloak.test.util.NopX509TrustManager;
import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public class KeycloakQuarkusContainer {
    private static final Logger log = Logger.getLogger(KeycloakQuarkusContainer.class);

    private static KeycloakQuarkusContainer singleton = null;

    private final KeycloakQuarkusConfiguration configuration;
    private Process container;
    private KeycloakQuarkusOutput stdOutput;
    private KeycloakQuarkusOutput errOutput;
    private Keycloak adminCli;

    public static KeycloakQuarkusContainer start(KeycloakQuarkusConfiguration configuration) {
        if (singleton == null) {
            singleton = new KeycloakQuarkusContainer(configuration);
        }
        return singleton;
    }

    private KeycloakQuarkusContainer(KeycloakQuarkusConfiguration configuration) {
        if (configuration.getKeycloakStartException() != null) {
            // This configuration already made failing Keycloak
            throw new CloudtrustRuntimeException(configuration.getKeycloakStartException());
        }
        try {
            this.configuration = configuration;
            log.infof("Starting Keycloak container. Home folder is: " + configuration.getKeycloakPath());
            applyConfiguration();
            buildKeycloak();
            sleep(2000);
            runKeycloak();
        } catch (Exception e) {
            log.error("Can't start Keycloak", e);
            configuration.setKeycloakStartException(e);
            throw new CloudtrustRuntimeException(e);
        }
    }

    public String getBaseUrl() {
        return this.configuration.getBaseUrl();
    }

    private void applyConfiguration() throws IOException {
        // Modules
        if (!this.configuration.getModuleJarFiles().isEmpty()) {
            for (File moduleJar : this.configuration.getModuleJarFiles()) {
                this.installModule(moduleJar);
            }
            for (String module : this.configuration.resolve("providers").toFile().list()) {
                log.info("providers> " + module);
            }
        }
        // Classes
        this.installClasspath(this.configuration.getClasspath());
        // Properties
        String now = DateFormat.getDateTimeInstance().format(new Date());
        StringBuilder builder = new StringBuilder("##\n## Added by KeycloakQuarkusContainer on " + now + "\n##\n");
        for (Entry<String, String> entry : this.configuration.getProperties().entrySet()) {
            log.infof("add property> %s=%s", entry.getKey(), entry.getValue());
            builder.append('\n').append(entry.getKey()).append('=').append(entry.getValue());
        }
        // check if bindHttpPort is not 8080 while passing arguments makes the process fail
        if (configuration.getBindHttpPort() != null && configuration.getBindHttpPort() != 8080) {
            builder.append("\nhttp-enabled=true\nhttp-port=").append(configuration.getBindHttpPort());
        }
        if (configuration.getBindHttpsPort() != null) {
            builder.append("\nhttps-port=").append(configuration.getBindHttpsPort());
        }
        if (builder.length() > 0) {
            try (FileWriter fw = new FileWriter(this.configuration.resolve("conf").resolve("keycloak.conf").toFile(), false)) {
                fw.write(builder.toString());
            }
        }
    }

    private void installModule(File jarFile) throws IOException {
        Path providerDir = this.configuration.resolve("providers").resolve(jarFile.getName());
        Files.copy(jarFile.toPath(), providerDir, StandardCopyOption.REPLACE_EXISTING);
    }

    private void installClasspath(Collection<File> filenames) {
        //Path importDir = this.configuration.resolve("lib").resolve("app");
        Path importDir = this.configuration.resolve("lib").resolve("lib").resolve("main");
        filenames.forEach(f -> {
            try {
                Files.copy(f.toPath(), importDir.resolve(f.getName()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new CloudtrustRuntimeException(e);
            }
        });
    }

    private void buildKeycloak() {
        runProcess(getProcessBuildCommands(), configuration.isCaptureOutput(), () -> false);
    }

    private void runKeycloak() {
        if (!runProcess(getProcessStartCommands(), configuration.isCaptureOutput(), this::isKeycloakRunnning)) {
            throw new CloudtrustRuntimeException("Could not start Keycloak");
        }
    }

    private boolean runProcess(String[] args, boolean captureOutput, BooleanSupplier endCondition) {
        File wrkDir = configuration.resolve("bin").toFile();
        ProcessBuilder builder = new ProcessBuilder(args).directory(wrkDir);
        if (!captureOutput) {
            builder.inheritIO();
            // no need to redirect input
            //.redirectOutput(Redirect.INHERIT)
            //.redirectError(Redirect.INHERIT);
        }

        Map<String, String> env = builder.environment();
        env.put("KEYCLOAK_ADMIN", configuration.getAdminUsername());
        env.put("KEYCLOAK_ADMIN_PASSWORD", configuration.getAdminSecurity());
        configuration.getEnvironment().forEach((k, v) -> log.debugf("env> %s=%s", k, v));
        env.putAll(configuration.getEnvironment());

        try {
            container = builder.start();
            if (captureOutput) {
                stdOutput = new KeycloakQuarkusOutput(container.getInputStream());
                errOutput = new KeycloakQuarkusOutput(container.getErrorStream());
            }
            return waitProcess(endCondition);
        } catch (IOException ioe) {
            return false;
        }
    }

    public void stop() {
        if (this.container != null) {
            synchronized (this) {
                if (this.container != null) {
                    log.info("stop()");
                    container.children().forEach(ProcessHandle::destroy);
                    this.container.destroy();
                    boolean force;
                    try {
                        force = !this.container.waitFor(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        force = true;
                    }
                    if (force) {
                        log.info("Force Keycloak to stop immediately");
                        container.children().forEach(ProcessHandle::destroyForcibly);
                        this.container.destroyForcibly();
                    } else {
                        log.info("Keycloak stopped");
                    }
                    this.container = null;
                }
            }
        }
    }

    private List<String> getProcessCommandWithoutArguments() {
        List<String> commands = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            commands.addAll(Arrays.asList("cmd /C kc.bat".split(" ")));
        } else {
            commands.add("./kc.sh");
        }
        return commands;
    }

    private String[] getProcessBuildCommands() {
        List<String> commands = getProcessCommandWithoutArguments();
        commands.add("build");
        commands.addAll(this.configuration.getBuildArguments());
        log.info("Container args: " + commands);
        return commands.toArray(new String[0]);
    }

    private String[] getProcessStartCommands() {
        List<String> commands = getProcessCommandWithoutArguments();

        commands.add("start-dev");
        commands.addAll(this.configuration.getExecArguments());

        log.info("Container args: " + commands);
        return commands.toArray(new String[0]);
    }

    private boolean waitProcess(BooleanSupplier endCondition) {
        long startTime = System.currentTimeMillis();

        while (true) {
            if (System.currentTimeMillis() - startTime > this.configuration.getStartTimeout()) {
                stop();
                log.error("Timeout [" + this.configuration.getStartTimeout() + "] while waiting for Quarkus server");
                return false;
            }

            if (!this.container.isAlive()) {
                log.warn("Process stopped");
                return false;
            }

            try {
                // wait before checking for opening a new connection
                sleep(1000);
                if (endCondition.getAsBoolean()) {
                    break;
                }
            } catch (Exception ignore) {
                // Ignore
            }
        }
        return true;
    }

    private void sleep(int millis) throws IOException, InterruptedException {
        long limit = System.currentTimeMillis() + millis;
        for (; ; ) {
            long delay = Math.min(limit - System.currentTimeMillis(), 100);
            if (delay < 0) {
                return;
            }
            if (this.errOutput != null) {
                this.errOutput.getConsoleOutput();
            }
            if (this.stdOutput != null) {
                this.stdOutput.getConsoleOutput();
            }
            Thread.sleep(delay);
        }
    }

    private boolean isKeycloakRunnning() {
        try {
            URL contextRoot = new URL(this.configuration.getBaseUrl() + "/realms/master/");
            HttpURLConnection connection = createConnection(contextRoot);

            connection.setReadTimeout((int) this.configuration.getStartTimeout());
            connection.setConnectTimeout((int) this.configuration.getStartTimeout());
            connection.connect();

            if (connection.getResponseCode() == 200) {
                log.infof("Keycloak is ready at %s", contextRoot);
                return true;
            }

            connection.disconnect();
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    private HttpURLConnection createConnection(URL contextRoot) throws IOException {
        if (!"https".equals(contextRoot.getProtocol())) {
            return (HttpURLConnection) contextRoot.openConnection();
        }
        HttpURLConnection connection = (HttpURLConnection) contextRoot.openConnection();
        HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
        httpsConnection.setSSLSocketFactory(NopX509TrustManager.createInsecureSslSocketFactory());
        httpsConnection.setHostnameVerifier((s, sslSession) -> true);
        return connection;
    }

    public Keycloak getKeycloakAdminClient() {
        if (this.adminCli == null) {
            String adminCliUrl = this.configuration.getBaseUrl();
            log.info("Creating Keycloak admin client using base URL " + adminCliUrl);
            this.adminCli = Keycloak.getInstance(adminCliUrl, configuration.getAdminRealm(), configuration.getAdminUsername(), configuration.getAdminSecurity(),
                    configuration.getAdminClientId());
        }
        return this.adminCli;
    }
}
