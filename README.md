# Fortify FoD-SSC Sync

## Work in progress

At the moment, only initial beta versions are provided; no official release is
available yet. Recent beta versions are mostly functional, but have not been thoroughly tested.
See https://github.com/fortify-ps/FortifySyncFoDToSSC/issues for a list of current issues and
ideas for future improvements.

## Prerequisites

The following sections describe various prerequisites for running the sync utility.

### SSC Preparations

The following SSC preparations are required to use this utility:

* (Yet to be automated) Define the following SSC application version attributes. All attributes should have 'Scope' set to 'Application Version', and 'Category' set to 'Organization':
    * FoD Sync - Release Id: Integer
    * FoD Sync - Include Scan Types: List of Values - Multiple Selection
        * Static
        * Dynamic
        * ~~Mobile~~ (Currently not supported by FoD API)
    * FoD Sync - Status: Text - Multiple Lines, Hidden
    
* Define the user account to be used by the utility to connect with SSC. This user must
  have a role with the following permissions:
    
    * Universal access
    * Add application versions
    * ~~Approve analysis results upload? (do we want to auto-approve?)~~
    * ~~Delete application versions? (do we want to auto-delete if release in FoD is deleted?)~~
    * Edit application versions? (do we need this to set custom attributes, access, ... while creating app version?)
    * ~~Manage application version access? (do we need this if we specify users while creating version?)~~
    * ~~Manage attribute definitions: to auto-create FoD Sync-related attributes~~ (not yet implemented)
    * Upload analysis results
    * View application versions
    * View attribute definitions
    * View issue templates, process templates, ... (is this necessary to determine default issue template?)

### FoD Preparations

The following SSC preparations are required to use this utility:

* Define the user account to be used by the utility to connect with FoD. This user must have permissions to view all applications and releases.

## Download, configure and run the utility

The following sections describe where the utility can be downloaded, how to configure
the utility, and how to run the utility.

### Downloads

Both beta versions and release versions can be downloaded from https://bintray.com/fortify-ps/binaries/FortifySyncFoDToSSC. Release version may also
be posted on https://github.com/fortify-ps/FortifySyncFoDToSSC/releases. 

Note that beta versions are automatically published whenever there are code changes;
there is no guarantee that these versions are functional, and documentation may not 
be up to date.

Every beta version build consists of the following three artifacts: 
* `FortifySyncFoDToSSC-<version>-beta-<date>-<time>.jar`
* `FortifySyncFoDToSSC-<version>-beta-<date>-<time>-licenseReport.zip`
* `FortifySyncFoDToSSC-<version>-beta-<date>-<time>-dependencySources.zip`

For each beta version, multiple builds may be available (with different date/time stamps). If you 
are unsuccessful with a specific beta version build, you may want to try another build (if available).
To identify the latest available beta version build, you may want to sort by the `Updated` column
on the Bintray Files page.

Release versions are named similarly but without date/time stamps, as there is only one build for each release version:
* `FortifySyncFoDToSSC-<version>-release.jar`
* `FortifySyncFoDToSSC-<version>-release-licenseReport.zip`
* `FortifySyncFoDToSSC-<version>-release-dependencySources.zip`

In order to run the utility, only the `.jar` file needs to be downloaded. The license report 
and dependency sources zip files are only provided to comply with 3rd-party licensing requirements.
 

### Utility Configuration File

The utility requires a configuration file named `config.yml` to be available in a directory 
named  `<fortify.home>/FortifySyncFoDToSSC/`. Here, the `<fortify.home>` directory 
can be an existing Fortify SSC or SCA home directory, or you can use a separate home directory for 
this utility. The utility uses it's own utility-specific sub-directory in the `<fortify.home>`
directory, and doesn't share any configuration or data with any of the other Fortify products.

The utility will use the following methods to locate the fortify.home directory, in this order:
1. Specified on the Java command line with `-Dfortify.home=[directory]`
2. Specified as an environment variable `FORTIFY_HOME=[directory]`
3. Default value `~/.fortify`

Following is an example configuration file:

