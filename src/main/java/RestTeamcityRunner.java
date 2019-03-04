import com.google.common.base.MoreObjects;
import configuration.model.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.util.Properties;

public class RestTeamcityRunner implements Runner {

	private String teamcityURL;
	private String teamcityUser;
	private String teamcityPassword;
	private int waitCounts;

	private Logger logger = LoggerFactory.getLogger(RestTeamcityRunner.class);

	RestTeamcityRunner(Properties properties) {
		this.teamcityURL = properties.getProperty("teamcity.url");
		this.teamcityUser =  properties.getProperty("teamcity.login");
		this.teamcityPassword = properties.getProperty("teamcity.password");
		this.waitCounts = Integer.parseInt(MoreObjects.firstNonNull(properties.getProperty("waitCounts"), "30"));
	}

	@Override
	public void run(String buildNumber) throws JAXBException, InterruptedException{
		Build build = new Build();
		build.setBuildTypeId(buildNumber);

		TeamcityRestClient client = new TeamcityRestClient(teamcityURL, teamcityUser, teamcityPassword);
		Build result = client.triggerBuild(build);
		Long triggeredBuildId = result.getId();
		logger.debug("Triggered build {} with id {}", buildNumber, triggeredBuildId);
		String currentState; // state may be - queued, running or finished
		int localWaitCounts = waitCounts;
		do {
			Thread.sleep(60000); // a minute
			currentState = client.getBuildInfo(triggeredBuildId).getState();
			logger.debug("Current state of build {} is '{}'", triggeredBuildId, currentState);
			localWaitCounts--;
			if (localWaitCounts == 0) {
				throw new InterruptedException("Timed out when waiting to finish the build");
			}
		} while (!currentState.equalsIgnoreCase("finished"));

		// here build state must be finished and we looking is it successful
		String buildStatus = client.getBuildInfo(triggeredBuildId).getStatus();
		if (!buildStatus.equalsIgnoreCase("success")) {
			throw new RuntimeException("Build was unsuccessfull");
		}
		logger.debug("Build {} was successful executed", triggeredBuildId);
	}
}
