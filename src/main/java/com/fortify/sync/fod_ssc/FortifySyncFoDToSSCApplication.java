package com.fortify.sync.fod_ssc;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

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
	
	
}
