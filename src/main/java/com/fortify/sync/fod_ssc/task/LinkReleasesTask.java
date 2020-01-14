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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fortify.client.fod.api.FoDApplicationAPI;
import com.fortify.client.fod.api.FoDReleaseAPI;
import com.fortify.client.fod.api.query.builder.FoDApplicationsQueryBuilder;
import com.fortify.client.fod.api.query.builder.FoDReleasesQueryBuilder;
import com.fortify.client.fod.connection.FoDAuthenticatingRestConnection;
import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.api.SSCApplicationVersionAPI.CreateApplicationVersionBuilder;
import com.fortify.client.ssc.api.SSCAttributeAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.sync.fod_ssc.config.LinkReleasesTaskConfig;
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
		 * Construct an {@link FoDApplicationsQueryBuilder} instance based on our
		 * configuration.
		 * 
		 * @return
		 */
		private final FoDApplicationsQueryBuilder getApplicationsQueryBuilder() {
			ConfigApplicationFilters applicationFilters = config.getFod().getFilters().getApplication();
			FoDApplicationsQueryBuilder qb = fodConn.api(FoDApplicationAPI.class).queryApplications()
					.onDemandAll()
					.paramFilterAnd(applicationFilters.getFodFilterParam());
			if ( applicationFilters.getFilterExpressions()!=null ) {
				for ( SimpleExpression expr : applicationFilters.getFilterExpressions()) {
					qb.preProcessor(new JSONMapFilterSpEL(MatchMode.INCLUDE, expr));
				}
				// TODO (performance improvement) call qb.paramFields to have FoD return only fields referenced by the filter expressions 
			}
			return qb;
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
				.paramFilterAnd(releaseFilters.getFodFilterParam())
				.preProcessor(new JSONMapEnrichWithValue("application", application));
			OrderBy onlyFirst = releaseFilters.getOnlyFirst();
			if ( onlyFirst!=null ) {
				qb.maxResults(1)
					.paramOrderBy(onlyFirst.getOrderBy(), onlyFirst.getDirection());
			}
			if ( releaseFilters.getFilterExpressions()!=null ) {
				for ( SimpleExpression expr : releaseFilters.getFilterExpressions()) {
					qb.preProcessor(new JSONMapFilterSpEL(MatchMode.INCLUDE, expr));
				}
				// TODO (performance improvement) call qb.paramFields to have FoD return only fields referenced by the filter expressions
			}
			return qb;
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
			// on each task run.
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
			String fodApplicationName = release.getPath("application.applicationName", String.class);
			String fodReleaseName = release.getPath("releaseName", String.class);
			LOG.debug("Processing unlinked FoD release {}:{}", release.getPath("application.applicationName",String.class), release.getPath("releaseName",String.class));
			
			JSONMap sscApplicationVersion = sscConn.api(SSCApplicationVersionAPI.class).getApplicationVersionByName(fodApplicationName, fodReleaseName, false);
			if ( sscApplicationVersion==null ) {
				processUnlinkedFoDReleaseWithoutMatchingSSCApplicationVersion(release);
			} else {
				processUnlinkedFoDReleaseWithMatchingSSCApplicationVersion(release, sscApplicationVersion);
			}
		}

		/**
		 * If auto-create SSC versions is disabled, this method will simply log a message and return. If
		 * auto-create SSC versions is enabled, this method will call {@link #createLinkedSSCApplicationVersion(String, String, String)}
		 * to actually create the application version. 
		 * @param release
		 */
		private final void processUnlinkedFoDReleaseWithoutMatchingSSCApplicationVersion(JSONMap release) {
			String fodApplicationName = release.getPath("application.applicationName", String.class);
			String fodReleaseName = release.getPath("releaseName", String.class);
			
			if ( !config.getSsc().getAutoCreateVersions().isEnabled() ) {
				LOG.debug("SSC application version creation disabled; not creating SSC application version {}:{} for unlinked FoD release", fodApplicationName, fodReleaseName);
			} else {
				LOG.debug("Creating SSC application version {}:{} for unlinked FoD release", fodApplicationName, fodReleaseName);
				createLinkedSSCApplicationVersion(fodApplicationName, fodReleaseName, release.get("releaseId", String.class));
			}
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
			CreateApplicationVersionBuilder createVersionBuilder = sscConn.api(SSCApplicationVersionAPI.class).createApplicationVersion()
				.applicationName(sscApplicationName).versionName(sscVersionName)
				.versionDescription("Automatically created for FoD Release")
				.autoAddRequiredAttributes(true)
				// TODO Remove duplication with #getAttributesMap
				// TODO Get all allowed options for 'Include Scan Types' from SSC
				// TODO Get default issue template from SSC, allow override in config?
				.attribute(SyncConfig.SSC_ATTR_FOD_RELEASE_ID, linkedFoDReleaseId)
				.issueTemplateName(autoCreateVersionsConfig.getIssueTemplateName());
			
			for (String scanType : autoCreateVersionsConfig.getEnabledFoDScanTypes()) {
				// TODO Remove duplication with #getAttributesMap
				createVersionBuilder.attribute(SyncConfig.SSC_ATTR_INCLUDE_FOD_SCAN_TYPES, scanType);
			}
			
			createVersionBuilder.execute();
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
				LOG.warn("SSC application version id %s matches FoD release id %s, but release is already linked to another SSC version",
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
		// TODO Merge implementations for setting sync attrs for both 'update existing' and 
		// 'create new' SSC application version into a single implementation
		private final MultiValueMap<String, Object> getSyncConfigAttributesMap(String fodReleaseId) {
			MultiValueMap<String,Object> attributes = new LinkedMultiValueMap<>();
			attributes.add(SyncConfig.SSC_ATTR_FOD_RELEASE_ID, fodReleaseId);
			attributes.addAll(SyncConfig.SSC_ATTR_INCLUDE_FOD_SCAN_TYPES, Arrays.asList(config.getSsc().getAutoCreateVersions().getEnabledFoDScanTypes()));
			return attributes;
		}
	}
	
}
