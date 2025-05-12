package io.cloudtrust.keycloak.test.ctpages;

import org.openqa.selenium.WebDriver;

public class RegisterPage extends AbstractCtPage {
    public RegisterPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return null;
    }

    public boolean isCurrent() {
        return getPageTitle().equals("Register");
    }
}
