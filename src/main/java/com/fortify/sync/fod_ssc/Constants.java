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

/**
 * This class provides access to various constants like home directory locations
 * and configuration file locations. This class includes some logic to automatically
 * determine the appropriate values for these constants.
 * 
 * @author Ruud Senden
 *
 */
public class Constants {
	public static final String FORTIFY_HOME = getFortifyHome();
	public static final String SYNC_HOME = getSyncHome();
	public static final String SYNC_CONFIG = getSyncHome()+"/config.yml";
	public static final String SCANS_TEMP_DIR = getSyncHome()+"/scans";
	
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
	private static final String getFortifyHome() {
		String fortifyHome = System.getProperty("fortify.home", System.getenv("FORTIFY_HOME"));
		return fortifyHome!=null?fortifyHome:"~/.fortify";
	}
	
	/**
	 * Get the home directory for the FoD to SSC sync utility.
	 * @return 
	 */
	private static final String getSyncHome() {
		return getFortifyHome()+"/FortifySyncFoDToSSC";
	}
	
	/**
	 * Set various system properties for later use.
	 */
	public static final void setSystemProperties() {
		System.setProperty("fortify.home", FORTIFY_HOME);
		System.setProperty("sync.home", SYNC_HOME);
		System.setProperty("sync.config", SYNC_CONFIG);
		System.setProperty("spring.config.location", "classpath:/application.yml,file:"+SYNC_CONFIG);
	}
}
