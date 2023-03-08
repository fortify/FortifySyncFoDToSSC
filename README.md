# Fortify FoD to SSC Synchronization Utility 


<!-- START-INCLUDE:p.marketing-intro.md -->

Build secure software fast with [Fortify](https://www.microfocus.com/en-us/solutions/application-security). Fortify offers end-to-end application security solutions with the flexibility of testing on-premises and on-demand to scale and cover the entire software development lifecycle.  With Fortify, find security issues early and fix at the speed of DevOps. 

<!-- END-INCLUDE:p.marketing-intro.md -->



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


## Resources


<!-- START-INCLUDE:repo-resources.md -->

* **Usage**: [USAGE.md](USAGE.md)
* **Releases**: https://github.com/fortify/FortifySyncFoDToSSC/releases
    * _Development releases may be unstable or non-functional. The `*-thirdparty.zip` file is for informational purposes only and does not need to be downloaded._
* **Docker images**: https://hub.docker.com/repository/docker/fortify/sync-fod-to-ssc
    * `latest` and `stable` tags point to the latest production release
    * `vX.Y.Z` and `X.Y.Z` tags point to the given patch release
    * `vX.Y` and `X.Y` tags point to the latest patch release of the given minor release
    * `vX` and `X` tags point to the latest minor and patch release of the given major release
    * `latest_<branch>` tags point to the latest development release for a given branch
    * `latest_rc` tag points to the latest development release on the main branch
* **Sample configuration files**: [config](config)
* **Source code**: https://github.com/fortify/FortifySyncFoDToSSC
* **Automated builds**: https://github.com/fortify/FortifySyncFoDToSSC/actions
* **Contributing Guidelines**: [CONTRIBUTING.md](CONTRIBUTING.md)
* **Code of Conduct**: [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
* **License**: [LICENSE.txt](LICENSE.txt)

<!-- END-INCLUDE:repo-resources.md -->


## Support

The software is provided "as is", without warranty of any kind, and is not supported through the regular Micro Focus Support channels. Support requests may be submitted through the [GitHub Issues](https://github.com/fortify/FortifySyncFoDToSSC/issues) page for this repository. A (free) GitHub account is required to submit new issues or to comment on existing issues. 

Support requests created through the GitHub Issues page may include bug reports, enhancement requests and general usage questions. Please avoid creating duplicate issues by checking whether there is any existing issue, either open or closed, that already addresses your question, bug or enhancement request. If an issue already exists, please add a comment to provide additional details if applicable.

Support requests on the GitHub Issues page are handled on a best-effort basis; there is no guaranteed response time, no guarantee that reported bugs will be fixed, and no guarantee that enhancement requests will be implemented. If you require dedicated support for this and other Fortify software, please consider purchasing Micro Focus Fortify Professional Services. Micro Focus Fortify Professional Services can assist with general usage questions, integration of the software into your processes, and implementing customizations, bug fixes, and feature requests (subject to feasibility analysis). Please contact your Micro Focus Sales representative or fill in the [Professional Services Contact Form](https://www.microfocus.com/en-us/cyberres/contact/professional-services) to obtain more information on pricing and the services that Micro Focus Fortify Professional Services can provide.

---

*This document was auto-generated from README.template.md; do not edit by hand*
