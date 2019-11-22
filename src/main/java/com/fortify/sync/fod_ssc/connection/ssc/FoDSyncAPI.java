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
package com.fortify.sync.fod_ssc.connection.ssc;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.client.fod.api.FoDReleaseAPI;
import com.fortify.client.fod.connection.FoDAuthenticatingRestConnection;
import com.fortify.client.ssc.api.AbstractSSCAPI;
import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.api.SSCAttributeAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.sync.fod_ssc.config.ConfigSyncScansTask;
import com.fortify.sync.fod_ssc.util.DefaultObjectMapperFactory;
import com.fortify.util.rest.json.JSONMap;
import com.fortify.util.rest.json.processor.AbstractJSONMapProcessor;

import lombok.Data;

public class FoDSyncAPI extends AbstractSSCAPI {
	private static SSCSyncedApplicationVersionFilter filter;
	
	public FoDSyncAPI(SSCAuthenticatingRestConnection conn) {
		super(conn);
	}
	
	public void updateSyncStatus(SyncData syncData, JSONMap fodRelease) {
		ScanStatus oldStatus = syncData.getScanStatus();
		// TODO We should only copy properties from fodRelease for which we actually processed a scan
		//      Otherwise if we temporarily disable a scan type, it won't be uploaded 
		//      once we re-enable that scan type, until a new scan of that type is 
		//      available on FoD
		ScanStatus newStatus = ScanStatus.parse(fodRelease);
		if ( newStatus!=null && !newStatus.equals(oldStatus) ) {
			String applicationVersionId = syncData.getApplicationVersionId();
			MultiValueMap<String, Object> attributes = new LinkedMultiValueMap<>();
			attributes.add("FoD Sync - Status", newStatus.asSyncStatusString());
			conn().api(SSCAttributeAPI.class)
				.updateApplicationVersionAttributes(applicationVersionId, attributes);
		}
	}
	
	public void processSyncedApplicationVersions(final Consumer<SyncData> consumer) {
		String authEntityName = filter.getSscSyncScansUserName();
		conn().api(SSCApplicationVersionAPI.class)
			.queryApplicationVersionsByAuthEntityName(authEntityName)
			.paramFields("id", "name", "project")
			.onDemandAttributeValuesByName()
			.build().processAll(new AbstractJSONMapProcessor() {
				
				@Override
				public void process(JSONMap json) {
					SyncData syncData = new SyncData(json);
					if ( syncData.isSyncEnabled() ) {
						consumer.accept(syncData);
					}
				}
			});
	}
	
	public void processSyncedApplicationVersionsAndFoDReleases(final FoDAuthenticatingRestConnection fodConn, final BiConsumer<SyncData,JSONMap> consumer) {
		processSyncedApplicationVersions(syncData->processSyncedApplicationVersionsAndFoDReleases(fodConn, syncData, consumer));
	}
	
	private void processSyncedApplicationVersionsAndFoDReleases(final FoDAuthenticatingRestConnection fodConn, final SyncData syncData, final BiConsumer<SyncData,JSONMap> consumer) {
		String fodReleaseId = syncData.getFodReleaseId();
		JSONMap fodRelease = fodConn.api(FoDReleaseAPI.class)
			.queryReleases()
			.releaseId(fodReleaseId)
			.paramFields("releaseId", "applicationName", "releaseName", "staticScanDate", "dynamicScanDate", "mobileScanDate")
			.build().getUnique();
		consumer.accept(syncData, fodRelease);
	}

	@FunctionalInterface
	public static interface ISSCSyncDataAndFoDReleaseProcessor {
		public void process(SyncData sscSyncData, JSONMap fodRelease);
	}

	@FunctionalInterface
	public static interface ISSCSyncDataProcessor {
		public void process(SyncData syncData);
	}
	
	@Data
	public static final class SyncData {
		private String applicationVersionId;
		private String fodReleaseId;
		private String[] includedScanTypes;
		private ScanStatus scanStatus;
		
