#!/bin/bash

projectName=FortifySyncFoDToSSC

# Modular scan doesn't work properly yet, so for now we just add the fortify-client-api build model
# Note that either approach requires fortify-client-api to be translated/scanned on the same machine
# before running this script.
#scanOpts="-scan -include-modules fortify-client-api -Dcom.fortify.sca.UseSynchronousSerialization=true -Dcom.fortify.sca.ThreadCount=1 -Dcom.fortify.sca.DefaultAnalyzers=dataflow"
scanOpts="-b fortify-client-api -scan" 

set -x

# Clean our build model
sourceanalyzer -b ${projectName} -clean

# Translate using SCA Gradle integration
# The -Pfortify option informs our build script that 
# we're running a Fortify translation, allowing the
# build script to take appropriate measures if necessary
sourceanalyzer -b ${projectName} gradle clean build -Pfortify

# Scan the project as a module (SCA 19.2+ feature)
sourceanalyzer -b ${projectName} -f ${projectName}.fpr ${scanOpts}
