package io.cloudtrust.keycloak.test.pages;

import org.openqa.selenium.WebDriver;

public class Browser extends AbstractPage {
    public WebDriver getDriver() {
        return driver;
    }

    @Override
    public boolean isCurrent() {
        throw new IllegalStateException("Shoul not be called on Browser");
    }

    @Override
    public void open() {
        throw new IllegalStateException("Shoul not be called on Browser");
    }
}