```yaml
# Override the default log level from INFO to DEBUG
logging:
  level:
    com.fortify.sync.fod_ssc: DEBUG
#    org.apache.http: TRACE
#    org.glassfish.jersey: TRACE

sync:
  # Define FoD and SSC connections
  connections:
    fod:
      baseUrl: https://emea.fortify.com # Use appropriate ams/apj/emea domain
      tenant: ${FOD_TENANT} # Get from environment variable
      userName: ${FOD_USER} # Get from environment variable
      password: ${FOD_PWD}  # Get from environment variable
      
      # Configure the number of retries if FoD rate limit is exceeded
      # If userName configured above is only used for this integration,
      # this can be left at the default (1). If userName is also used
      # by other integrations, you may want to increase this to retry
      # multiple times in case the other integration is assigned a free 
      # rate limit slot
      #rateLimitMaxRetries: 1
      
      # Configure the proxy if necessary
      #proxy:
      #  url: {proxy URL}
      #  userName: {optional proxy user}
      #  password: {password for proxy user}
    ssc:
      baseUrl: ${SSC_URL} # Get from environment variable
      userName: ${SSC_USER} # Get from environment variable
      password: ${SSC_PWD} # Get from environment variable
  
  # Configuration for the various tasks
  tasks:
    
    # Configuration for syncScans task
    syncScans:
      # Configure the schedule for running the syncScans task.
      # This example runs every minute at the 30-second mark; 
      # you probably want to reduce this for production runs.
      cronSchedule: '30 * * * * *'
      
      # Configure how long to keep scans downloaded from FoD, mainly
      # for debugging purposes. Default value is 0, meaning scans will 
      # be deleted immediately after uploading to SSC. If set to a 
      # non-zero number, any matching scans will be deleted whenever
      # the syncScans task runs.
      deleteScansOlderThanMinutes: 5
      
      # Ignore any scans on FoD if they are older than this number of days.
      # FoD has a retention policy of 2 years, after which scans are no longer
      # downloadable. As such, the default value for this property is 730 days. 
      # Setting this property to more than 730 days may cause errors when 
      # trying to download older scans. Optionally, this property can be set
      # to a smaller number of days, like 365 or 30, if you want to ignore
      # older scans.
      #ignoreScansOlderThanDays: 730
      
    # Configuration for the linkReleases task
    linkReleases:
      # Configure the schedule for running the linkReleases task.
      # This example runs every minute at the 0-second mark; 
      # you probably want to reduce this for production runs.
      cronSchedule: '0 * * * * *'
      
      # FoD-related configuration for the linkReleases task
      fod:
        # Configure which FoD applications and releases should be taken
        # into account for linking FoD releases to SSC application versions.
        # Note that at least one filter property must be specified for both
        # applications and releases, although the actual filter value may be
        # empty.
        filters:
        
          application:  
            # Have FoD filter the list of applications by passing the given value
            # as FoD 'filter' request parameter
            # fodFilterParam: applicationName:test 
            
            # Client-side filter based on SpEL predicate expressions; see 
            # https://docs.spring.io/spring/docs/5.2.1.RELEASE/spring-framework-reference/core.html#expressions-language-ref
            # Expressions can reference FoD application properties and application
            # attributes. This example only takes applications into account for 
            # which the custom 'SyncWithSSC' application attribute has been set 
            # to 'True'.
            filterExpressions:
            - attributesMap['SyncWithSSC'] == 'True'
            #- applicationName == 'test'
          
          release:
            # Have FoD filter the list of releases by passing the given value
            # as FoD 'filter' request parameter
            # fodFilterParam: releaseName:5.0
            
            # Similar to application filter expressions, allows for filtering
            # based on FoD release properties. This example only takes releases
            # into account that have either static or dynamic scan results.
            filterExpressions:
            #- releaseName matches '5.0'
            
            # For each matching application, link only the first release that matches
            # the filters above, based on the configured order by property and direction.
            onlyFirst:
              orderBy: releaseCreatedDate
              direction: DESC
      
      # SSC-related configuration for the linkReleases task
      ssc:
        autoCreateVersions: 
          # If enabled, automatically create new SSC application versions for FoD releases 
          # that match the FoD filter criteria defined above. If disabled, FoD releases will
          # only be linked if the corresponding SSC application version already exists.
          enabled: true
          
          # For newly created SSC application versions, configure which FoD scan types should
          # be synchronized by default.
          enabledFoDScanTypes: 
          - Static
          - Dynamic
          #- Mobile # FoD REST API does not yet allow for downloading Mobile scan results
          
          # Ceate an SSC application version only if there are scans to be synchronized. If set
          # to true (default), the utility will only create application versions for FoD releases
          # that have one of the scan types defined in the enabledFoDScanTypes property above,
          # and at least one of those scans is not older than the ignoreScansOlderThanDays 
          # property defined in the syncScans configuration. If set to false, SSC application 
          # versions will be created for any release that matches the FoD filters configured above,
          # independent of whether there are actually any scans to be synchronized.
          #createOnlyIfSyncableScans: true
          
          # Configure the issue template name to be used for newly created SSC application versions.
          issueTemplateName: Prioritized High Risk Issue Template
```

The `cronSchedule` property is in extended cron format, using 6 fields to specify
second, minute, hour, day of month, month, and day of week. To disable either 
the `syncScans` or `linkReleases` you can either remove/comment out the 
corresponding `cronSchedule` property, or specify '-' as the property value. See
https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/scheduling/support/CronSequenceGenerator.html for more information about the supported cron format.

### Running the utility

The utility is provided as a single runnable JAR file; you will need to have a Java 
Runtime Environment (JRE) version 1.8 or later installed in order to run the utility. 

Once all prerequisites have been met, the utility can be started using the following 
command:

```
java [-Dfortify.home=/path/to/fortify/home] -jar [jar-file]
```

The utility will output some log messages to the console; there is no further interaction
with the utility at the moment. The utility will run the various synchronization tasks 
in the background, according to the configured schedules.  

## Managing FoD application release - SSC application version mappings

### Setting up a manual mapping

In some cases, for example if the linkReleases task is disabled or if the task fails to automatically
link a FoD release to an SSC application version (for example because they are named differently), the
following steps will allow for setting up a manual mapping:

* Create a new SSC application version, or edit an existing application version that is not being synchronized yet
* On the 'Organization Attributes' page:
    * Set the FoD release id that this application version should be synchronized with
    * Select one or more scan result types that should be synchronized 

### Disable a mapping

To temporarily or permanently disable synchronization for a specific mapping, simply deselect all
scan types from the `FoD Sync - Include Scan Types` application version attribute in the
SSC application version configuration.

The same approach can be used to temporarily or permanently disable synchronization of a specific
scan type, for example to only synchronize static scans but not dynamic scans.


## Build from source

The utility can be built by running one of the following commands from the source tree 
root directory:
* Linux/bash: `./gradlew build -x test`
* Linux/bash: `gradlew build -x test`

You will need to have Java JDK 1.8 or later installed. Once built, the binary
jar file can be found in the build/libs folder, which you can optionally copy to 
any location on your system.
