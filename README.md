# Fortify FoD-SSC Sync

### WORK IN PROGRESS, NOT YET FUNCTIONAL

This is still work in progress, but basic functionality should be working. 

## To-do's

* Major cleanup of code, in particular for link releases task
* Much more testing
* Performance improvements (avoid duplicate requests, use bulk requests, ...)
* More configuration options, more functionality

## SSC Preparations

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

## FoD Preparations

The following SSC preparations are required to use this utility:

* Define the user account to be used by the utility to connect with FoD. This user have permissions to view all applications and releases.

## Installation and usage

The utility can be built by running one of the following commands from the source tree 
root directory:
* Linux/bash: `./gradlew build -x test`
* Linux/bash: `gradlew build -x test`

You will need to have Java JDK 1.8 (or later?) installed. Once built, the binary
jar file can be found in the build/libs folder, which you can optionally copy to 
any location on your system.

Next step is to configure the utility; following is an example configuration file:

```yaml
sync:
  connections:
    fod:
      baseUrl: https://api.{region}.fortify.com
      tenant: {FoD tenant}
      userName: {FoD user}
      password: {FoD password or PAT}
      # Configure the number of retries if FoD rate limit is exceeded
      # If user configured above is only used for this integration,
      # this can be left at the default (1). If user is also used
      # by other integrations, you may want to increase this to retry
      # multiple times if the other integration is assigned a free 
      # rate limit slot
      #rateLimitMaxRetries: 1
      #proxy:
      #  url: {proxy URL}
      #  userName: {optional proxy user}
      #  password: {password for proxy user}
    ssc:
      baseUrl: {SSC URL}
      userName: {SSC user}
      password: {SSC password}
  jobs:
    syncScans:
      schedule: '*/5 * * * * *'
    linkReleases:
      schedule: '0 0 */2 * * *'
      fod:
        filters:
          application:
            fodFilterParam:
            filterExpressions:
            # Filter based on custom FoD boolean attribute
            - attributesMap['SyncWithSSC'] == 'True'
            #  - applicationName == 'wg'
          release:
            fodFilterParam:
            #filterExpressions:
            #  - releaseName matches '5.0'
            onlyFirst:
              orderBy: releaseCreatedDate
              direction: DESC
      ssc:
        enabled: true
        enabledScanTypes: Static
        issueTemplateName: Prioritized High Risk Issue Template
```

The `schedule` property is in extended cron format, using 6 fields to specify
second, minute, hour, day of month, month, and day of week. To disable either 
the `syncScans` or `linkReleases` you can either remove/comment out the 
corresponding `schedule` property, or specify '-' as the property value.

The configuration file will need to be available in the following location:
`$fortify.home/FortifySyncFoDSSC/config.yml`

`$fortify.home` can be an existing Fortify home directory, or a new directory. The utility
will use the following methods to locate the fortify.home directory, in this order:
1. Specified on the Java command line with -Dfortify.home=[directory]
2. Specified as an environment variable FORTIFY_HOME=[directory]
3. ~/.fortify

Once the configuration file has been installed in the correct location, you can
start the utility using the following command:

```
java [-Dfortify.home=/path/to/fortify/home] -jar [jar-file]
```

The utility will output some log messages to the console; there is no further interaction
with the utility at the moment. The utility will run the various synchronization tasks 
in the background.  


## Setting up a manual mapping

Perform the following steps in order to set up a manual mapping for synchronizing FoD scan results to SSC:

* Create a new SSC application version, or edit an existing application version that is not being synchronized yet
* On the 'Organization Attributes' page:
    * Set the FoD release id that this application version should be synchronized with
    * Select one or more scan result types that should be synchronized 

## Disable a mapping

To temporarily or permanently disable synchronization for a specific mapping, one of the following two approaches can be used:

* Remove access to the SSC application version for the 'FoD Sync - Scans' user
* Deselect all scan types from the `FoD Sync - Include Scan Types` application version attribute 

## Implementation Plan

Considerations:

* Due to FoD rate limiting (like download at most 1 FPR per 30 seconds), it doesn't make sense to run a lot of tasks in parallel; processing will be mostly sequential.
* By default, the utility should run in a fully automated fashion, without requiring users to manually configure any mappings between FoD releases and SSC application versions.
* If needed, user should have manual control over mappings and the synchronization process.
* Preferably, synchronization state should be externalized to either SSC or FoD as much as possible, to avoid having to store and maintain a potentially large status database. 
  

