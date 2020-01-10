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
import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.api.SSCApplicationVersionAPI.CreateApplicationVersionBuilder;
import com.fortify.client.ssc.api.SSCAttributeAPI;
import com.fortify.sync.fod_ssc.config.LinkReleasesTaskConfig;
import com.fortify.sync.fod_ssc.config.LinkReleasesTaskConfig.ConfigApplicationFilters;
import com.fortify.sync.fod_ssc.config.LinkReleasesTaskConfig.ConfigAutoCreate;
import com.fortify.sync.fod_ssc.config.LinkReleasesTaskConfig.ConfigReleaseFilters;
import com.fortify.sync.fod_ssc.config.LinkReleasesTaskConfig.OrderBy;
import com.fortify.sync.fod_ssc.util.SyncHelper;
import com.fortify.sync.fod_ssc.util.SyncHelper.LinkedVersionsAndReleasesIds;
import com.fortify.util.rest.json.JSONMap;
import com.fortify.util.rest.json.preprocessor.enrich.JSONMapEnrichWithValue;
import com.fortify.util.rest.json.preprocessor.filter.AbstractJSONMapFilter.MatchMode;
import com.fortify.util.rest.json.preprocessor.filter.JSONMapFilterSpEL;
import com.fortify.util.spring.expression.SimpleExpression;

@Component
public class LinkReleasesTask extends AbstractScheduledTask {
	private static final Logger LOG = LoggerFactory.getLogger(LinkReleasesTask.class);
	private final SyncHelper syncHelper;
	private final LinkReleasesTaskConfig config;
	
	@Autowired
	public LinkReleasesTask(LinkReleasesTaskConfig config, SyncHelper syncHelper) {
		super(config);
		this.config = config;
		this.syncHelper = syncHelper;
		LOG.info("linkReleases task configuration: {}", config);
	}
	
	public void runTask() {
		new FoDUnlinkedReleasesProcessor().processFoDApplications();
	}
	
	@Override
	protected String getTaskName() {
		return "linkReleases";
	}

	private final class FoDUnlinkedReleasesProcessor {
		private final LinkedVersionsAndReleasesIds linkedVersionsAndReleasesIds = syncHelper.getLinkedVersionsAndReleasesIds();
		
		private void processFoDApplications() {
			LOG.debug("Loading applications");
			ConfigApplicationFilters applicationFilters = config.getFod().getFilters().getApplication();
			FoDApplicationsQueryBuilder qb = syncHelper.getFodConn().api(FoDApplicationAPI.class).queryApplications()
					.onDemandAll()
					.paramFilterAnd(applicationFilters.getFodFilterParam());
			if ( applicationFilters.getFilterExpressions()!=null ) {
				// TODO qb.paramFields(<id + name + all fields referenced by filter expressions>) 
				for ( SimpleExpression expr : applicationFilters.getFilterExpressions()) {
					qb.preProcessor(new JSONMapFilterSpEL(MatchMode.INCLUDE, expr));
				}
			}
			qb.build().processAll(this::processFoDApplication);
		}
		
		private void processFoDApplication(JSONMap application) {
			ConfigReleaseFilters releaseFilters = config.getFod().getFilters().getRelease();
			FoDReleasesQueryBuilder qb = syncHelper.getFodConn().api(FoDReleaseAPI.class).queryReleases()
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
				// TODO qb.paramFields(<id + name + fields referenced by filter expressions>) 
				for ( SimpleExpression expr : releaseFilters.getFilterExpressions()) {
					qb.preProcessor(new JSONMapFilterSpEL(MatchMode.INCLUDE, expr));
				}
			}
			qb.build().processAll(this::processFoDRelease);
		}
		
		private void processFoDRelease(JSONMap release) {
			// We need to filter here instead of using a filtering pre-processor
			// on the FoDReleasesQueryBuilder; otherwise the 'onlyFirst' release 
			// filter would return the first release that hasn't been linked yet
			// on each task run.
			if ( !linkedVersionsAndReleasesIds.getLinkedFoDReleaseIds().contains(release.get("releaseId", String.class)) ) {
				processUnlinkedFoDRelease(release);
			}
		}
	
