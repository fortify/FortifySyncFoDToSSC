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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.fortify.client.ssc.api.AbstractSSCAPI;
import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.api.SSCAttributeDefinitionAPI.SSCAttributeDefinitionHelper;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.util.rest.json.JSONMap;

import lombok.Data;

/**
 * This {@link AbstractSSCAPI} implementation provides access to synchronization
 * data stored as SSC application version attributes.
 * 
 * @author Ruud Senden
 *
 */
public final class SyncAPI extends AbstractSSCAPI {
	/**
	 * Configure SSC connection
	 * @param conn
	 */
	public SyncAPI(SSCAuthenticatingRestConnection conn) {
		super(conn);
	}
	
	/**
	 * Query which SSC application versions are currently linked to an FoD release,
	 * and which FoD releases are currently linked to an SSC application version.
	 * @return
	 */
	public final LinkedVersionsAndReleasesIds getLinkedVersionsAndReleasesIds(SSCAttributeDefinitionHelper attributeDefinitionHelper) {
		LinkedVersionsAndReleasesIds result = new LinkedVersionsAndReleasesIds();
		processSyncData(attributeDefinitionHelper, result, SyncConfigPredicate.IS_LINKED);
		return result;
	}
	
	/**
	 * This method provides the following functionality:
	 * <ol>
	 *  <li>Query all SSC application versions for their id's and attribute values by name</li>
	 *  <li>Process each application version using a {@link SyncDataConsumerWrapper} 
	 *      configured with the given {@link SyncData} {@link Consumer} and 
	 *      {@link SyncConfigPredicate}</li>
	 *  <li>
	 * </ol>
	 * Effectively this means that the given {@link SyncData} {@link Consumer} will be called 
	 * for every application version for which the given {@link SyncConfigPredicate} is true.
	 * 
	 * @param syncDataConsumer
	 * @param predicate
	 */
	public final void processSyncData(SSCAttributeDefinitionHelper attributeDefinitionHelper, final Consumer<SyncData> syncDataConsumer, SyncConfigPredicate predicate) {
		conn().api(SSCApplicationVersionAPI.class)
			.queryApplicationVersions()
			.paramFields("id")
			.embedAttributeValuesByName(attributeDefinitionHelper)
			.build().processAll(new SyncDataConsumerWrapper(syncDataConsumer, predicate));
	}
	
	/**
	 * This enumeration provides various predicates based on sync configuration.
	 * @author Ruud Senden
	 *
	 */
	public static enum SyncConfigPredicate {
		ALL(x -> true),
		IS_LINKED(x -> x.isLinked()),
		IS_SYNC_ENABLED(x -> x.isLinked());
		
		private final Predicate<SyncConfig> predicate;
		private SyncConfigPredicate(Predicate<SyncConfig> predicate) {
			this.predicate = predicate;
		}
		public boolean isIncluded(SyncConfig syncConfig) {
			return predicate.test(syncConfig);
		}
	}
	
	/**
	 * This data class provides access to the following data:
	 * <ul>
	 *  <li>Set of all SSC application version id's that are currently linked to an FoD release</li>
	 *  <li>Set of all FoD release id's that are currently linked to an SSC application version</li>
	 * </ul>
	 * @author Ruud Senden
	 *
	 */
	@Data
	public static final class LinkedVersionsAndReleasesIds implements Consumer<SyncData> {
		private final Set<String> linkedSSCApplicationVersionIds = new HashSet<>();
		private final Set<String> linkedFoDReleaseIds = new HashSet<>();
		
		/**
		 * Private constructor to disallow external instantiation
		 */
		private LinkedVersionsAndReleasesIds() {}

		/**
		 * For each synced application version, this method stores the SSC 
		 * application version id and FoD release id for later look-up.
		 * 
		 * @param syncData
		 */
		@Override
		public void accept(SyncData syncData) {
			SyncConfig syncConfig = syncData.getSyncConfig();
			linkedFoDReleaseIds.add(syncConfig.getFodReleaseId());
			linkedSSCApplicationVersionIds.add(syncData.getSSCApplicationVersionId());
		}
	}
	
	/**
	 * Simple {@link Consumer} implementation that performs the following:
	 * <ol>
	 *  <li>Construct a {@link SyncData} instance for every consumed {@link JSONMap}
	 *      that represents an SSC application version</li>
	 *  <li>Check whether the configured predicate returns true for the {@link SyncData}
	 *      instance</li>
	 *  <li>If the predicate returns true, invoke the configured {@link SyncData} {@link Consumer}</li>
	 * </ol>
	 * @author Ruud Senden
	 *
	 */
	private static final class SyncDataConsumerWrapper implements Consumer<JSONMap> {
		private final Consumer<SyncData> syncDataConsumer;
		private final SyncConfigPredicate predicate;

		public SyncDataConsumerWrapper(Consumer<SyncData> syncDataConsumer, SyncConfigPredicate predicate) {
			this.predicate = predicate;
			this.syncDataConsumer = syncDataConsumer;
		}
		
		@Override
		public void accept(JSONMap sscApplicationVersion) {
			SyncData syncData = new SyncData(sscApplicationVersion);
			if ( predicate.isIncluded(syncData.getSyncConfig()) ) {
				syncDataConsumer.accept(syncData);
			}
		}
	}
}
