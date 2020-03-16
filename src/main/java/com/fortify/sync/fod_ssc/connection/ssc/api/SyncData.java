/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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
package com.fortify.sync.fod_ssc.connection.ssc.api;

import com.fortify.util.rest.json.JSONMap;

/**
 * This data class provides access to the {@link SyncConfig} and {@link SyncStatus}
 * instances for the given SSC application version.
 * 
 * @author Ruud Senden
 *
 */
public class SyncData {
	private final JSONMap sscApplicationVersion;
	private SyncConfig syncConfig;
	private SyncStatus syncStatus;

	/**
	 * Constructor for setting the current SSC application version JSON data
	 * @param sscApplicationVersion
	 */
	public SyncData(JSONMap sscApplicationVersion) {
		this.sscApplicationVersion = sscApplicationVersion;
	}
	
	/**
	 * Provide lazy access to the {@link SyncConfig} instance for the configured 
	 * application version.
	 * @return
	 */
	public SyncConfig getSyncConfig() {
		if ( syncConfig==null ) {
			syncConfig = SyncConfig.getFromApplicationVersion(sscApplicationVersion);
		}
		return syncConfig;
	}
	
	/**
	 * Provide lazy access to the {@link SyncStatus} instance for the configured 
	 * application version.
	 * @return
	 */
	public SyncStatus getSyncStatus() {
		if ( syncStatus==null ) {
			syncStatus = SyncStatus.getFromApplicationVersion(sscApplicationVersion);
		}
		return syncStatus;
	}
	
	public String getSSCApplicationVersionId() {
		return sscApplicationVersion.get("id", String.class);
	}

}
