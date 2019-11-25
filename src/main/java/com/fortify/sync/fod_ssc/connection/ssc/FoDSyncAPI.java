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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.client.fod.api.FoDReleaseAPI;
import com.fortify.client.fod.connection.FoDAuthenticatingRestConnection;
import com.fortify.client.ssc.api.AbstractSSCAPI;
import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.api.SSCAttributeAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.sync.fod_ssc.util.DefaultObjectMapperFactory;
import com.fortify.util.rest.json.JSONMap;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public class FoDSyncAPI extends AbstractSSCAPI {
	private static final Logger LOG = LoggerFactory.getLogger(FoDSyncAPI.class);
	private static final ObjectMapper MAPPER = DefaultObjectMapperFactory.getDefaultObjectMapper();
	
	public FoDSyncAPI(SSCAuthenticatingRestConnection conn) {
		super(conn);
	}
	
	public final void updateSyncStatus(String sscApplicationVersionId, ScanStatus scanStatus) {
		if ( scanStatus.isModified() ) {
			LOG.debug("Updating sync status for application version id {}", sscApplicationVersionId);
			MultiValueMap<String, Object> attributes = new LinkedMultiValueMap<>();
			attributes.add("FoD Sync - Status", scanStatus.asSyncStatusString());
			conn().api(SSCAttributeAPI.class)
				.updateApplicationVersionAttributes(sscApplicationVersionId, attributes);
		}
	}
	
	public void processSyncedApplicationVersions(final Consumer<SyncData> consumer) {
		conn().api(SSCApplicationVersionAPI.class)
			.queryApplicationVersions()
			.paramFields("id", "name", "project")
			.onDemandAttributeValuesByName()
			.build().processAll(json->processSyncedApplicationVersion(consumer, json));
	}

	private void processSyncedApplicationVersion(final Consumer<SyncData> consumer, JSONMap json) {
		SyncData syncData = new SyncData(json);
		if ( syncData.isSyncEnabled() ) {
			consumer.accept(syncData);
		}
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
	
	@Data
	public static final class SyncData {
		// TODO add application/version name?
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
			this.scanStatus = ScanStatus.parse(attributeValuesByName.get("FoD Sync - Status", String.class));
		}
		
		public boolean isSyncEnabled() {
			return StringUtils.isNotBlank(this.fodReleaseId) 
					&& includedScanTypes.length > 0;
		}
	}
	
	@Data
	public static final class ScanStatus {
		@JsonProperty private String fodReleaseId;
		@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) 
		@JsonProperty private Map<String,Date> scanDates = new HashMap<>();
		@JsonIgnore private boolean modified;
		
		public static final ScanStatus parse(String syncStatusString) {
			ScanStatus result = new ScanStatus();
			try {
				if ( StringUtils.isNotBlank(syncStatusString) ) {
					result = MAPPER.readerForUpdating(result).readValue(syncStatusString);
					result.modified = false;
				}
			} catch (JsonProcessingException e) {
				LOG.warn("Sync Status cannot be parsed; FPR files will be re-synced");
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
		
		public final ScanStatus newIfDifferentFoDReleaseId(String fodReleaseId) {
			if ( Objects.equals(this.fodReleaseId, fodReleaseId) ) {
				return this;
			} else {
				if ( this.fodReleaseId!=null ) {
					LOG.warn("Linked FoD Release Id has changed since last sync, ignoring previous scan status");
				}
				ScanStatus result = new ScanStatus();
				result.setFoDReleaseId(fodReleaseId);
				return result;
			}
		}
		
		public final void setFoDReleaseId(String fodReleaseId) {
			if ( !Objects.equals(this.fodReleaseId, fodReleaseId) ) {
				this.modified = true;
				this.fodReleaseId = fodReleaseId;
			}
		}

		public final Date getScanDate(String scanType) {
			return this.scanDates.get(scanType.toLowerCase());
		}

		public void setScanDate(String scanType, Date scanDate) {
			if ( !Objects.equals(getScanDate(scanType), scanDate)) {
				this.modified = true;
				this.scanDates.put(scanType.toLowerCase(), scanDate);
			}
		}
	}
}
