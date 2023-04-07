
<!-- START-INCLUDE:repo-usage.md -->

<x-tag-head>
<x-tag-meta http-equiv="X-UA-Compatible" content="IE=edge"/>

<x-tag-script language="JavaScript"><!--
<X-INCLUDE url="https://cdn.jsdelivr.net/gh/highlightjs/cdn-release@10.0.0/build/highlight.min.js"/>
--></x-tag-script>

<x-tag-script language="JavaScript"><!--
<X-INCLUDE url="https://ajax.googleapis.com/ajax/libs/jquery/3.4.1/jquery.min.js" />
--></x-tag-script>

<x-tag-script language="JavaScript"><!--
<X-INCLUDE url="${gradleHelpersLocation}/spa_readme.js" />
--></x-tag-script>

<x-tag-style><!--
<X-INCLUDE url="https://cdn.jsdelivr.net/gh/highlightjs/cdn-release@10.0.0/build/styles/github.min.css" />
--></x-tag-style>

<x-tag-style><!--
<X-INCLUDE url="${gradleHelpersLocation}/spa_readme.css" />
--></x-tag-style>
</x-tag-head>

# Fortify FoD to SSC Synchronization Utility - Usage Instructions

## Introduction


<!-- START-INCLUDE:repo-intro.md -->

This stand-alone utility allows for automated, scheduled synchronization of Fortify on Demand (FoD) application releases and scans with Fortify Software Security Center (SSC). This functionality is based on two tasks that run automatically on a configurable schedule: 

* Link Releases task:
	* Based on configurable filtering criteria, look for FoD releases that have not yet been linked to an SSC application version
	* If a similarly named SSC application version already exists, link the FoD release to that application version
	* Optionally create a new SSC application version with the same name as the FoD application release 
* Sync Scans task:
	* Iterate over all SSC application versions that have been previously linked (either automatically or manually) to an FoD release
	* Check whether any new scans exist on the FoD release
	* If so, download the scan from FoD and upload to SSC

<!-- END-INCLUDE:repo-intro.md -->


## FoD Configuration

### Permissions

The utility supports accessing FoD using either user credentials or client credentials. Depending on the 
authentication method you want to use, you will either need to define an API key or a user account that
will be used by the utility to connect to FoD. Following is a list of requirements and recommendations,
depending on the chosen authentication method:

* User authentication (with password or Personal Access Token):
	* Required: Permissions to view all applications and releases
	* Recommended: Use a Personal Access Token with `view-apps` and `view-issues` scopes
	* Recommended: Do not use the same user account for any other integrations
* Client credentials:
	* Required: Read Only role

### Attributes

The utility allows for configuring FoD application and release filters to control which releases get 
synchronized with SSC automatically. These filters may be based on FoD application attributes. For
example, the utility could be configured to only synchronize releases from FoD applications that have
the custom `SyncWithSSC` application attribute set to `true`. Obviously, if you wish to use such filters,
you will need to define the corresponding application attributes in FoD.


## SSC Configuration

### Permissions

It is recommended to created a dedicated user role for this utility, specifying the exact
permissions required for running the utility. In general, the following permissions are 
required by the utility:

* `Universal access`
* `Add application versions`
* `Edit application versions`
* `Manage attribute definitions`
* `Upload analysis results`
* `View application versions`
* `View attribute definitions`
* `View issue templates`

Obviously, if for example automatic creation of SSC application versions is disabled
in the utility configuration, the `Add application versions` permission will not be 
required. As such, depending on the utility configuration, you may be able to remove 
some of these permissions.

### Authentication

The utility supports accessing SSC using either user credentials or an authentication token.
If you wish to use an authentication token, you will need to follow the following additional
steps:

* Define a custom token type in SSC's `WEB-INF/internal/serviceContext.xml` file as listed below, adjusting
 the `maxDaysToLive` property value to your needs
* Restart SSC
* Use FortifyClient or the SSC web interface to generate an authentication token of type `FortifySyncFoDToSSC`
	* Make sure to generate the token for the dedicated user that has been assigned the dedicated role
  described above.

