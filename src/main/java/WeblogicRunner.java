import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class WeblogicRunner implements Runner {

	private static final String RUNNING = "RUNNING";
	private static final String SHUTDOWN = "SHUTDOWN";
	private WebDriver driver;
	private WebDriverWait wait;
	private String weblogicURL;
	private String weblogicUser;
	private String weblogicPassword;
	private String currentURL;
	private String expectedStatus = "";

	private Logger logger = LoggerFactory.getLogger(WeblogicRunner.class);

	WeblogicRunner(WebDriver driver, Properties properties) {
		this.driver = driver;
		this.wait = new WebDriverWait(driver, 45);
		this.weblogicURL = properties.getProperty("weblogic.url");
		this.weblogicUser =  properties.getProperty("weblogic.login");
		this.weblogicPassword = properties.getProperty("weblogic.password");
		this.currentURL = this.weblogicURL;
	}

	@Override
	public void run(String serverName) {

		driver.get(currentURL);
		// если при входе на страницу нас встречает окно авторизации
		if (driver.getTitle().startsWith("Oracle WebLogic Server")) {
			logIn();
		}

		// если мы на начальной странце
		if (driver.getTitle().startsWith("Home Page")) {
			moveToNecessaryPage(serverName);
		}

		// если мы уже находимся на нужной вкладке
		if (driver.getTitle().startsWith("Settings for " + serverName)) {
			// смотрим состояние сервера
			String currentStatus = driver.findElement(By.id("state1")).getText();
			logger.debug("Current status of WL: {}", currentStatus);

			// если работает - останавливаем
			if (currentStatus.equals(RUNNING)) {
				expectedStatus = SHUTDOWN;
				shutdownServer(serverName);
				logger.debug("Stopping WL. Expected status: {}", expectedStatus);
			} else if (currentStatus.equals(SHUTDOWN)){
				// иначе - запускаем
				expectedStatus = RUNNING;
				startServer(serverName);
				logger.debug("Starting WL. Expected status: {}", expectedStatus);
			}

			// дожидаемся, что сервер перешел на нужный нам статус
			while (!driver.findElement(By.id("state1")).getText().equals(expectedStatus)) {
				logger.debug("Waiting for expected status: {}. Current status is: {}", expectedStatus, driver.findElement(By.id("state1")).getText());
				try {
					Thread.sleep(60000); //минута
				} catch (InterruptedException e) {
					throw new RuntimeException();
				}
				driver.navigate().refresh();
			}
			logger.debug("Finishing work with WL");
		}
	}

	private void startServer(String serverName) {
		try{
			driver.findElement(By.xpath("//button[contains(text(), 'Start')]")).click();
			driver.findElement(By.xpath("//button[contains(text(), 'Yes')]")).click();
		} catch (Exception e) {
			throw new RuntimeException ("Failed to start server " + serverName + ". Error: " + e.getMessage());
		}
	}

	private void shutdownServer(String serverName) {
		try  {
			driver.findElement(By.xpath("//button[contains(text(), 'Shutdown')]")).click();
			driver.findElement(By.xpath("//*[contains(text(), 'Force Shutdown Now')]")).click();
			driver.findElement(By.xpath("//button[contains(text(), 'Yes')]")).click();
		} catch (Exception e) {
			throw new RuntimeException ("Failed to stop server " + serverName + ". Error: " + e.getMessage());
		}
	}

	private void moveToNecessaryPage(String path) {
		// переходим на вкладку с серверами
		driver.findElement(By.xpath("//*[contains(text(), 'Servers')]")).click();

		// заходим в нужный нам сервер
		driver.findElement(By.linkText(path)).click();

		// переходим на вкладку контроля
		driver.findElement(By.linkText("Control")).click();
		currentURL = driver.getCurrentUrl();
	}

	private void logIn() {
		driver.get(weblogicURL);
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("j_username")));

		driver.findElement(By.id("j_username")).sendKeys(weblogicUser);
		driver.findElement(By.id("j_password")).sendKeys(weblogicPassword);
		driver.findElement(By.cssSelector("input.formButton")).click();

		try {
			driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("HomePageBook")));
		} catch (TimeoutException e) {
			throw new RuntimeException("Timed out the login to the page " + weblogicURL);
		}
		currentURL = driver.getCurrentUrl();
	}
}
