package configuration;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import java.util.HashMap;
import java.util.Map;

public class DriverManager {

	public static WebDriver getHtmUnitDriver() {
		Map<String, Object> cap = new HashMap<>();
		cap.put("marionette", false);
		cap.put("javascriptEnabled", true);
		cap.put("browserName", "htmlunit");
		Capabilities capabilities = new ImmutableCapabilities(cap);
		return new HtmlUnitDriver(capabilities);
	}

	public static WebDriver getFirefoxDriver(String pathToDriver) {
		if (StringUtils.isBlank(pathToDriver)){
			throw new NotFoundException("Path to Firefox driver is empty!");
		}
		System.setProperty("webdriver.gecko.driver", pathToDriver);
		FirefoxOptions options = new FirefoxOptions();
		options.setCapability("marionette", false);
		options.setHeadless(true);
		return new FirefoxDriver(options);
	}
}
