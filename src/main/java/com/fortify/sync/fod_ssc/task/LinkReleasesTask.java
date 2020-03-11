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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import com.fortify.client.fod.api.FoDApplicationAPI;
import com.fortify.client.fod.api.FoDReleaseAPI;
import com.fortify.client.fod.api.query.builder.AbstractFoDEntityQueryBuilder;
import com.fortify.client.fod.api.query.builder.AbstractFoDEntityQueryBuilder.IFoDEntityQueryBuilderParamFilter;
import com.fortify.client.fod.api.query.builder.FoDApplicationsQueryBuilder;
import com.fortify.client.fod.api.query.builder.FoDReleasesQueryBuilder;
import com.fortify.client.fod.connection.FoDAuthenticatingRestConnection;
import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.api.SSCAttributeAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.sync.fod_ssc.config.LinkReleasesTaskConfig;
import com.fortify.sync.fod_ssc.config.LinkReleasesTaskConfig.AbstractFoDQueryConfig;
import com.fortify.sync.fod_ssc.config.LinkReleasesTaskConfig.ConfigApplicationFilters;
import com.fortify.sync.fod_ssc.config.LinkReleasesTaskConfig.ConfigAutoCreate;
import com.fortify.sync.fod_ssc.config.LinkReleasesTaskConfig.ConfigReleaseFilters;
import com.fortify.sync.fod_ssc.config.LinkReleasesTaskConfig.OrderBy;
import com.fortify.sync.fod_ssc.connection.ssc.api.SyncAPI;
import com.fortify.sync.fod_ssc.connection.ssc.api.SyncAPI.LinkedVersionsAndReleasesIds;
import com.fortify.sync.fod_ssc.connection.ssc.api.SyncConfig;
import com.fortify.util.rest.json.JSONMap;
import com.fortify.util.rest.json.preprocessor.enrich.JSONMapEnrichWithValue;
import com.fortify.util.rest.json.preprocessor.filter.AbstractJSONMapFilter.MatchMode;
import com.fortify.util.rest.json.preprocessor.filter.JSONMapFilterSpEL;
import com.fortify.util.spring.expression.SimpleExpression;

/**
 * This task is responsible for automatically linking FoD releases to SSC application versions,
 * based on the configuration provided by {@link LinkReleasesTaskConfig}.
 * 
 * @author Ruud Senden
 *
 */
@Component
public class LinkReleasesTask extends AbstractScheduledTask<LinkReleasesTaskConfig> {
	private static final Logger LOG = LoggerFactory.getLogger(LinkReleasesTask.class);
	@Autowired private LinkReleasesTaskConfig config;
	@Autowired private FoDAuthenticatingRestConnection fodConn;
	@Autowired private SSCAuthenticatingRestConnection sscConn;
	@Autowired private IHasSyncableScanChecker hasSyncableScanChecker;
	
	/**
	 * Allow our superclass to access our configuration
	 */
	@Override
	protected LinkReleasesTaskConfig getConfig() {
		return config;
	}
	
	/**
	 * This method is called by our superclass based on the configured schedule. This method
	 * just constructs a new {@link FoDUnlinkedReleasesProcessor} instance that loads a
	 * {@link LinkedVersionsAndReleasesIds} instance, and then defers the actual work to 
	 * {@link FoDUnlinkedReleasesProcessor#processFoDApplications()}.
	 */
	protected void runTask() {
		new FoDUnlinkedReleasesProcessor().processFoDApplications();
	}

	/**
	 * This inner class processes applicable FoD releases that have not yet been linked
	 * to SSC application versions, by optionally creating new SSC application versions 
	 * and configuring the SSC application version to be synchronized with the 
	 * corresponding FoD releases. 
	 * 
	 * @author Ruud Senden
	 *
	 */
	private final class FoDUnlinkedReleasesProcessor {
		private final LinkedVersionsAndReleasesIds linkedVersionsAndReleasesIds;
		
		/**
		 * Constructor to initialize our {@link LinkedVersionsAndReleasesIds} instance
		 * used to look up which SSC application versions and FoD releases have already 
		 * been linked before.
		 */
		public FoDUnlinkedReleasesProcessor() {
			this.linkedVersionsAndReleasesIds = sscConn.api(SyncAPI.class).getLinkedVersionsAndReleasesIds();
		}
		
		/**
		 * Construct an {@link FoDApplicationsQueryBuilder} instance based on our
		 * configuration.
		 * 
		 * @return
		 */
		private final FoDApplicationsQueryBuilder getApplicationsQueryBuilder() {
			ConfigApplicationFilters applicationFilters = config.getFod().getFilters().getApplication();
			FoDApplicationsQueryBuilder qb = fodConn.api(FoDApplicationAPI.class).queryApplications().onDemandAll();
			addParamFilter(qb, applicationFilters);
			addFilterExpressions(qb, applicationFilters);
			return qb;
		}
		