		// TODO Make attribute names configurable?
		public SyncData(JSONMap json) {
			this.applicationVersionId = json.get("id", String.class);
			JSONMap attributeValuesByName = json.get("attributeValuesByName", JSONMap.class);
			this.fodReleaseId = attributeValuesByName.get("FoD Sync - Release Id", String.class);
			this.includedScanTypes = attributeValuesByName.getOrCreateJSONList("FoD Sync - Include Scan Types").toArray(new String[]{});
			this.scanStatus = ScanStatus.parse(this.fodReleaseId, attributeValuesByName.get("FoD Sync - Status", String.class));
		}
		
		public boolean isSyncEnabled() {
			return StringUtils.isNotBlank(this.fodReleaseId) 
					&& includedScanTypes.length > 0;
		}
	}
	
	@Data
	public static final class ScanStatus {
		private static final SimpleDateFormat FMT_DATE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		private static final ObjectMapper MAPPER = DefaultObjectMapperFactory.getDefaultObjectMapper();
		private String releaseId;
		private Date staticScanDate;
		private Date dynamicScanDate;
		private Date mobileScanDate;

		public ScanStatus() {}
		
		public static final ScanStatus parse(JSONMap fodRelease) {
			ScanStatus result = new ScanStatus();
			result.releaseId = fodRelease.get("releaseId", String.class);
			result.staticScanDate = parseFoDDate(fodRelease.get("staticScanDate", String.class));
			result.dynamicScanDate = parseFoDDate(fodRelease.get("dynamicScanDate", String.class));
			result.mobileScanDate = parseFoDDate(fodRelease.get("mobileScanDate", String.class));
			return result;
		}
		
		private static final Date parseFoDDate(String dateString) {
			if ( dateString == null ) { return null; }
			try {
				return FMT_DATE.parse(StringUtils.substringBefore(dateString, "."));
			} catch ( ParseException e ) {
				throw new RuntimeException("Error parsing scan date "+dateString+" returned by FoD", e);
			}
		}
		
		public static final ScanStatus parse(String currentReleaseId, String syncStatusString) {
			ScanStatus result = new ScanStatus();
			try {
				if ( StringUtils.isNotBlank(syncStatusString) ) {
					result = MAPPER.readerForUpdating(result).readValue(syncStatusString);
				}
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Exception parsing FoD sync status", e);
			}
			if (result == null || currentReleaseId==null || !currentReleaseId.equals(result.releaseId)) {
				// Reset sync status if release id has changed
				result = new ScanStatus();
			}
			return result;
		}

		public final String asSyncStatusString() {
			try {
				return MAPPER.writeValueAsString(this);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Exception generating FoD sync status string", e);
			}
		}

		public final Date getScanDate(String scanType) {
			switch (scanType.toLowerCase()) {
			case "static": return getStaticScanDate();
			case "dynamic": return getDynamicScanDate();
			case "mobile": return getMobileScanDate();
			default: throw new RuntimeException("Unknown scan type "+scanType);
			}
		}

		public void setScanDate(String scanType, Date scanDate) {
			switch (scanType.toLowerCase()) {
			case "static": setStaticScanDate(scanDate); break;
			case "dynamic": setDynamicScanDate(scanDate); break;
			case "mobile": setMobileScanDate(scanDate); break;
			default: throw new RuntimeException("Unknown scan type "+scanType);
			}
		}
	}
	
	@Component @Data
	public static final class SSCSyncedApplicationVersionFilter {
		private String sscSyncScansUserName;
		
		@Autowired
		public SSCSyncedApplicationVersionFilter(ConfigSyncScansTask config) {
			this.sscSyncScansUserName = config.getSsc().getUserName();
		}
		
		@PostConstruct
		public void postConstruct() {
			FoDSyncAPI.filter = this;
		}
	}

}
