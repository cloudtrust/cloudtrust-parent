package io.cloudtrust.keycloak.test.ctpages;

import io.cloudtrust.keycloak.test.pages.AbstractPage;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginPasswordResetPage extends AbstractCtPage {
    @FindBy(id = "username")
    private WebElement usernameInput;

    @FindBy(id = "input-error-username")
    private WebElement usernameError;

    @FindBy(css = "[type=\"submit\"]")
    private WebElement submitButton;

    @FindBy(className = "alert-success")
    private WebElement emailSuccessMessage;

    @FindBy(className = "alert-error")
    private WebElement emailErrorMessage;

    @FindBy(partialLinkText = "Back to Login")
    private WebElement backToLogin;

    public LoginPasswordResetPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public String getExpectedPageId() {
        return null;
    }

    public void changePassword() {
        submitButton.click();
    }

    public void changePassword(String username) {
        usernameInput.clear();
        usernameInput.sendKeys(username);

        submitButton.click();
    }

    @Override
    public boolean isCurrent() {
        return getPageTitle(driver).equals("Forgot Your Password?");
    }

    public String getSuccessMessage() {
        return emailSuccessMessage != null ? emailSuccessMessage.getText() : null;
    }

    public String getUsernameError() {
        try {
            return getTextFromElement(usernameError);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getErrorMessage() {
        try {
            return getTextFromElement(emailErrorMessage);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getUsername() {
        return usernameInput.getAttribute("value");
    }

    public void backToLogin() {
        backToLogin.click();
    }
}
