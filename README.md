# Fortify FoD-SSC Sync

This stand-alone utility allows for automated, scheduled synchronization of Fortify on Demand (FoD) application releases
and scans with Fortify Software Security Center (SSC). This functionality is based on two tasks that run automatically 
on a configurable schedule: 

* `Link Releases` task:
  * Based on configurable filtering criteria, look for FoD releases that have not yet been linked to an SSC application version
  * If a similarly named SSC application version already exists, link the FoD release to that application version
  * Optionally create a new SSC application version with the same name as the FoD application release 
* `Sync Scans` task:
  * Iterate over all SSC application versions that have been previously linked (either automatically or manually) to an FoD release
  * Check whether any new scans exist on the FoD release
  * If so, download the scan from FoD and upload to SSC
  
 ### Related Links

* **Branches**: https://github.com/fortify-ps/FortifySyncFoDToSSC/branches  
  Current development is usually done on latest snapshot branch, which may not be the default branch
* **Automated builds**: https://travis-ci.com/fortify-ps/FortifySyncFoDToSSC
* **Binaries**: https://bintray.com/beta/#/fortify-ps/binaries/FortifySyncFoDToSSC?tab=files  
  Sort by `Updated` column to find latest
* **Sample configuration files**: [config](config)
* **OWASP Dependency Check resources**:
  * https://owasp.org/www-project-dependency-check/ 


## Usage

### FoD Configuration - Authentication

The utility supports accessing FoD using either user credentials or client credentials. Depending on the 
authentication method you want to use, you will either need to define an API key or a user account that
will be used by the utility to connect to FoD. Following is a list of requirements and recommendations,
depending on the chosen authentication method:

* User authentication:
  * Required: Permissions to view all applications and releases
  * Recommended: Use a Personal Access Token with `view-apps` and `view-issues` scopes
  * Recommended: Do not use the same user account for any other integrations
* Client credentials:
  * Required: Read Only role

### FoD Configuration - Application Attributes

The utility allows for configuring FoD application and release filters to control which releases get 
synchronized with SSC automatically. These filters may be based on FoD application attributes. For
example, the utility could be configured to only synchronize releases from FoD applications that have
the custom `SyncWithSSC` application attribute set to `true`. Obviously, if you wish to use such filters,
you will need to define the corresponding application attributes in FoD.

### SSC Configuration - Authentication

TODO: Update this section, include info about SSC tokens versus user credentials. 

Define the user account to be used by the utility to connect with SSC. This user must
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


### SSC Configuration - Application Version Attributes

All state related to the synchronization process is stored in SSC application version
attributes. If these attributes do not yet exist when running the utility for the first 
time, the utility will attempt to create these attributes as listed below. You may want
to have an SSC administrator create these attributes beforehand, for example if you do not 
want to provide the `Edit Attribute Definition` permission to the utility. 

* `FoD Sync - Release Id`
  * `Category`: `Technical`
  * `Type`: `Integer`
  * `Hidden`: no
* `FoD Sync - Include Scan Types`
  * `Category`: `Technical`
  * `Type`: `List of Values - Multiple Selection`
  * `Hidden`: no
  * `Values`: 
    * `Static`
    * `Dynamic`
* `FoD Sync - Status`
  * `Category`: `Technical`
  * `Type`: `Text - Multiple Lines`
  * `Hidden`: yes

The values for the `Category` and `Hidden` properties listed above are just default values 
and may be changed. Although it is recommended to use the same category for all attributes, 
you may move them to either the `Organization` or `Business` category.

The `FoD Sync - Status` attribute should only be modified by the utility, hence the recommendation 
to hide this field. However, you may want to (temporarily) make this field visible for debugging or 
testing purposes.

Similarly, the `FoD Sync - Release Id` and `FoD Sync - Include Scan Types` attributes are 
visible by default, in order to allow SSC users to manually link SSC application versions 
to FoD releases, and modify the scan types to be synchronized. If you would like to have
the utility fully manage the process of linking FoD releases with SSC application versions,
you may opt to hide these attributes.

### Running the utility

The utility is provided as a single runnable JAR file; you will need to have a Java 
Runtime Environment (JRE) version 1.8 or later installed in order to run the utility.
Before running the utility, you will need to download one of the sample configuration
files from the [config](config/) directory and modify it according to your requirements.
See the comments in the [config/FortifySyncFoDToSSC-full.yml](config/FortifySyncFoDToSSC-full.yml)
sample file for more information.

