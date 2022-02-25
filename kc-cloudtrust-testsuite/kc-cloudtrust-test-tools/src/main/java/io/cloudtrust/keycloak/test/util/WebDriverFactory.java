package io.cloudtrust.keycloak.test.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class WebDriverFactory {
	private static WebDriver webDriver;
	private static final List<String> ignorePatterns = Arrays.asList("patternfly/dist/css", "react-core/dist");

	private WebDriverFactory() {
	}

	public static void ignorePatterns(Collection<String> patterns) {
		WebDriverFactory.ignorePatterns.clear();
		WebDriverFactory.ignorePatterns.addAll(patterns);
	}

	public static WebDriver provide() {
		if (webDriver==null) {
			String driver = TestSuiteParameters.get().getEnv("browser", "");
			switch (driver) {
			case "chrome": webDriver = createChromeDriver(); break;
			case "firefox" : webDriver = createFirefoxDriver(); break;
			default: webDriver = createHtmlUnitDriver();
			}
		}
		return webDriver;
	}

	private static WebDriver createHtmlUnitDriver() {
		return new CloudtrustHtmlUnitDriver(ignorePatterns);
	}

	private static WebDriver createChromeDriver() {
		/*
		String key = "webdriver.chrome.driver";
		String systemValue = System.getenv(key);
		String envDriver = TestSuiteParameters.get().getEnv(key, null);
		if (envDriver!=null && !envDriver.equals(systemValue)) {
			EnvironmentVariables vars = new EnvironmentVariables();
			vars.set(key, envDriver);
			Assert.assertEquals(envDriver, System.getenv(key));
		}
		*/
		return new ChromeDriver();
	}

	private static WebDriver createFirefoxDriver() {
		return new FirefoxDriver();
	}
}
