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
import com.fortify.sync.fod_ssc.config.ConfigLinkReleasesTask;
import com.fortify.sync.fod_ssc.config.ConfigSyncScansTask;

@SpringBootApplication
@EnableScheduling
public class FortifySyncFoDToSSCApplication {
	public static void main(String[] args) {
		Constants.setSystemProperties();
		checkConfigFile();
		SpringApplication.run(FortifySyncFoDToSSCApplication.class, args);
	}

	private static final void checkConfigFile() {
		String configFileName = System.getProperty("sync.config");
		File configFile = new File(configFileName);
		if ( !configFile.exists() ) {
			throw new RuntimeException("Configuration file "+configFileName+" does not exist");
		}
		if ( !configFile.canRead() ) {
			throw new RuntimeException("Configuration file "+configFileName+" cannot be read");
		}
	}
	
	@Bean
	@ConfigurationProperties("sync.connections.fod") 
	public FoDAuthenticatingRestConnectionBuilder fodConnectionBuilder() {
		return FoDAuthenticatingRestConnection.builder().useCache(false).multiThreaded(true);
	}
	
	@Bean
	@ConfigurationProperties("sync.connections.ssc") 
	public SSCAuthenticatingRestConnectionBuilder sscConnectionBuilder() {
		return SSCAuthenticatingRestConnection.builder().useCache(false).multiThreaded(true);
	}
	
	@Bean
	@ConfigurationProperties("sync.jobs.link-releases")
	public ConfigLinkReleasesTask configLinkReleasesTask() {
		return new ConfigLinkReleasesTask();
	}
	
	@Bean
	@ConfigurationProperties("sync.jobs.sync-scans")
	public ConfigSyncScansTask configSyncScansTask() {
		return new ConfigSyncScansTask();
	}
	
	
	@Bean
	public FoDAuthenticatingRestConnection fodConnection() {
		return fodConnectionBuilder().build();
	}
	
	@Bean
	public SSCAuthenticatingRestConnection sscConnection() {
		return sscConnectionBuilder().build();
	}
}