		/**
		 * Construct an {@link FoDReleasesQueryBuilder} instance for the given application,
		 * based on our configuration.
		 * 
		 * @return
		 */
		private final FoDReleasesQueryBuilder getReleasesQueryBuilder(JSONMap application) {
			ConfigReleaseFilters releaseFilters = config.getFod().getFilters().getRelease();
			FoDReleasesQueryBuilder qb = fodConn.api(FoDReleaseAPI.class).queryReleases()
				.onDemandAll()
				.paramFilterAnd("applicationId", application.get("applicationId", String.class))
				.preProcessor(new JSONMapEnrichWithValue("application", application));
			addParamFilter(qb, releaseFilters);
			addFilterExpressions(qb, releaseFilters);
			addOnlyFirstFilter(qb, releaseFilters);
			return qb;
		}

		/**
		 * Add the FoD 'filter' query parameter
		 * @param qb
		 * @param queryConfig
		 */
		private final void addParamFilter(IFoDEntityQueryBuilderParamFilter<?> qb, AbstractFoDQueryConfig queryConfig) {
			qb.paramFilterAnd(queryConfig.getFodFilterParam());
		}
		
		/**
		 * Add the FoD client-side filter expressions if applicable
		 * @param qb
		 * @param queryConfig
		 */
		private final void addFilterExpressions(AbstractFoDEntityQueryBuilder<?> qb, AbstractFoDQueryConfig queryConfig) {
			if ( ArrayUtils.isNotEmpty(queryConfig.getFilterExpressions()) ) {
				for ( SimpleExpression expr : queryConfig.getFilterExpressions()) {
					qb.preProcessor(new JSONMapFilterSpEL(MatchMode.INCLUDE, expr));
				}
			}
		}
		
		/**
		 * Add query parameters and filters for processing only the first FoD release
		 * that matches the other filters.
		 * @param qb
		 * @param releaseFilters
		 */
		private final void addOnlyFirstFilter(FoDReleasesQueryBuilder qb, ConfigReleaseFilters releaseFilters) {
			OrderBy onlyFirst = releaseFilters.getOnlyFirst();
			if ( onlyFirst!=null ) {
				qb.maxResults(1)
					.paramOrderBy(onlyFirst.getOrderBy(), onlyFirst.getDirection());
			}
		}
		
		/**
		 * This method calls {@link #getApplicationsQueryBuilder()} to build an
		 * {@link FoDApplicationsQueryBuilder} instance, then invokes the 
		 * {@link #processFoDApplication(JSONMap)} method for each FoD application
		 * loaded by this {@link FoDApplicationsQueryBuilder} instance.
		 */
		private final void processFoDApplications() {
			LOG.debug("Loading applications");
			getApplicationsQueryBuilder().build().processAll(this::processFoDApplication);
		}
		
		/**
		 * This method calls {@link #getReleasesQueryBuilder()} to build an
		 * {@link FoDReleasesQueryBuilder} instance, then invokes the 
		 * {@link #processFoDRelease(JSONMap)} method for each FoD release
		 * loaded by this {@link FoDReleasesQueryBuilder} instance.
		 */
		private final void processFoDApplication(JSONMap application) {
			LOG.debug("Loading releases for application "+application.get("applicationName", String.class));
			getReleasesQueryBuilder(application).build().processAll(this::processFoDRelease);
		}
		
		/**
		 * For every FoD release matching the configured application and release filters, this
		 * method checks whether the given release has already been linked to an SSC application 
		 * version. If not, the {@link #processUnlinkedFoDRelease(JSONMap)} method will be called 
		 * to potentially link the given release to a new or existing SSC application version.
		 *  
		 * @param release
		 */
		private final void processFoDRelease(JSONMap release) {
			// We need to filter here instead of using a filtering pre-processor
			// on the FoDReleasesQueryBuilder; otherwise the 'onlyFirst' release 
			// filter would return the first release that hasn't been linked yet
			// on each task run, thereby linking an additional release on each task run
			if ( !linkedVersionsAndReleasesIds.getLinkedFoDReleaseIds().contains(release.get("releaseId", String.class)) ) {
				processUnlinkedFoDRelease(release);
			}
		}
	
		/**
		 * For the given FoD release, this method checks whether a similarly named SSC application version
		 * already exists. If so, the {@link #processUnlinkedFoDReleaseWithMatchingSSCApplicationVersion(JSONMap, JSONMap)}
		 * method is called, otherwise the {@link #processUnlinkedFoDReleaseWithoutMatchingSSCApplicationVersion(JSONMap)}
		 * method is called.
		 * 
		 * @param release
		 */
		private final void processUnlinkedFoDRelease(JSONMap release) {
			String fodApplicationName = getFoDApplicationName(release);
			String fodReleaseWithMicroserviceName = getFoDReleaseWithMicroserviceName(release);
			
			LOG.debug("Processing unlinked FoD release {}:{}", fodApplicationName, fodReleaseWithMicroserviceName);
			
			JSONMap sscApplicationVersion = sscConn.api(SSCApplicationVersionAPI.class).getApplicationVersionByName(fodApplicationName, fodReleaseWithMicroserviceName, false);
			if ( sscApplicationVersion==null ) {
				processUnlinkedFoDReleaseWithoutMatchingSSCApplicationVersion(release);
			} else {
				processUnlinkedFoDReleaseWithMatchingSSCApplicationVersion(release, sscApplicationVersion);
			}
		}

