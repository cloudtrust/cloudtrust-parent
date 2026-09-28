package io.cloudtrust.keycloak.test.ctpages;

import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.openqa.selenium.WebDriver;

public class RegisterPage extends AbstractCtPage {

    public RegisterPage(ManagedWebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return null;
    }

    @Override
    public boolean isActivePage() {
        return getPageTitle().equals("Register");
    }
}
