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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fortify.client.fod.api.FoDReleaseAPI;
import com.fortify.client.fod.connection.FoDAuthenticatingRestConnection;
import com.fortify.client.ssc.api.SSCArtifactAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.sync.fod_ssc.Constants;
import com.fortify.sync.fod_ssc.config.ConfigSyncScansTask;
import com.fortify.sync.fod_ssc.connection.ConnectionFactory;
import com.fortify.sync.fod_ssc.connection.ConnectionTester;
import com.fortify.sync.fod_ssc.connection.ssc.FoDSyncAPI;
import com.fortify.sync.fod_ssc.connection.ssc.FoDSyncAPI.ScanStatus;
import com.fortify.sync.fod_ssc.connection.ssc.FoDSyncAPI.SyncData;
import com.fortify.util.rest.json.JSONMap;

//TODO Get schedule from injected config, instead of directly from property (for @Scheduled and @ContionalOnExpression)?
@Component
//Only load bean if schedule is defined and not equal to '-'
@ConditionalOnExpression("'${sync.jobs.syncScans.schedule:-}'!='-'")
public class SyncScansTask {
	private final FoDAuthenticatingRestConnection fodConn;
	private final SSCAuthenticatingRestConnection sscConn;

	@Autowired
	public SyncScansTask(ConfigSyncScansTask config, ConnectionFactory connFactory) {
		this.fodConn = connFactory.getFodConnection(config.getFod());
		this.sscConn = connFactory.getSSCConnection(config.getSsc());
	}
	
	@Scheduled(cron="${sync.jobs.syncScans.schedule}")
	public void syncScans() {
		System.out.println("Running syncScans task");
		sscConn.api(FoDSyncAPI.class).processSyncedApplicationVersionsAndFoDReleases(
				fodConn, this::processSyncedApplicationVersions);
	}
	
	private final void processSyncedApplicationVersions(SyncData syncData, JSONMap fodRelease) {
		for ( String scanType : syncData.getIncludedScanTypes() ) {
			ScanStatus oldStatus = syncData.getScanStatus();
			ScanStatus newStatus = ScanStatus.parse(fodRelease);
			Date oldScanDate = oldStatus.getScanDate(scanType);
			Date newScanDate = newStatus.getScanDate(scanType);
			if ( newScanDate!=null && (oldScanDate==null || newScanDate.after(oldScanDate)) ) {
				Path tempFile = Paths.get(Constants.SYNC_HOME, String.format("%s-%s.fpr", scanType, UUID.randomUUID()));
				try {
					String fodReleaseId = syncData.getFodReleaseId();
					System.out.println("Processing: "+syncData.hashCode());
					System.out.println("Downloading "+scanType+" scan from release "+fodReleaseId);
					fodConn.api(FoDReleaseAPI.class).saveFPR(fodReleaseId, scanType, tempFile);
					System.out.println("Uploading "+scanType+" scan to version "+syncData.getApplicationVersionId());
					sscConn.api(SSCArtifactAPI.class).uploadArtifact(syncData.getApplicationVersionId(), tempFile.toFile());
				} finally {
					if ( tempFile.toFile().exists() ) {
						tempFile.toFile().delete();
					}
				}
			} 
			
		}
		sscConn.api(FoDSyncAPI.class).updateSyncStatus(syncData, fodRelease);
	}
	
	private void printDebugMsg(Object obj) {
		System.err.println(new Date().toString()+": "+obj);
	}
	
	@PostConstruct
	public void postConstruct() {
		ConnectionTester.testFoDConnection(fodConn);
		ConnectionTester.testSSCConnection(sscConn);
		// TODO Any other tests?
	}
}
