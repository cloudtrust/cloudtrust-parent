package io.cloudtrust.keycloak.test.webdriver;

import org.jboss.logging.Logger;
import org.keycloak.testframework.ui.webdriver.AbstractWebDriverSupplier;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

/**
 * Headless-Chrome WebDriver supplier for CI.
 *
 * <p>The stock {@code chrome-headless} supplier shipped with {@code keycloak-test-framework-ui} launches
 * Chrome without a {@code --user-data-dir}.
 *
 * <p>This supplier mirrors the stock options but adds a unique throw-away {@code --user-data-dir} plus
 * {@code --no-sandbox} (required when Chrome runs as root in the container). Selected via
 * {@code -Dkc.test.browser=chrome-headless-ci} in the surefire configuration.
 *
 * <p>Shared by every consumer module via {@code kc-cloudtrust-test-tools}, so the throw-away profile
 * directory can no longer be named after one specific project. Consumers should set the
 * {@value #PROFILE_PREFIX_PROPERTY} system property (e.g. to {@code ${project.artifactId}}, next to the
 * existing {@code kc.test.browser} entry in their surefire {@code systemPropertyVariables}). If it isn't set, the
 * prefix falls back to the name of the current working directory (Surefire forks with
 * {@code workingDirectory=${basedir}} by default, so this is normally the consumer module's own
 * directory name already), and finally to a generic constant if that can't be resolved either.
 */
public class CiChromeHeadlessWebDriverSupplier extends AbstractWebDriverSupplier {

    private static final Logger LOG = Logger.getLogger(CiChromeHeadlessWebDriverSupplier.class);

    /** System property consumers can set to name their Chrome profile-dir prefix. */
    public static final String PROFILE_PREFIX_PROPERTY = "kc.test.chrome.profile-prefix";

    private static final String GENERIC_PROFILE_PREFIX = "kc-test";

    @Override
    public String getAlias() {
        return "chrome-headless-ci";
    }

    @Override
    public WebDriver getWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.setCapability("pageLoadStrategy", PageLoadStrategy.NORMAL.toString());
        options.setCapability("timeouts", Map.of("implicit", Duration.ofSeconds(5).toMillis()));
        options.addArguments(
                "--headless",
                "--disable-gpu",
                "--window-size=1920,1200",
                "--ignore-certificate-errors",
                "--disable-dev-shm-usage",
                "--no-sandbox"
        );
        options.addArguments("--user-data-dir=" + createUniqueUserDataDir());
        return new ChromeDriver(options);
    }

    private static String createUniqueUserDataDir() {
        String prefix = profilePrefix() + "-chrome-";
        try {
            return Files.createTempDirectory(prefix).toAbsolutePath().toString();
        } catch (IOException e) {
            // Fall back to the system temp dir suffixed with the JVM identity; still unique enough to
            // avoid the default-profile collision that breaks the build.
            String fallback = System.getProperty("java.io.tmpdir") + "/" + prefix + System.identityHashCode(new Object());
            LOG.warnf(e, "Could not create temp user-data-dir, falling back to %s", fallback);
            return fallback;
        }
    }

    /**
     * Resolves the consumer-identifying prefix for the throw-away Chrome profile directory: the
     * explicit {@value #PROFILE_PREFIX_PROPERTY} system property if set, otherwise the name of the
     * current working directory (normally the consumer module's own basedir), otherwise a generic
     * constant.
     */
    private static String profilePrefix() {
        String explicit = System.getProperty(PROFILE_PREFIX_PROPERTY);
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        try {
            Path cwd = Paths.get(System.getProperty("user.dir", "")).toAbsolutePath();
            Path name = cwd.getFileName();
            if (name != null && !name.toString().isBlank()) {
                return name.toString();
            }
        } catch (RuntimeException e) {
            LOG.debugf(e, "Could not derive profile-dir prefix from user.dir, falling back to %s", GENERIC_PROFILE_PREFIX);
        }
        return GENERIC_PROFILE_PREFIX;
    }
}
