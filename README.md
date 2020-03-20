# Fortify FoD-SSC Sync

This stand-alone utility allows for automated, scheduled synchronization of Fortify on Demand (FoD) application releases
and scans with Fortify Software Security Center (SSC). This functionality is based on two tasks that run automatically 
on a configurable schedule: 

* Link Releases task:
  * Based on configurable filtering criteria, look for FoD releases that have not yet been linked to an SSC application version
  * If a similarly named SSC application version already exists, link the FoD release to that application version
  * Optionally create a new SSC application version with the same name as the FoD application release 
* Sync Scans task:
  * Iterate over all SSC application versions that have been previously linked (either automatically or manually) to an FoD release
  * Check whether any new scans exist on the FoD release
  * If so, download the scan from FoD and upload to SSC
  
### Related Links

* **Downloads**:  
  _Beta versions may be unstable or non-functional. The `*-licenseReport.zip` and `*-dependencySources.zip` files are for informational purposes only and do not need to be downloaded._
  * **Release versions**: https://bintray.com/package/files/fortify-ps/binaries/FortifySyncFoDToSSC-release?order=desc&sort=fileLastModified&basePath=&tab=files  
  * **Beta versions**: https://bintray.com/package/files/fortify-ps/binaries/FortifySyncFoDToSSC-beta?order=desc&sort=fileLastModified&basePath=&tab=files
  * **Sample configuration files**: [config](config)
* **Automated builds**: https://travis-ci.com/fortify-ps/FortifySyncFoDToSSC


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

It is recommended to created a dedicated user role for this utility, specifying the exact
permissions required for running the utility. In general, the following permissions are 
required by the utility:

* Universal access
* Add application versions
* Edit application versions
* Manage attribute definitions
* Upload analysis results
* View application versions
* View attribute definitions
* View issue templates

Obviously, if for example automatic creation of SSC application versions is disabled
in the utility configuration, the `Add application versions` permission will not be 
required. As such, depending on the utility configuration, you may be able to remove 
some of these permissions.

The utility supports accessing SSC using either user credentials or an authentication token.
If you wish to use an authentication token, you will need to follow the following additional
steps:

* Define a custom token type in SSC's `WEB-INF/internal/serviceContext.xml` file as listed below
  * Adjust the `maxDaysToLive` property value to your needs
  * Note that this token definition has not yet been tested; based on rejected access errors you
  may need to add additional permitted actions
* Restart SSC
* Use FortifyClient or the SSC web interface to generate an authentication token of type `FortifySyncFoDToSSC`
  * Make sure to generate the token for the dedicated user that has been assigned the dedicated role
  described above.

```xml
	<bean id="FortifySyncFoDToSSC" class="com.fortify.manager.security.ws.AuthenticationTokenSpec">
		<property name="key" value="FortifySyncFoDToSSC"/>
		<property name="maxDaysToLive" value="90" />
		<property name="actionPermitted">
			<list value-type="java.lang.String">
				<value>GET=/api/v\d+/attributeDefinitions</value>
				<value>GET=/api/v\d+/issueTemplates</value>
				<value>GET=/api/v\d+/projectVersions</value>
				<value>POST=/api/v\d+/attributeDefinitions</value>
				<value>POST=/api/v\d+/fileTokens</value>
				<value>POST=/api/v\d+/projectVersions</value>
				<value>POST=/upload/resultFileUpload.html</value>
				<value>PUT=/api/v\d+/projectVersions/\d+/action</value>
				<value>PUT=/api/v\d+/projectVersions/\d+/attributes</value>
			</list>
		</property>
		<property name="terminalActions">
			<list value-type="java.lang.String">
				<value>InvalidateTokenRequest</value>
				<value>DELETE=/api/v\d+/auth/token</value>
			</list>
		</property>
	</bean>

```



### SSC Configuration - Application Version Attributes

All state related to the synchronization process is stored in SSC application version
attributes. If these attributes do not yet exist when running the utility for the first 
time, the utility will attempt to create these attributes as listed below. You may want
to have an SSC administrator create these attributes beforehand, for example if you do not 
want to provide the `Manage attribute definitions` permission to the utility. 

* `FoD Sync - Release Id`
  * Category: Technical
  * Type: Integer
  * Hidden: no
* `FoD Sync - Include Scan Types`
  * Category: Technical
  * Type: List of Values - Multiple Selection
  * Hidden: no
  * Values: 
    * Static
    * Dynamic
* `FoD Sync - Status`
  * Category: Technical
  * Type: Text - Multiple Lines
  * Hidden: yes

The values for the `Category` and `Hidden` properties listed above are just default values 
and may be changed. Although it is recommended to use the same category for all attributes, 
you may move them to either the `Organization` or `Business` category instead of the default
`Technical` category.

The `FoD Sync - Status` attribute should only be modified by the utility, hence the recommendation 
to define this as a hidden attribute. However, you may want to (temporarily) make this attribute 
visible for debugging or testing purposes.