		private void processUnlinkedFoDRelease(JSONMap release) {
			String fodApplicationName = release.getPath("application.applicationName", String.class);
			String fodReleaseName = release.getPath("releaseName", String.class);
			LOG.debug("Processing unlinked FoD release {}:{}", release.getPath("application.applicationName",String.class), release.getPath("releaseName",String.class));
			
			JSONMap sscApplicationVersion = syncHelper.getSscConn().api(SSCApplicationVersionAPI.class).getApplicationVersionByName(fodApplicationName, fodReleaseName, false);
			if ( sscApplicationVersion==null ) {
				processprocessUnlinkedFoDReleaseWithoutMatchingSSCApplicationVersion(release);
			} else {
				processprocessUnlinkedFoDReleaseWithMatchingSSCApplicationVersion(release, sscApplicationVersion);
			}
		}

		private void processprocessUnlinkedFoDReleaseWithoutMatchingSSCApplicationVersion(JSONMap release) {
			ConfigAutoCreate autoCreateVersionsConfig = config.getSsc().getAutoCreateVersions();
			String fodApplicationName = release.getPath("application.applicationName", String.class);
			String fodReleaseName = release.getPath("releaseName", String.class);
			String fodReleaseId = release.get("releaseId", String.class);
			
			if ( !autoCreateVersionsConfig.isEnabled() ) {
				LOG.debug("SSC application version creation disabled; not creating SSC application version {}:{} for unlinked FoD release", fodApplicationName, fodReleaseName);
			} else {
				LOG.debug("Creating SSC application version {}:{} for unlinked FoD release", fodApplicationName, fodReleaseName);
	
				CreateApplicationVersionBuilder createVersionBuilder = syncHelper.getSscConn().api(SSCApplicationVersionAPI.class).createApplicationVersion()
					.applicationName(fodApplicationName).versionName(fodReleaseName)
					.versionDescription("Automatically created for FoD Release")
					.autoAddRequiredAttributes(true)
					// TODO Remove duplication with #getAttributesMap
					// TODO Get all allowed options for 'Include Scan Types' from SSC
					// TODO Get default issue template from SSC, allow override in config?
					.attribute("FoD Sync - Release Id", fodReleaseId)
					.issueTemplateName(autoCreateVersionsConfig.getIssueTemplateName());
				
				for (String scanType : autoCreateVersionsConfig.getEnabledFoDScanTypes()) {
					createVersionBuilder.attribute("FoD Sync - Include Scan Types", scanType);
				}
				
				createVersionBuilder.execute();
			}
		}

		private void processprocessUnlinkedFoDReleaseWithMatchingSSCApplicationVersion(JSONMap release, JSONMap sscApplicationVersion) {
			String fodReleaseId = release.get("releaseId", String.class);
			String sscApplicationVersionId = sscApplicationVersion.get("id", String.class);
			if ( linkedVersionsAndReleasesIds.getLinkedSSCApplicationVersionIds().contains(sscApplicationVersionId)) {
				LOG.warn("SSC application version id %s matches FoD release id %s, but release is already linked to another SSC version",
					sscApplicationVersionId, fodReleaseId);
			} else {
				LOG.debug("Linking existing SSC application version id {} to FoD release id {}", sscApplicationVersionId, fodReleaseId);
				syncHelper.getSscConn().api(SSCAttributeAPI.class).updateApplicationVersionAttributes(
						sscApplicationVersionId, getAttributesMap(fodReleaseId));
			}
			
		}

		private MultiValueMap<String, Object> getAttributesMap(String fodReleaseId) {
			MultiValueMap<String,Object> attributes = new LinkedMultiValueMap<>();
			attributes.add("FoD Sync - Release Id", fodReleaseId);
			attributes.addAll("FoD Sync - Include Scan Types", Arrays.asList(config.getSsc().getAutoCreateVersions().getEnabledFoDScanTypes()));
			return attributes;
		}
	}
	
}
