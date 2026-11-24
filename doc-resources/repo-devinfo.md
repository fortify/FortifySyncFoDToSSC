## Information for Developers

{{include:devinfo/h3.release-please.md}}

{{include:devinfo/h3.lombok.md}}

{{include:devinfo/h3.gradle-wrapper.md}}

{{include:devinfo/p.gradle-helpers.md}}

### Common Commands

Install JDK 17 and use the included Gradle 8.14.8 wrapper. Gradle 8 is retained
because the Markdown plugin's link renderer is incompatible with Gradle 9.
The build substitutes `com.wavefront:remark` for the unavailable original
`com.overzealous:remark` artifact, as in the Fortify SSC parser projects.
Jersey 3.1 and Jackson 2 are retained for compatibility with the Fortify client's
Apache HTTP 4 connector and JSON API.

All commands listed below use Linux/bash notation; adjust accordingly if you
are running on a different platform. All commands are to be executed from
the main project directory.

* `./gradlew tasks --all`: List all available tasks
* Build: (executable JAR will be stored in `build/libs`)
	* `./gradlew clean build`: Clean and build the project
	* `./gradlew build`: Build the project without cleaning
	* `./gradlew dist distThirdParty`: Build distribution zip and third-party information bundle
	* `./gradlew test`: Run migration tests without FoD/SSC credentials (REST tests use loopback HTTP)
	* `./gradlew jibBuildTar`: Build `build/jib-image.tar` without publishing; includes default configuration and samples
	* `./gradlew jib -PjibImageTag=<tag>`: Publish an image; requires Docker Hub credentials
* `./fortify-scan.sh`: Run a Fortify scan; requires Fortify SCA to be installed

### GitHub Workflows

Workflows reference commit-pinned actions and reusable workflows from
`fortify/shared-github`. Pull requests and manual runs build and test without
publishing. Pushes publish production releases created by release-please, or
development releases when the corresponding `dev_<branch>` Git tag already
exists. Special characters in branch names are replaced with `.` in tags.
Docker publishing uses `DOCKER_HUB_USER` and `DOCKER_HUB_TOKEN`.

The shared FoD workflow runs on main-branch pushes and manual requests, using
`OSS_FOD_TENANT`, `OSS_FOD_USER`, and `OSS_FOD_PAT`. It uses FoD US and
repository-based application/release setup, with SAST, SCA, and Aviator enabled;
the old `OSS_FOD_RELEASE_ID` setting is no longer used.

The bump workflow checks shared-github pins on pushes, daily, manually, and on
`shared-github-updated` dispatch events. Stale pins fail visibly and produce a
patch for a maintainer to apply; the workflow does not write workflow files.
