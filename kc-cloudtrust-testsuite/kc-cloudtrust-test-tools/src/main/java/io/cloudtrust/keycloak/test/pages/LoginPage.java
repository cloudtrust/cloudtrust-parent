package io.cloudtrust.keycloak.test.pages;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginPage extends LanguageComboboxAwarePage {

    @FindBy(id = "username")
    protected WebElement usernameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "input-error")
    private WebElement inputError;

    @FindBy(id = "totp")
    private WebElement totp;

    @FindBy(id = "rememberMe")
    private WebElement rememberMe;

    @FindBy(name = "login")
    protected WebElement submitButton;

    @FindBy(name = "cancel")
    private WebElement cancelButton;

    @FindBy(linkText = "Register")
    private WebElement registerLink;

    @FindBy(linkText = "Forgot Password?")
    private WebElement resetPasswordLink;

    @FindBy(linkText = "Username")
    private WebElement recoverUsernameLink;

    @FindBy(className = "alert-error")
    private WebElement loginErrorMessage;

    @FindBy(className = "alert-warning")
    private WebElement loginWarningMessage;

    @FindBy(className = "alert-success")
    private WebElement loginSuccessMessage;


    @FindBy(className = "alert-info")
    private WebElement loginInfoMessage;

    @FindBy(className = "instruction")
    private WebElement instruction;


    public void login(String username, String password) {
        usernameInput.clear();
        usernameInput.sendKeys(username);

        passwordInput.clear();
        passwordInput.sendKeys(password);

        clickLink(submitButton);
    }

    public void login(String password) {
        passwordInput.clear();
        passwordInput.sendKeys(password);

        clickLink(submitButton);
    }

    public void missingPassword(String username) {
        usernameInput.clear();
        usernameInput.sendKeys(username);
        passwordInput.clear();
        clickLink(submitButton);

    }
    public void missingUsername() {
        usernameInput.clear();
        clickLink(submitButton);
    }

    public String getUsername() {
        return usernameInput.getAttribute("value");
    }

    public boolean isUsernameInputEnabled() {
        return usernameInput.isEnabled();
    }

    public String getPassword() {
        return passwordInput.getAttribute("value");
    }

    public void cancel() {
        cancelButton.click();
    }

    public String getInputError() {
        try {
            return getTextFromElement(inputError);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getError() {
        try {
            return getTextFromElement(loginErrorMessage);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getInstruction() {
        return instruction != null ? instruction.getText() : null;
    }

    public String getSuccessMessage() {
        return loginSuccessMessage != null ? loginSuccessMessage.getText() : null;
    }
    public String getInfoMessage() {
        return loginInfoMessage != null ? loginInfoMessage.getText() : null;
    }

    @Override
    public boolean isCurrent() {
        String realm = "test";
        return isCurrent(realm);
    }

    public boolean isCurrent(String realm) {
        return driver.getTitle().equals("Sign in to " + realm) || driver.getTitle().equals("Anmeldung bei " + realm);
    }

    public void assertCurrent(String realm) {
        String name = getClass().getSimpleName();
        Assertions.assertTrue(isCurrent(realm), "Expected " + name + " but was " + driver.getTitle() + " (" + driver.getCurrentUrl() + ")");
    }

    public void clickRegister() {
        registerLink.click();
    }

    public void clickSocial(String alias) {
        WebElement socialButton = findSocialButton(alias);
        clickLink(socialButton);
    }

    public WebElement findSocialButton(String alias) {
        String id = "social-" + alias;
        return driver.findElement(By.id(id));
    }

    public void resetPassword() {
        clickLink(resetPasswordLink);
    }

    public void recoverUsername() {
        clickLink(recoverUsernameLink);
    }

    public void setRememberMe(boolean enable) {
        boolean current = rememberMe.isSelected();
        if (current != enable) {
            rememberMe.click();
        }
    }

    public boolean isRememberMeChecked() {
        return rememberMe.isSelected();
    }

    @Override
    public void open() {
        openLoginForm();
        assertCurrent();
    }

    private void openLoginForm() {
        driver.navigate().to(getLoginFormUrl());
    }
}