Similarly, the `FoD Sync - Release Id` and `FoD Sync - Include Scan Types` attributes are 
visible by default, in order to allow SSC users to manually link SSC application versions 
to FoD releases, and modify the scan types to be synchronized. If you would like to have
the utility fully manage the process of linking FoD releases with SSC application versions,
you may opt to hide these attributes.

### Utility Configuration

The utility requires a configuration file to operate. Sample configuration files are provided
in the [config](config/) directory; you will need to download one of these configuration files
and modify it according to your requirements. See the comments in the 
[config/FortifySyncFoDToSSC-full.yml](config/FortifySyncFoDToSSC-full.yml) sample file for more 
information.

The utility will look for the configuration file in these locations, in this order, where 
`${propertyName}` denotes a system property:

* `${sync.config}`
* `FortifySyncFoDToSSC.yml` in the current working directory
* `${sync.home}/config.yml`
  * Where `${sync.home}` defaults to `${fortify.home}/FortifySyncFoDToSSC`
  * Where `${fortify.home}` defaults to the value of the FORTIFY_HOME environment variable, or `<user home>/.fortify` if not defined

Note that by default, the utility will also create a `${sync.home}/scans` directory to temporarily store downloaded scans, 
so modifying the `${sync.home}` property will affect both the default configuration file location and
default scan download location. In addition, the [config/FortifySyncFoDToSSC-full.yml](config/FortifySyncFoDToSSC-full.yml)
sample configuration file also configures `${sync.home}/logs` as the log files location.

Some examples:

* When not overriding any of the system properties, the utility will look for the configuration file in these two locations:
  * `FortifySyncFoDToSSC.yml` in the current working directory
  * `<user home>/.fortify/FortifySyncFoDToSSC/config.yml`
* `-Dsync.config=/my/custom/config.file` will load the configuration from `/my/custom/config.file`
* `-Dsync.home=/my/sync/home` will:
  * Load the configuration file from `/my/sync/home/config.yml` (assuming no `FortifySyncFoDToSSC.yml` exists in the current working directory)
  * Temporarily store downloaded scans in the `/my/sync/home/scans` directory (if not overridden in configuration file)
  * May store log files in the `/my/sync/home/logs` directory when using the [config/FortifySyncFoDToSSC-full.yml](config/FortifySyncFoDToSSC-full.yml) sample configuration.
* `-Dfortify.home=/my/fortify/home`, or setting the `FORTIFY_HOME` environment variable, will change the default
  value for the `sync.home` property to `/my/fortify/home/FortifySyncFoDToSSC`, with similar effects as changing that 
  property directly. 


### Running the utility

The utility is provided as a single runnable JAR file; you will need to have a Java 
Runtime Environment (JRE) version 1.8 or later installed in order to run the utility.
Before running the utility, you will need to have prepared the utility configuration
file; see the previous section for details.

The generic command for starting the utility is as follows:  
`java [-Dproperty=value] -jar <jar-file>`

* `<jar-file>` denotes the name and location of the Jar file downloaded from Bintray 
(see [Related Links](#related-links)) or manually built (see [Information for developers](#information-for-developers)).
* `[-Dproperty=value]` optionally defines a system property, as described in the
  [Utility Configuration](#utility-configuration) section.
  
Following are some examples:

* `java -jar <jar-file>`
  * Will look for configuration file located at `<user home>/.fortify/FortifySyncFoDToSSC/config.yml`, or `FortifySyncFoDToSSC.yml` in current directory
  * Will by default store temporary downloaded scans in `<user home>/.fortify/FortifySyncFoDToSSC/scans`
* `java -Dfortify.home=/some/dir -jar <jar-file>`
  * Will look for configuration file located at `/some/dir/FortifySyncFoDToSSC/config.yml`, or `FortifySyncFoDToSSC.yml` in current directory
  * Will by default store temporary downloaded scans in `/some/dir/FortifySyncFoDToSSC/scans`

Once runinng, the utility will output some log messages to the console; there is no further interaction with 
the utility at the moment. The utility will run the various synchronization tasks in the background, according 
to the configured schedules.  

### Managing Mappings

### Setting up a manual mapping

In some cases you may want to manually configure a mapping between an FoD release and SSC application version.
The following steps will allow for manually configuring a mapping:

* Create a new SSC application version, or edit an existing application version that is not being synchronized yet
* Set the `FoD Sync - Release Id` attribute to the FoD release id that this SSC application version should be synchronized with
* Set the `FoD Sync - Include Scan Types` attribute to one or more scan types that should be synchronized

### Disable synchronization for specific release

To temporarily or permanently disable synchronization for a specific application version, 
simply deselect all scan types from the `FoD Sync - Include Scan Types` attribute. Likewise,
you can temporarily or permanently disable synchronization of a specific scan type by deselecting
that scan type from the `FoD Sync - Include Scan Types` attribute.


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


