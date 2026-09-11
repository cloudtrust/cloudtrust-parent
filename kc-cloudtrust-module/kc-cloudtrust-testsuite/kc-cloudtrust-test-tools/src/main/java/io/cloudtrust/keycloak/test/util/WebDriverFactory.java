package io.cloudtrust.keycloak.test.util;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.jboss.logging.Logger;
import org.junit.platform.commons.util.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.AbstractDriverOptions;

import java.util.Map;

public class WebDriverFactory {
    private static final Logger LOG = Logger.getLogger(WebDriverFactory.class);
    private static WebDriver webDriver;

    private WebDriverFactory() {
    }

    public static WebDriver provide() {
        if (webDriver == null) {
            webDriver = createHtmlUnitDriver(true);
        }
        return webDriver;
    }

    /**
     * Use @link{WebDriverFactory.createHtmlUnitDriver}
     * @param driver
     * @return
     */
    @Deprecated
    public static WebDriver provide(String driver) {
        return createHtmlUnitDriver(true);
    }

    public static WebDriver createHtmlUnitDriver(boolean enableJavascript) {
        LOG.debug("Creating HTMLUnit driver");
        return new HtmlUnitDriver(enableJavascript);
    }

    private static void logOptions(String title, AbstractDriverOptions<?> options) {
        LOG.debug(title);
        Map<String, Object> map = options.asMap();
        if (map != null) {
            for (Map.Entry<String, Object> option : map.entrySet()) {
                LOG.debugf("Driver option: %s=%s", option.getKey(), option.getValue());
            }
        }
    }

    private static ChromeDriver chromeDriver = null;

    public static WebDriver createChromeDriver(String envArgs, boolean headless) {
        if (chromeDriver == null) {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            if (StringUtils.isNotBlank(envArgs)) {
                for (String chromeArg : envArgs.split(" ")) {
                    options.addArguments(chromeArg);
                }
            } else {
                if (headless) {
                    options.addArguments("--headless=new");
                }
                options.addArguments("--remote-allow-origins=*"); // stackoverflow.com/questions/75680149/unable-to-establish-websocket-connection
            }
            options.addArguments("--no-sandbox", "--disable-gpu", "--window-size=1420,1080", "--ignore-certificate-errors", "--disable-dev-shm-usage");
            logOptions("Creating Chrome driver", options);
            chromeDriver = new ChromeDriver(options);
        }
        return chromeDriver;
    }

    public static WebDriver createFirefoxDriver(String envArgs, boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        if (StringUtils.isNotBlank(envArgs)) {
            for (String chromeArg : envArgs.split(" ")) {
                options.addArguments(chromeArg);
            }
        } else if (headless) {
            options.addArguments("-headless");
        }
        logOptions("Creating Firefox driver", options);
        return new FirefoxDriver(options);
    }
}
