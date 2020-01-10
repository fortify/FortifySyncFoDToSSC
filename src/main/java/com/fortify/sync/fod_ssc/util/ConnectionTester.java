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
package com.fortify.sync.fod_ssc.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fortify.client.fod.api.FoDReleaseAPI;
import com.fortify.client.fod.connection.FoDAuthenticatingRestConnection;
import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;

/**
 * Simple {@link Component} that tests the injected FoD and SSC connection
 * instances by executing a simple query during application start-up. Failure 
 * to connect to either SSC or FoD will cause the application to terminate.
 * 
 * @author Ruud Senden
 *
 */
@Component
public class ConnectionTester {
	
	/**
	 * Test the injected {@link FoDAuthenticatingRestConnection} by querying for
	 * a single arbitrary release.
	 * 
	 * @param conn
	 */
	@Autowired
	public final void testFoDConnection(FoDAuthenticatingRestConnection conn) {
		conn.api(FoDReleaseAPI.class)
			.queryReleases()
			.maxResults(1)
			.paramFields("releaseId")
			.build().getUnique();
	}

	/**
	 * Test the injected {@link SSCAuthenticatingRestConnection} by querying for
	 * a single arbitrary application version.
	 * 
	 * @param conn
	 */
	@Autowired
	public final void testSSCConnection(SSCAuthenticatingRestConnection conn) {
		conn.api(SSCApplicationVersionAPI.class)
			.queryApplicationVersions()
			.maxResults(1)
			.paramFields("id")
			.build().getUnique();
	}

}