Following are some examples on how to run the utility, where `<jar-file>` denotes the name and location 
of the Jar file downloaded from Bintray (see [Related Links](#related-links)) or manually built 
(see [Information for developers](#information-for-developers)):

* `java -jar <jar-file>`
  * Will look for configuration file located at `<user home>/.fortify/FortifySyncFoDToSSC/config.yml`, or `FortifySyncFoDToSSC.yml` in current directory
  * Will by default store temporary downloaded scans in `<user home>/.fortify/FortifySyncFoDToSSC/scans`
* `java -Dfortify.home=/some/dir -jar <jar-file>`
  * Will look for configuration file located at `/some/dir/FortifySyncFoDToSSC/config.yml`, or `FortifySyncFoDToSSC.yml` in current directory
  * Will by default store temporary downloaded scans in `/some/dir/FortifySyncFoDToSSC/scans`
* `java -Dsync.home=/some/dir/for/sync -jar <jar-file>`
  * Will look for configuration file located at `/some/dir/for/sync/config.yml`, or `FortifySyncFoDToSSC.yml` in current directory
  * Will by default store temporary downloaded scans in `/some/dir/for/sync/scans`
* `java -Dsync.config=/some/path/to/sync.config.yml -jar <jar-file>`
  * Will look for configuration file located at `/some/path/to/sync.config.yml`
  * Will by default store temporary downloaded scans in `<user home>/.fortify/FortifySyncFoDToSSC/scans`
* `java -Dfortify.home=/some/dir -Dsync.config=/some/path/to/sync.config.yml -jar <jar-file>`
  * Will look for configuration file located at `/some/path/to/sync.config.yml`
  * Will by default store temporary downloaded scans in `/some/dir`
  
Instead of setting the `fortify.home` system property as illustrated in some of the examples above, the
utility also allows the Fortify home directory to be specified using the `FORTIFY_HOME` environment variable.

The utility will output some log messages to the console; there is no further interaction
with the utility at the moment. The utility will run the various synchronization tasks 
in the background, according to the configured schedules.  

### Managing FoD application release - SSC application version mappings

### Setting up a manual mapping

In some cases, for example if the linkReleases task is disabled or if the task fails to automatically
link a specific FoD release to an SSC application version (for example because they are named differently), 
the following steps will allow for setting up a manual mapping:

* Create a new SSC application version, or edit an existing application version that is not being synchronized yet
* Set the `FoD Sync - Release Id` attribute to the FoD release id that this SSC application version should be synchronized with
* Set the `FoD Sync - Include Scan Types` attribute to one or more scan types that should be synchronized

### Disable a mapping

To temporarily or permanently disable synchronization for a specific application version, 
simply deselect all scan types from the `FoD Sync - Include Scan Types` attribute. Likewise,
you can temporarily or permanently disable synchronization of a specific scan type by deselecting
that scna type from the `FoD Sync - Include Scan Types` attribute.


## Information for developers

The following sections provide information that may be useful for developers of this utility.

### IDE's

This project uses Lombok. In order to have your IDE compile this project without errors, 
you may need to add Lombok support to your IDE. Please see https://projectlombok.org/setup/overview 
for more information.

### Gradle

It is strongly recommended to build this project using the included Gradle Wrapper
scripts; using other Gradle versions may result in build errors and other issues.

The Gradle build uses various helper scripts from https://github.com/fortify-ps/gradle-helpers;
please refer to the documentation and comments in included scripts for more information. 

### Commonly used commands

All commands listed below use Linux/bash notation; adjust accordingly if you
are running on a different platform. All commands are to be executed from
the main project directory.

* `./gradlew tasks --all`: List all available tasks
* Build: (plugin binary will be stored in `build/libs`)
  * `./gradlew clean build`: Clean and build the project
  * `./gradlew build`: Build the project without cleaning
* Version management:
  * `./gradlew printProjectVersion`: Print the current version
  * `./gradlew startSnapshotBranch -PnextVersion=2.0`: Start a new snapshot branch for an upcoming `2.0` version
  * `./gradlew releaseSnapshot`: Merge the changes from the current branch to the master branch, and create release tag
* `./fortify-scan.sh`: Run a Fortify scan; requires Fortify SCA to be installed

Note that the version management tasks operate only on the local repository; you will need to manually
push any changes (including tags and branches) to the remote repository.

### Versioning

The various version-related Gradle tasks assume the following versioning methodology:

* The `master` branch is only used for creating tagged release versions
* A branch named `<version>-SNAPSHOT` contains the current snapshot state for the upcoming release
* Optionally, other branches can be used to develop individual features, perform bug fixes, ...
  * However, note that the Gradle build may be unable to identify a correct version number for the project
  * As such, only builds from tagged versions or from a `<version>-SNAPSHOT` branch should be published to a Maven repository

### Automated Builds & publishing

Travis-CI builds are automatically triggered when there is any change in the project repository,
for example due to pushing changes, or creating tags or branches. If applicable, binaries and related 
artifacts are automatically published to Bintray using the `bintrayUpload` task:

* Building a tagged version will result in corresponding release version artifacts to be published
* Building a branch named `<version>-SNAPSHOT` will result in corresponding beta version artifacts to be published
* No artifacts will be deployed for any other build, for example when Travis-CI builds the `master` branch

See the [Related Links](#related-links) section for the relevant Travis-CI and Bintray links.


# Licensing
See [LICENSE.TXT](LICENSE.TXT)


