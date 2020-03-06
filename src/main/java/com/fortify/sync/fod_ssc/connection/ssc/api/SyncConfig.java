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
package com.fortify.sync.fod_ssc.connection.ssc.api;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fortify.util.rest.json.JSONMap;

import lombok.Data;

/**
 * This class holds the current sync configuration for an SSC application version. This includes the 
 * following information:
 * <ul>
 *  <li>The SSC application version id</li>
 *  <li>The linked FoD release id</li>
 *  <li>The scan types to be synchronized</li>
 * </ul>
 * Apart from the actual sync status data, this class provides various methods for loading the 
 * current configuration from an SSC application version.  
 *  
 * @author Ruud Senden
 *
 */
@Data
public final class SyncConfig {
	private static final String SSC_ATTR_INCLUDE_FOD_SCAN_TYPES = "XFoD Sync - Include Scan Types";
	private static final String SSC_ATTR_FOD_RELEASE_ID = "FoD Sync - Release Id";
	static final String[] SSC_REQUIRED_ATTRS = {SSC_ATTR_FOD_RELEASE_ID, SSC_ATTR_INCLUDE_FOD_SCAN_TYPES};
	private final String fodReleaseId;
	private final String[] includedScanTypes;
	
	/**
	 * Constructor for creating a new instance with the given FoD release id and included scan types.
	 * 
	 * @param fodReleaseId
	 * @param includedScanTypes
	 */
	public SyncConfig(String fodReleaseId, String[] includedScanTypes) {
		this.fodReleaseId = fodReleaseId;
		this.includedScanTypes = includedScanTypes;
	}
	
	/**
	 * Public static method to get an instance of this class for the given
	 * SSC application version instance.
	 * 
	 * @param sscApplicationVersion
	 * @return
	 */
	public static final SyncConfig getFromApplicationVersion(JSONMap sscApplicationVersion) {
		JSONMap attributeValuesByName = sscApplicationVersion.get("attributeValuesByName", JSONMap.class);
		String fodReleaseId = attributeValuesByName.get(SSC_ATTR_FOD_RELEASE_ID, String.class);
		String[] includedScanTypes = attributeValuesByName.getOrCreateJSONList(SSC_ATTR_INCLUDE_FOD_SCAN_TYPES).toArray(new String[]{});
		return new SyncConfig(fodReleaseId, includedScanTypes);
	}
	
	/**
	 * Get the current configuration as an SSC application version attributes map 
	 * @return
	 */
	public final MultiValueMap<String,Object> asAttributesMap() {
		MultiValueMap<String,Object> attributes = new LinkedMultiValueMap<>();
		attributes.add(SSC_ATTR_FOD_RELEASE_ID, fodReleaseId);
		attributes.addAll(SSC_ATTR_INCLUDE_FOD_SCAN_TYPES, Arrays.asList(includedScanTypes));
		return attributes;
	}
	
	/**
	 * Indicate whether the current application version is currently linked to
	 * an FoD release.
	 * @return
	 */
	public boolean isLinked() {
		return StringUtils.isNotBlank(this.fodReleaseId);
	}
	
	/**
	 * Indicate whether sync is enabled for the current application version.
	 * @return
	 */
	public boolean isSyncEnabled() {
		return isLinked() && includedScanTypes.length > 0;
	}
}