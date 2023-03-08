## Information for Developers

{{include:devinfo/h3.release-please.md}}

{{include:devinfo/h3.lombok.md}}

{{include:devinfo/h3.gradle-wrapper.md}}

{{include:devinfo/p.gradle-helpers.md}}

### Common Commands

All commands listed below use Linux/bash notation; adjust accordingly if you
are running on a different platform. All commands are to be executed from
the main project directory.

* `./gradlew tasks --all`: List all available tasks
* Build: (plugin binary will be stored in `build/libs`)
	* `./gradlew clean build`: Clean and build the project
	* `./gradlew build`: Build the project without cleaning
	* `./gradlew dist distThirdParty`: Build distribution zip and third-party information bundle
* `./fortify-scan.sh`: Run a Fortify scan; requires Fortify SCA to be installed
