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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fortify.client.fod.api.FoDReleaseAPI;
import com.fortify.client.ssc.api.SSCArtifactAPI;
import com.fortify.sync.fod_ssc.Constants;
import com.fortify.sync.fod_ssc.config.SyncScansTaskConfig;
import com.fortify.sync.fod_ssc.util.SyncHelper;
import com.fortify.sync.fod_ssc.util.SyncHelper.ScanStatus;
import com.fortify.sync.fod_ssc.util.SyncHelper.SyncData;
import com.fortify.util.rest.json.JSONMap;

//TODO Get schedule from injected config, instead of directly from property (for @Scheduled and @ContionalOnExpression)?
@Component
//Only load bean if schedule is defined and not equal to '-'
@ConditionalOnExpression("'${sync.jobs.syncScans.schedule:-}'!='-'")
public class SyncScansTask {
	private static final Logger LOG = LoggerFactory.getLogger(SyncScansTask.class);
	private static final SimpleDateFormat FMT_FOD_DATE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private final SyncHelper syncHelper;

	@Autowired
	public SyncScansTask(SyncScansTaskConfig config, SyncHelper syncHelper) {
		this.syncHelper = syncHelper;
		LOG.info("syncScans task configuration: {}", config);
	}
	
	@Scheduled(cron="${sync.jobs.syncScans.schedule}")
	public void syncScans() {
		LOG.debug("Running syncScans task");
		try {
			syncHelper.processSyncedApplicationVersionsAndFoDReleases(this::processSyncedApplicationVersions);
		} finally {
			LOG.debug("Completed syncScans task");
		}
	}
	
	private final void processSyncedApplicationVersions(SyncData syncData, JSONMap fodRelease) {
		String sscApplicationVersionId = syncData.getApplicationVersionId();
		String fodReleaseId = fodRelease.get("releaseId",String.class);
		ScanStatus scanStatus = syncData.getScanStatus().newIfDifferentFoDReleaseId(fodReleaseId);
		String[] scanTypes = syncData.getIncludedScanTypes();
		processSyncedApplicationVersion(sscApplicationVersionId, fodRelease, scanTypes, scanStatus);
		syncHelper.updateSyncStatus(sscApplicationVersionId, scanStatus);
	}

	protected void processSyncedApplicationVersion(
			String sscApplicationVersionId, JSONMap fodRelease,
			String[] scanTypes, ScanStatus scanStatus) {
		for ( String scanType : scanTypes ) {
			Date fodScanDate = getFoDScanDate(fodRelease, scanType);
			Date oldScanDate = scanStatus.getScanDate(scanType);
			LOG.debug("[{} - {}] Scan type {}: current scan date {}, previous scan date {}", fodRelease.get("applicationName", String.class), fodRelease.get("releaseName", String.class), scanType, fodScanDate, oldScanDate);
			if ( fodScanDate!=null && (oldScanDate==null || fodScanDate.after(oldScanDate)) ) {
				Path tempFile = Paths.get(Constants.SYNC_HOME, String.format("%s-%s.fpr", scanType, UUID.randomUUID()));
				try {
					// TODO Pipe FPR input stream from FoD directly to SSC, instead of using temp file
					String fodReleaseId = fodRelease.get("releaseId",String.class);
					LOG.info("Downloading {} scan from FoD release id {}", scanType, fodReleaseId);
					syncHelper.getFodConn().api(FoDReleaseAPI.class).saveFPR(fodReleaseId, scanType, tempFile);
					LOG.info("Uploading {} scan to SSC application version id {}", scanType, sscApplicationVersionId);
					syncHelper.getSscConn().api(SSCArtifactAPI.class).uploadArtifact(sscApplicationVersionId, tempFile.toFile());
				} finally {
					if ( tempFile.toFile().exists() ) {
						tempFile.toFile().delete();
					}
				}
				scanStatus.setScanDate(scanType, fodScanDate);
			} 
		}
	}
	
	private static final Date getFoDScanDate(JSONMap fodRelease, String scanType) {
		return parseFoDDate(fodRelease.get(scanType.toLowerCase()+"ScanDate", String.class));
	}
	
	private static final Date parseFoDDate(String dateString) {
		if ( dateString == null ) { return null; }
		try {
			return FMT_FOD_DATE.parse(StringUtils.substringBefore(dateString, "."));
		} catch ( ParseException e ) {
			throw new RuntimeException("Error parsing scan date "+dateString+" returned by FoD", e);
		}
	}
}
