import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class TeamcityRunner implements Runner {

	private WebDriver driver;
	private WebDriverWait wait;
	private String teamcityURL;
	private String teamcityUser;
	private String teamcityPassword;

	private Logger logger = LoggerFactory.getLogger(TeamcityRunner.class);
	TeamcityRunner(WebDriver driver, Properties properties) {
		this.driver = driver;
		this.wait = new WebDriverWait(driver, 30);
		this.teamcityURL = properties.getProperty("teamcity.url");
		this.teamcityUser =  properties.getProperty("teamcity.login");
		this.teamcityPassword = properties.getProperty("teamcity.password");
	}

	@Override
	public void run(String buildPlanPart) {
		driver.get(teamcityURL);
		// если при входе на страницу нас встречает окно авторизации
		if (driver.getTitle().startsWith("Log in to TeamCity")) {
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
			driver.findElement(By.id("username")).sendKeys(teamcityUser);
			driver.findElement(By.id("password")).sendKeys(teamcityPassword);
			driver.findElement(By.cssSelector("input.btn.loginButton")).click();

			try {
				driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
				wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("bodyWrapper")));
			} catch (TimeoutException e) {
				throw new RuntimeException("Timed out the login to the page " + teamcityURL);
			}
		}

		try {
			// переходим по ссылке внутрь билдплана
			driver.findElement(By.partialLinkText(buildPlanPart)).click();
			// получаем номер последнего билда
			int lastBuildNum = getBuildNum();
			logger.debug("Last build number: {}", lastBuildNum);
			// запускаем билд
			driver.findElement(By.xpath("//*[contains(text(), 'Run')]")).click();
			int waitCounts = 0;
			// дожидаемся, что номер билда изменился
			while (getBuildNum() <= lastBuildNum) {
				Thread.sleep(60000); //минута
				driver.navigate().refresh();
				waitCounts++;
				if (waitCounts == 15) {
					throw new InterruptedException();
				}
			}

			// выходим из билдплана
			driver.navigate().back();
		} catch (Exception e) {
			throw new RuntimeException("Failed to start a buildplan " + buildPlanPart + " on TeamCity. Error: " + e.getMessage());
		}
	}

	/**
	 * Метод получающий номер предыдущего билдплана.
	 * В цикле проходим по таблице с билдами, начиная с первой строки. В случае если встретили не цифру, а литерал, например 'N/A' - переходим к следующей
	 * строке, и так 4 раза. Если на последней промсмотренной строке не нашли цифру - считаем, что последний номер 0.
	 * @return
	 */
	private int getBuildNum() {
		int result = 0;
		for (int  i = 1; i < 5; i++){
			try{
				result = Integer.parseInt(driver.findElement(By.xpath("//*[@id=\"historyTableInner\"]/table/tbody/tr[" + i + "]/td[2]")).getText().replace("#","").trim());
			}catch (Exception e){
				// do nothing
			}
			if (result != 0) {
				return result;
			}
		}
		return result;
	}
}
