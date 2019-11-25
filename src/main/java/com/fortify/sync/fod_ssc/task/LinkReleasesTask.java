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
package com.fortify.sync.fod_ssc.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fortify.client.fod.connection.FoDAuthenticatingRestConnection;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.sync.fod_ssc.config.ConfigLinkReleasesTask;
import com.fortify.sync.fod_ssc.connection.ConnectionHolder;

@Component
// Only load bean if schedule is defined and not equal to '-'
@ConditionalOnExpression("'${sync.jobs.linkReleases.schedule:-}'!='-'")
public class LinkReleasesTask {
	private static final Logger LOG = LoggerFactory.getLogger(LinkReleasesTask.class);
	private final FoDAuthenticatingRestConnection fodConn;
	private final SSCAuthenticatingRestConnection sscConn;
	//private final ConfigLinkReleasesTask config;
	
	@Autowired
	public LinkReleasesTask(ConfigLinkReleasesTask config, ConnectionHolder connFactory) {
		//this.config = config;
		this.fodConn = connFactory.getFodConnection();
		this.sscConn = connFactory.getSscConnection();
	}
	
	// TODO Set schedule based on inject config, instead of directly from property?
	@Scheduled(cron="${sync.jobs.linkReleases.schedule}")
	public void linkReleases() {
		LOG.debug("Running linkReleases task");
	}
}
