package io.cloudtrust.keycloak.test.init;

import io.cloudtrust.keycloak.test.AbstractInKeycloakTest;
import io.cloudtrust.keycloak.test.container.KeycloakQuarkusConfiguration;
import io.cloudtrust.keycloak.test.pages.AbstractPage;
import io.cloudtrust.keycloak.test.pages.WebPage;
import io.cloudtrust.keycloak.test.util.OAuthClient;
import io.cloudtrust.keycloak.test.util.WebDriverFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TestInitializer {
    public void init(AbstractInKeycloakTest target, boolean forceReInit) throws io.cloudtrust.keycloak.test.init.InjectionException {
        for (Class<?> clazz = target.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field f : clazz.getDeclaredFields()) {
                for (Annotation a : f.getAnnotations()) {
                    if (WebPage.class == a.annotationType()) {
                        initWebPage(target, f, forceReInit);
                    }
                }
            }
        }
    }

    private <T extends AbstractInKeycloakTest> void initWebPage(T testInstance, Field field, boolean forceReInit) throws InjectionException {
        try {
            field.setAccessible(true);
            if (forceReInit || field.get(testInstance) == null) {
                field.set(testInstance, createWebPage(field.getType()));
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new InjectionException("Can't initialize WebPage for field " + field.getName(), e);
        }
    }

    private Object createWebPage(Class<?> type) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Object newInstance = type.getDeclaredConstructor().newInstance();
        Method webDriverSetter = AbstractPage.class.getDeclaredMethod("setDriver", WebDriver.class);
        if (webDriverSetter != null) {
            webDriverSetter.invoke(newInstance, WebDriverFactory.provide());
        }
        Method oauthClientSetter = AbstractPage.class.getDeclaredMethod("setOAuthClient", OAuthClient.class);
        if (oauthClientSetter != null) {
            oauthClientSetter.invoke(newInstance, getOAuthClient());
        }
        PageFactory.initElements(WebDriverFactory.provide(), newInstance);
        return newInstance;
    }

    private OAuthClient getOAuthClient() {
        KeycloakQuarkusConfiguration cfg = KeycloakQuarkusConfiguration.createBuilder().build();
        return new OAuthClient(cfg.getBaseUrl());
    }
}
