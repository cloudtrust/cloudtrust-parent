package io.cloudtrust.keycloak.test.webdriver;

import org.keycloak.testframework.TestFrameworkExtension;
import org.keycloak.testframework.injection.Supplier;

import java.util.List;

/**
 * Registers the CI-friendly headless-Chrome WebDriver supplier ({@link CiChromeHeadlessWebDriverSupplier})
 * with the Keycloak test framework. The {@code WebDriver -> "browser"} value-type alias is already
 * contributed by the framework's own UI extension, so this extension only adds the extra supplier.
 */
public class CiUITestFrameworkExtension implements TestFrameworkExtension {

    @Override
    public List<Supplier<?, ?>> suppliers() {
        return List.of(new CiChromeHeadlessWebDriverSupplier());
    }
}
