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

Sample configuration files can be found in the GitHub repository at
https://github.com/fortify-ps/FortifySyncFoDToSSC/samples. 

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
