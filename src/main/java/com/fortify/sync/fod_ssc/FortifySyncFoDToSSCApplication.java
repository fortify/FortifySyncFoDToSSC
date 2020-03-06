package com.fortify.sync.fod_ssc;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fortify.client.fod.connection.FoDAuthenticatingRestConnection;
import com.fortify.client.fod.connection.FoDAuthenticatingRestConnection.FoDAuthenticatingRestConnectionBuilder;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection.SSCAuthenticatingRestConnectionBuilder;
import com.fortify.sync.fod_ssc.config.LinkReleasesTaskConfig;
import com.fortify.sync.fod_ssc.config.SyncScansTaskConfig;

/**
 * This {@link SpringBootApplication} class provides the following functionality:
 * <ul>
 *  <li><code>main()</code> method for starting the utility:
 *      <ul>
 *       <li>Set various system properties by calling {@link Constants#setSystemProperties()}</li>
 *       <li>Check whether the required configuration file exists</li>
 *       <li>Start the actual application by calling {@link SpringApplication#run(Class, String...)}</li>
 *      </ul></li>
 *  <li>{@link Bean}-annotated methods that provide configuration beans and
 *      connections instances for accessing FoD and SSC<li>
 * </ul>
 * @author Ruud Senden
 *
 */
@SpringBootApplication
@EnableScheduling
public class FortifySyncFoDToSSCApplication {
	/**
	 * Start the application
	 * @param args
	 */
	public static void main(String[] args) {
		Constants.setSystemProperties();
		checkConfigFile();
		createScansTempDir();
		SpringApplication.run(FortifySyncFoDToSSCApplication.class, args);
	}

	/**
	 * Check whether the required configuration file exists
	 */
	private static final void checkConfigFile() {
		File configFile = new File(Constants.SYNC_CONFIG);
		if ( !configFile.exists() ) {
			throw new RuntimeException("Configuration file "+Constants.SYNC_CONFIG+" does not exist");
		}
		if ( !configFile.canRead() ) {
			throw new RuntimeException("Configuration file "+Constants.SYNC_CONFIG+" cannot be read");
		}
	}
	
	/**
	 * Create the directory for temporarily holding scan data
	 * if it does not yet exist.
	 */
	private static final void createScansTempDir() {
		new File(Constants.SCANS_TEMP_DIR).mkdir();
	}
	
	
	
	/**
	 * Get a {@link FoDAuthenticatingRestConnectionBuilder} instance, automatically
	 * wiring all FoD connection properties defined in the configuration file. 
	 * @return
	 */
	@Bean
	@ConfigurationProperties("sync.connections.fod") 
	public FoDAuthenticatingRestConnectionBuilder fodConnectionBuilder() {
		return FoDAuthenticatingRestConnection.builder().useCache(false).multiThreaded(true).scopes("view-apps", "view-issues");
	}
	
	/**
	 * Get a {@link SSCAuthenticatingRestConnectionBuilder} instance, automatically
	 * wiring all SSC connection properties defined in the configuration file. 
	 * @return
	 */
	@Bean
	@ConfigurationProperties("sync.connections.ssc") 
	public SSCAuthenticatingRestConnectionBuilder sscConnectionBuilder() {
		return SSCAuthenticatingRestConnection.builder().useCache(false).multiThreaded(true);
	}
	
	/**
	 * Get a {@link LinkReleasesTaskConfig} instance, automatically wiring all 
	 * configuration properties defined in the configuration file. 
	 * @return
	 */
	@Bean
	@ConfigurationProperties("sync.tasks.link-releases")
	public LinkReleasesTaskConfig configLinkReleasesTask() {
		return new LinkReleasesTaskConfig();
	}
	
	/**
	 * Get a {@link SyncScansTaskConfig} instance, automatically wiring all 
	 * configuration properties defined in the configuration file. 
	 * @return
	 */
	@Bean
	@ConfigurationProperties("sync.tasks.sync-scans")
	public SyncScansTaskConfig configSyncScansTask() {
		return new SyncScansTaskConfig();
	}
	
	/**
	 * Instantiate the {@link FoDAuthenticatingRestConnection} instance
	 * used to connect to FoD, based on the connection builder returned
	 * by {@link #fodConnectionBuilder()}. 
	 * @return
	 */
	@Bean
	public FoDAuthenticatingRestConnection fodConnection() {
		return fodConnectionBuilder().useCache(false).build();
	}
	
	/**
	 * Instantiate the {@link SSCAuthenticatingRestConnection} instance
	 * used to connect to SSC, based on the connection builder returned
	 * by {@link #sscConnectionBuilder()}. 
	 * @return
	 */
	@Bean
	public SSCAuthenticatingRestConnection sscConnection() {
		return sscConnectionBuilder().useCache(false).build();
	}
}
