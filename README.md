# Fortify FoD to SSC Synchronization Utility 


<!-- START-INCLUDE:p.marketing-intro.md -->

[Fortify Application Security](https://www.microfocus.com/en-us/solutions/application-security) provides your team with solutions to empower [DevSecOps](https://www.microfocus.com/en-us/cyberres/use-cases/devsecops) practices, enable [cloud transformation](https://www.microfocus.com/en-us/cyberres/use-cases/cloud-transformation), and secure your [software supply chain](https://www.microfocus.com/en-us/cyberres/use-cases/securing-the-software-supply-chain). As the sole Code Security solution with over two decades of expertise and acknowledged as a market leader by all major analysts, Fortify delivers the most adaptable, precise, and scalable AppSec platform available, supporting the breadth of tech you use and integrated into your preferred toolchain. We firmly believe that your great code [demands great security](https://www.microfocus.com/cyberres/application-security/developer-security), and with Fortify, go beyond 'check the box' security to achieve that.

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

The only warranties for products and services of Open Text and its affiliates and licensors (“Open Text”) are as may be set forth in the express warranty statements accompanying such products and services. Nothing herein should be construed as constituting an additional warranty. Open Text shall not be liable for technical or editorial errors or omissions contained herein. The information contained herein is subject to change without notice.

The software is provided "as is" and is not supported through the regular OpenText Support channels. Support requests may be submitted through the [GitHub Issues](https://github.com/fortify/FortifySyncFoDToSSC/issues) page for this repository. A (free) GitHub account is required to submit new issues or to comment on existing issues. 

Support requests created through the GitHub Issues page may include bug reports, enhancement requests and general usage questions. Please avoid creating duplicate issues by checking whether there is any existing issue, either open or closed, that already addresses your question, bug or enhancement request. If an issue already exists, please add a comment to provide additional details if applicable.

Support requests on the GitHub Issues page are handled on a best-effort basis; there is no guaranteed response time, no guarantee that reported bugs will be fixed, and no guarantee that enhancement requests will be implemented. If you require dedicated support for this and other Fortify software, please consider purchasing OpenText Fortify Professional Services. OpenText Fortify Professional Services can assist with general usage questions, integration of the software into your processes, and implementing customizations, bug fixes, and feature requests (subject to feasibility analysis). Please contact your OpenText Sales representative or fill in the [Professional Services Contact Form](https://www.microfocus.com/en-us/cyberres/contact/professional-services) to obtain more information on pricing and the services that OpenText Fortify Professional Services can provide.

---

*[This document was auto-generated from README.template.md; do not edit by hand](https://github.com/fortify/shared-doc-resources/blob/main/USAGE.md)*
