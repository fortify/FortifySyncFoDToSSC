/*******************************************************************************
 * (c) Copyright 2017 EntIT Software LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.sync.fod_ssc;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * This class provides access to various constants like home directory locations
 * and configuration file locations. This class includes some logic to automatically
 * determine the appropriate values for these constants.
 * 
 * @author Ruud Senden
 *
 */
public class Constants {
	public static final String FORTIFY_HOME = _getFortifyHome();
	public static final String SYNC_HOME = _getSyncHome(FORTIFY_HOME);
	public static final String SYNC_CONFIG = _getSyncConfig(SYNC_HOME);
	
	/**
	 * Private constructor to disallow instantiation
	 */
	private Constants() {}

	/**
	 * Get the Fortify home directory using one of the following approaches, in this order:
	 * <ul>
	 *  <li>Value of the <code>fortify.home</code> system property</li>
	 *  <li>Value of the <code>FORTIFY_HOME</code> environment variable</li>
	 *  <li>Default value <code>~/.fortify</code></li>
	 * </ul>
	 * @return
	 */
	private static final String _getFortifyHome() {
		String fortifyHome = System.getProperty("fortify.home", System.getenv("FORTIFY_HOME"));
		return fortifyHome!=null?fortifyHome:new File(System.getProperty("user.home"),"/.fortify").getAbsolutePath();
	}
	
	/**
	 * Get the home directory for the FoD to SSC sync utility.
	 * @return 
	 */
	private static final String _getSyncHome(String fortifyHome) {
		return System.getProperty("sync.home", fortifyHome+"/FortifySyncFoDToSSC");
	}
	
	/**
	 * Find the configuration file in one of the following locations, in this order:
	 * <ul>
	 *  <li>File name specified by the <code>sync.config</code> system property</li>
	 *  <li>File named FortifySyncFoDToSSC.yml in the current working directory</li>
	 *  <li>File named config.yml in the directory returned by {@link #_getSyncHome()}</li>
	 * </ul>
	 * @return 
	 */
	private static final String _getSyncConfig(String syncHome) {
		String syncConfig = System.getProperty("sync.config");
		if ( StringUtils.isNotBlank(syncConfig) && checkReadable(new File(syncConfig)) ) {
			return syncConfig;
		} else {
			List<String> configFileNames = Arrays.asList(
				new File(".","/FortifySyncFoDToSSC.yml").getAbsolutePath(), 
				new File(syncHome, "/config.yml").getAbsolutePath());
			for ( String fileName : configFileNames ) {
				if ( isReadable(new File(fileName)) ) {
					return fileName;
				}
			}
			throw new IllegalArgumentException("Configuration file not found in any of the following locations: "+configFileNames);
		}
	}
	
	/**
	 * Return true if given {@link File} exists and is readable
	 */
	private static final boolean isReadable(File file) {
		return file.exists() && file.canRead();
	}
	
	/**
	 * Throw an exception if the given file does not exist or is not readable
	 */
	private static final boolean checkReadable(File file) {
		if ( !isReadable(file) ) {
			throw new IllegalArgumentException("Configuration file "+file.getAbsolutePath()+" does not exist or is not readable");
		}
		return true;
	}
	
	
	/**
	 * Set various system properties for later use.
	 */
	public static final void updateSystemProperties() {
		System.setProperty("fortify.home", FORTIFY_HOME);
		System.setProperty("sync.home", SYNC_HOME);
		System.setProperty("sync.config", SYNC_CONFIG);
		System.setProperty("spring.config.location", "classpath:/application.yml,file:"+SYNC_CONFIG);
	}
}
