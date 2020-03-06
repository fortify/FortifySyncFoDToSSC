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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fortify.client.fod.api.FoDReleaseAPI;
import com.fortify.client.fod.connection.FoDAuthenticatingRestConnection;
import com.fortify.client.ssc.api.SSCAttributeAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.sync.fod_ssc.connection.ssc.api.SyncAPI;

/**
 * Simple {@link Component} that tests for various prerequisites during application start-up. 
 * Any failure will cause the application to terminate.
 * 
 * @author Ruud Senden
 *
 */
@Component
public class PrerequisitesTester {
	private static final Logger LOG = LoggerFactory.getLogger(PrerequisitesTester.class);
	
	/**
	 * Test the injected {@link FoDAuthenticatingRestConnection} by querying for
	 * a single arbitrary release.
	 * 
	 * @param conn
	 */
	@Autowired
	public final void testFoDConnection(FoDAuthenticatingRestConnection conn) {
		LOG.info("Testing whether FoD can be contacted");
		conn.api(FoDReleaseAPI.class)
			.queryReleases()
			.maxResults(1)
			.paramFields("releaseId")
			.build().getUnique();
	}
	
	/**
	 * Test whether the necessary application attributes have been defined on SSC.
	 * This also implicitly tests whether we can successfully connect to SSC, so we
	 * do not need a separate testSSCConnection() method.
	 * 
	 * @param conn
	 */
	@Autowired
	public final void testSSCAttributes(SSCAuthenticatingRestConnection conn) {
		LOG.info("Testing whether SSC can be contacted, and all required application attributes have been defined");
		final Set<String> requiredAttrs = new HashSet<>(Arrays.asList(SyncAPI.getRequiredSSCApplicationAttributeNames()));
		conn.api(SSCAttributeAPI.class)
			.queryAttributeDefinitions()
			.paramFields("name")
			.build()
			.processAll(json->requiredAttrs.remove(json.get("name", String.class)));
		if ( requiredAttrs.size()>0 ) {
			throw new IllegalStateException("The following required application attributes are not defined on SSC: "+requiredAttrs);
		}
	}
}