		private final String getFoDReleaseWithMicroserviceName(JSONMap release) {
			String fodReleaseName = getFoDReleaseName(release);
			String fodMicroserviceName = getFoDMicroserviceName(release);
			return StringUtils.isBlank(fodMicroserviceName) ? fodReleaseName : String.format("%s-%s", fodMicroserviceName, fodReleaseName);
		}

		private final String getFoDMicroserviceName(JSONMap release) {
			return release.getPath("microserviceName", String.class);
		}

		private final String getFoDReleaseName(JSONMap release) {
			return release.getPath("releaseName", String.class);
		}

		private final String getFoDApplicationName(JSONMap release) {
			return release.getPath("application.applicationName", String.class);
		}

		/**
		 * If auto-create SSC versions is disabled, this method will simply log a message and return. If
		 * auto-create SSC versions is enabled, this method will call {@link #createLinkedSSCApplicationVersion(String, String, String)}
		 * to actually create the application version. 
		 * @param release
		 */
		private final void processUnlinkedFoDReleaseWithoutMatchingSSCApplicationVersion(JSONMap release) {
			String fodApplicationName = getFoDApplicationName(release);
			String fodReleaseWithMicroserviceName = getFoDReleaseWithMicroserviceName(release);
			
			if ( !config.getSsc().getAutoCreateVersions().isEnabled() ) {
				LOG.debug("SSC application version creation disabled; not creating SSC application version {}:{} for unlinked FoD release", fodApplicationName, fodReleaseWithMicroserviceName);
			} else if ( config.getSsc().getAutoCreateVersions().isCreateOnlyIfSyncableScans() && !hasSyncableScans(release) ) {
				LOG.debug("FoD release has no syncable scans; not creating SSC application version {}:{} for unlinked FoD release", fodApplicationName, fodReleaseWithMicroserviceName);
			} else {
				LOG.debug("Creating SSC application version {}:{} for unlinked FoD release", fodApplicationName, fodReleaseWithMicroserviceName);
				createLinkedSSCApplicationVersion(fodApplicationName, fodReleaseWithMicroserviceName, release.get("releaseId", String.class));
			}
		}

		private boolean hasSyncableScans(JSONMap release) {
			boolean result = false;
			for ( String fodScanType : config.getSsc().getAutoCreateVersions().getEnabledFoDScanTypes() ) {
				result |= hasSyncableScanChecker.hasSyncableScan(release, fodScanType);
			}
			return result;
		}

		/**
		 * Create a new SSC application version with the given application and version name, 
		 * and linked to the given FoD release id.
		 *  
		 * @param sscApplicationName
		 * @param sscVersionName
		 * @param linkedFoDReleaseId
		 */
		private void createLinkedSSCApplicationVersion(String sscApplicationName, String sscVersionName, String linkedFoDReleaseId) {
			ConfigAutoCreate autoCreateVersionsConfig = config.getSsc().getAutoCreateVersions();
			sscConn.api(SSCApplicationVersionAPI.class).createApplicationVersion()
				.applicationName(sscApplicationName).versionName(sscVersionName)
				.versionDescription("Automatically created for FoD Release")
				.autoAddRequiredAttributes(true)
				.attributes(getSyncConfigAttributesMap(linkedFoDReleaseId))
				.issueTemplateName(autoCreateVersionsConfig.getIssueTemplateName())
				.execute();
		}

		/**
		 * If the given SSC application version is not yet linked to any FoD release, this method will
		 * link the two. Otherwise, if the given application version is already linked to a different
		 * FoD release, a warning message will be logged without any further action.  
		 * @param release
		 * @param sscApplicationVersion
		 */
		private final void processUnlinkedFoDReleaseWithMatchingSSCApplicationVersion(JSONMap release, JSONMap sscApplicationVersion) {
			String fodReleaseId = release.get("releaseId", String.class);
			String sscApplicationVersionId = sscApplicationVersion.get("id", String.class);
			if ( linkedVersionsAndReleasesIds.getLinkedSSCApplicationVersionIds().contains(sscApplicationVersionId)) {
				LOG.warn("SSC application version id {} matches FoD release id {}, but release is already linked to another SSC version",
					sscApplicationVersionId, fodReleaseId);
			} else {
				LOG.debug("Linking existing SSC application version id {} to FoD release id {}", sscApplicationVersionId, fodReleaseId);
				sscConn.api(SSCAttributeAPI.class).updateApplicationVersionAttributes(
						sscApplicationVersionId, getSyncConfigAttributesMap(fodReleaseId));
			}
			
		}

		/**
		 * Get the SSC application version attributes map representing the {@link SyncConfig}
		 * properties.
		 * 
		 * @param fodReleaseId
		 * @return
		 */
		private final MultiValueMap<String, Object> getSyncConfigAttributesMap(String fodReleaseId) {
			return new SyncConfig(fodReleaseId, config.getSsc().getAutoCreateVersions().getEnabledFoDScanTypes()).asAttributesMap();
		}
	}
	
}