Implementation plan:

* Utility runs two background tasks:
    * Task `Link Releases` to automatically link FoD releases to SSC application versions
    * Task `Sync Scans` for syncing the latest scans for linked releases

* Both tasks use separate configuration values for the following:
    * Run interval (each task can have a different interval)
    * FoD and SSC credentials (each task can use different credentials)
    * Any other task-specific configuration    
    
* Task `Link releases` performs the following activities (using caching where applicable):
    1. Query `/api/v1/localUsers?q=userName:"FoD Sync - Scans"&fields=id`, user name taken from the `Sync Scans` task configuration
    2. Query `/authEntities/{id returned by step 1}/projectVersions?fields=id` to get all application versions already being synchronized  
    3. Query `/api/v1/projectVersions/{id}/attributes?fields=guid,value` for every application version, add the value for the guid matching the `FoD Sync - Release Id` to the 'already mapped release id's' cache
    4. Query all FoD releases that match configurable filter criteria
        * As much as possible, filtering will be done by FoD, but we also may need to do client-side filtering (for example if FoD doesn’t support filtering by application attribute)
        * Querying this list of releases may use the `/api/v3/applications` and `/api/v3/applications/{id}/releases` endpoints, or the `/api/v3/releases` endpoint (whichever approach allows us to do as much filtering as possible on the FoD side)
    5. For every matching release, if the release id is not found in the 'already mapped release id's' cache:
        * Query SSC to see whether a similarly named application version already exists
           * If a matching application version exists, update the application version attributes as described in the 'Setting up a manual mapping' section
           * If no matching application version exists, and if utility has been configured to automatically create new application versions, create new application version, using similar settings as described in the 'Setting up a manual mapping' section

* Task `Sync Scans` performs the following activities (using caching where applicable):
    1. Get all SSC application versions to which our current user is assigned
    2. For each application version
        1. Get the values for `FoD Sync - Release Id`, `FoD Sync - Include Scan Types`, and `FoD Sync - Status` attributes
        2. If release id or scan types empty, continue with next application version
        3. Query `/api/v3/releases?filters=releaseId%3A{releaseId}&fields=staticScanDate%2CdynamicScanDate%2CmobileScanDate`
        4. Compare static, dynamic and mobile scan dates against the values stored in the `FoD Sync - Status` attribute (note that attribute may be empty on first sync)
        5. If scan date returned by FoD is later than date stored in the status attribute, and the scan type is enabled in the `FoD Sync - Include Scan Types` attribute, download the FPR from FoD, upload FPR to SSC, and set the new scan date in the status field
            * Important: Don't update date scan date in status field if scan type is disabled, as we may want to enable a scan type later and sync the latest FPR file
        


Phase 1:

* Basic implementation of the two tasks
* Configuration is probably done through a property/yaml file containing the following settings:
    * FoD base URL and tentant
    * SSC base URL
    * Boolean whether to auto-create SSC version attributes? Or should we do so anyway as the utility won't run without it?
    * `Sync Scans` task:
        * FoD username and password/PAT
        * SSC username and password, or token
        * Task run interval (default once per hour or so?)
    * `Link Releases` task:
        * FoD username and password/PAT
        * SSC username and password, or token
        * Task run interval (default once per day or so?)
        * Boolean auto-create SSC application versions
        * FoD application/release search criteria (TBD: what search criteria do we want to support in first phase?)
* No user interface
* SSC application versions are created using SSC default issue template, and random values for any required attributes (of course setting appropriate values for the FoD Sync attributes)

Future phases:

* Provide web-based user interface for configuring the utility
    * Initially listening on localhost only, later add support for remote connections and authentication by checking credentials against FoD or SSC
* Add support for additional FoD application/release search criteria
* For new SSC application versions, set version attributes based on FoD application attributes
* For new SSC application versions, allow the user to configure an SSC application version that is used as a template for configuring issue template, version attributes, access on the new version
* Add support for viewing existing FoD release/SSC version links in the utility web UI?
* Add support for manually mapping FoD releases to SSC versions in the utility web UI?
    * The only advantage over configuring a mapping in SSC directly would be that the utility can display the list of available FoD release names, provide auto-complete release/version names, instead of having to configure a release id in SSC.


 