**Token definition**
```xml
	<bean id="FortifySyncFoDToSSC" class="com.fortify.manager.security.ws.AuthenticationTokenSpec">
		<property name="key" value="FortifySyncFoDToSSC"/>
		<property name="maxDaysToLive" value="90" />
		<property name="actionPermitted">
			<list value-type="java.lang.String">
				<value>GET=/api/v\d+/attributeDefinitions</value>
				<value>GET=/api/v\d+/issueTemplates</value>
				<value>GET=/api/v\d+/projectVersions</value>
				<value>GET=/api/v\\d+/projectVersions/\\d+/attributes</value>
				<value>POST=/api/v\d+/bulk</value>
				<value>POST=/api/v\d+/attributeDefinitions</value>
				<value>POST=/api/v\d+/fileTokens</value>
				<value>POST=/api/v\d+/projectVersions</value>
				<value>POST=/upload/resultFileUpload.html</value>
				<value>PUT=/api/v\d+/projectVersions/\d+</value>
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


### Attributes

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
		* Mobile
* `FoD Sync - Status`
	* Category: Technical
	* Type: Text - Multiple Lines
	* Hidden: no

Although it is recommended to use the same category for all attributes, you may move them to either 
the `Organization` or `Business` category instead of the `Technical` category as listed here.

It would make sense to define the `FoD Sync - Status` attribute as a hidden attribute, as the
contents are not supposed to be viewed or edited by users. Similarly, if you would like to have
the utility fully manage the process of linking FoD releases with SSC application versions, it
would make sense to define the `FoD Sync - Release Id` and/or `FoD Sync - Include Scan Types`
as hidden attributes. 

Unfortunately, current SSC versions do not properly support setting or retrieving values for 
hidden attributes through the SSC REST API. As such, for now all attributes must have the Hidden
flag set to 'no' (i.e. all attributes must be visible).

## Configuration File

### Contents
The utility requires a configuration file to operate. Sample configuration files are provided
in the [config](config/) directory; you will need to download one of these configuration files
and modify it according to your requirements. See the comments in the 
[config/FortifySyncFoDToSSC-full.yml](config/FortifySyncFoDToSSC-full.yml) sample file for more 
information.

<!-- TODO Move Style to class -->
<x-tag-div style="width: 95%; height: 25em; margin: auto; overflow:auto;">
<x-tag-pre><x-tag-code style="overflow-x: visible;" class="yaml hljs">
<X-INCLUDE url="file:config/FortifySyncFoDToSSC-full.yml"/>
</x-tag-code></x-tag-pre>
</x-tag-div>

### Location 
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


## Running the utility

### Requirements
The utility is provided as a single runnable JAR file; you will need to have a Java 
Runtime Environment (JRE) version 1.8 or later installed in order to run the utility.
Before running the utility, you will need to have prepared the utility configuration
file; see [Utility Configuration](#utility-configuration)) for details.

### Command Line
The generic command for starting the utility is as follows:  
`java [-Dproperty=value] -jar <jar-file>`

* `<jar-file>` denotes the name and location of the Jar file downloaded from the GitHub Releases page
* `[-Dproperty=value]` optionally defines a system property, as described in the
  [Configuration File](#configuration-file) section.
  
Following are some examples:

* `java -jar <jar-file>`
	* Will look for configuration file located at `<user home>/.fortify/FortifySyncFoDToSSC/config.yml`, or `FortifySyncFoDToSSC.yml` in current directory
	* Will by default store temporary downloaded scans in `<user home>/.fortify/FortifySyncFoDToSSC/scans`
* `java -Dsync.config=/path/to/config.yaml -jar <jar-file>`
	* Will look for configuration file located at `/path/to/config.yaml`
	* Will by default store temporary downloaded scans in `<user home>/.fortify/FortifySyncFoDToSSC/scans`
* `java -Dfortify.home=/some/dir -jar <jar-file>`
	* Will look for configuration file located at `/some/dir/FortifySyncFoDToSSC/config.yml`, or `FortifySyncFoDToSSC.yml` in current directory
	* Will by default store temporary downloaded scans in `/some/dir/FortifySyncFoDToSSC/scans`

Once running, the utility will output some log messages to the console; there is no further interaction with 
the utility. The utility will run the various synchronization tasks in the background, according 
to the configured schedules.  


### Docker

Docker images for running this utility are available at https://hub.docker.com/repository/docker/fortify/sync-fod-to-ssc/.
Please see the instructions in the Docker Hub repository for details on how to configure and run this image.


## Synchronization Settings

Synchronization settings are by default managed automatically by the utility, based on the various
settings in the utility configuration file. In some cases you may however want to manually control 
synchronization settings for individual application versions. The following sections describe how
to manually enable or disable synchronization for an individual application version/release.

### Enable Synchronization

The following steps described how to manually set up synchronization between an SSC application version
and FoD release:

* Create a new SSC application version, or edit an existing application version that is not being synchronized yet
* Set the `FoD Sync - Release Id` attribute to the FoD release id that this SSC application version should be synchronized with
* Set the `FoD Sync - Include Scan Types` attribute to one or more scan types that should be synchronized

### Disable Synchronization

To temporarily or permanently disable synchronization for a specific application version, 
simply deselect all scan types from the `FoD Sync - Include Scan Types` attribute. Likewise,
you can temporarily or permanently disable synchronization of a specific scan type by deselecting
that scan type from the `FoD Sync - Include Scan Types` attribute.

Note that removing the FoD release id from the `FoD Sync - Release Id` attribute will usually not 
be sufficient to disable synchronization, as most likely the value of this attribute will be restored
during the next run of the 'Link Releases' task.

<!-- END-INCLUDE:repo-usage.md -->


---

*[This document was auto-generated from USAGE.template.md; do not edit by hand](https://github.com/fortify/shared-doc-resources/blob/main/USAGE.md)*
