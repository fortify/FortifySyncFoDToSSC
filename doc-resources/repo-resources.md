* **Usage**: [USAGE.md](USAGE.md)
* **Releases**: {{var:repo-url}}/releases
    * _Development releases may be unstable or non-functional. The `*-thirdparty.zip` file is for informational purposes only and does not need to be downloaded._
* **Docker images**: https://hub.docker.com/repository/docker/fortify/sync-fod-to-ssc
    * `latest` and `stable` tags point to the latest production release
    * `vX.Y.Z` and `X.Y.Z` tags point to the given patch release
    * `vX.Y` and `X.Y` tags point to the latest patch release of the given minor release
    * `vX` and `X` tags point to the latest minor and patch release of the given major release
    * `latest_<branch>` tags point to the latest development release for a given branch
    * `latest_rc` tag points to the latest development release on the main branch
* **Sample configuration files**: [config](config)
* **Source code**: {{var:repo-url}}
* **Automated builds**: {{var:repo-url}}/actions
{{include:resources/nocomments.li.contrib-conduct-licence.md}}