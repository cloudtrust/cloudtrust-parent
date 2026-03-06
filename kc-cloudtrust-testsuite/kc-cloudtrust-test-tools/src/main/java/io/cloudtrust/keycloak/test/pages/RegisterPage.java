package io.cloudtrust.keycloak.test.pages;

@Deprecated
public class RegisterPage extends AbstractPage {
    public boolean isCurrent() {
        return getPageTitle().equals("Register");
    }
}
