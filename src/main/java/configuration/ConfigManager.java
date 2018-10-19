package configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

public class ConfigManager {
	private Logger logger = LoggerFactory.getLogger(ConfigManager.class);

	private static final String DEPLOY = "deploy";
	private static final String SERVER = "server";
	private static final String RESTART = "restart";
	private static final String BUILD = "build";
	private static final String DEFAULT_SERVER = "WC_Portlet";
	private static final String CONFIG_FILE = "config";

	private Map<String,String> arguments;
	private Properties properties;

	public ConfigManager(String[] args) {
		arguments = new HashMap<>();

		for (String arg : args) {
			String[] argPair = arg.split("=");
			if (argPair.length > 0) {
				arguments.put(argPair[0], argPair[1]);
			}
		}

		readPropertiesFile();
	}

	private void readPropertiesFile() {
		properties = new Properties();
		try (InputStream inputStream = new FileInputStream(arguments.get(CONFIG_FILE))) {
			properties.load(inputStream);
		} catch (Exception e) {
			logger.error("Can't read configuration file into properties! Error: \n{}",e);
		}
		if (arguments.containsKey(DEPLOY) && !arguments.containsKey(SERVER)) {
			properties.setProperty("weblogic.server", DEFAULT_SERVER);
		}
	}

	public Properties getProperties() {
		return properties;
	}

	public boolean isDeployable() {
		return Boolean.parseBoolean(this.arguments.get(DEPLOY));
	}

	public boolean isBuildable() {
		return Boolean.parseBoolean(this.arguments.get(BUILD));
	}

	public String getBuildName() {
		return this.properties.getProperty("teamcity.build");
	}

	public String getDeployName() {
		if (isDeployable()) {
			return this.properties.getProperty("teamcity.deploy");
		}
		return null;
	}

	public String getServer() {
		if (isDeployable() || isRestartable()) {
			return this.properties.getProperty("weblogic.server");
		}
		return null;
	}

	public boolean isRestartable() {
		return Boolean.parseBoolean(this.arguments.get(RESTART));
	}
}
