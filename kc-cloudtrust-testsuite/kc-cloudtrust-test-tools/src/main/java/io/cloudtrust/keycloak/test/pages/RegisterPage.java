package io.cloudtrust.keycloak.test.pages;

public class RegisterPage extends AbstractPage {
    public boolean isCurrent() {
        return getPageTitle().equals("Register");
    }
}
