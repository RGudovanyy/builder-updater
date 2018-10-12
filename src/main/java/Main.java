import configuration.ConfigManager;
import configuration.DriverManager;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		if (args.length == 0) {
			logger.error("No parameters was passed to the script!");
			return;
		}
		final ConfigManager configManager = new ConfigManager(args);

		final WebDriver teamcityDriver = DriverManager.getHtmUnitDriver();
		final Runner teamcityRunner = new TeamcityRunner(teamcityDriver, configManager.getProperties());

		try {
			// собираем
			if (configManager.isBuildable()) {
				logger.info("Start building patch with TeamCity");
				teamcityRunner.run(configManager.getBuildName());
				logger.info("Patch was successfully builded");
			}
			// деплоим
			if (configManager.isDeployable() || configManager.isRestartable()) {
				logger.info("Start termination of WebLogic server");
				final WebDriver weblogicDriver = DriverManager.getHtmUnitDriver();
				final Runner weblogicRunner = new WeblogicRunner(weblogicDriver, configManager.getProperties());
				final String serverName = configManager.getServer();
				// стопаем стенд
				weblogicRunner.run(serverName);
				logger.info("WebLogic server was successfully terminated");

				if (!configManager.isRestartable()) {
					// деплоим
					logger.info("Start deploying patch with TeamCity");
					teamcityRunner.run(configManager.getDeployName());
					logger.info("Patch was successfully deployed");
				}

				// запускаем стенд
				logger.info("Starting WebLogic server");
				weblogicRunner.run(serverName);
				logger.info("WebLogic server was successfully started");
				weblogicDriver.close();
			}
		} catch (Exception e) {
			logger.error("An unexpected error occurred during program execution: \n {}", e);
		}
		// закрываем коннекты
		teamcityDriver.close();
		logger.info("Bye");
	}
}
