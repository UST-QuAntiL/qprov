#!/usr/bin/env bash

# pipe fail
set -eo pipefail

docker_registry="docker.pkg.github.com"
docker_image_prefix="ust-quantil/qprov"
docker_tag="0.0.1-SNAPSHOT"

MAVEN_ARGS="--update-snapshots --threads 1C"

export JAVA_POST_PROCESS_FILE="/usr/local/bin/clang-format -i"
#export MAVEN_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dgraal.PrintCompilation=true"
#export MAVEN_BUILD_SCANNER=1

mvn="$(command -v mvn)"

# do not delete anything without "--really"
rm="echo rm -rf "

if [ "${1}" == "--really" ]; then
    rm="rm -rf"
fi

# clean local maven repository
$rm "${HOME}/.m2/repository/org/quantil/qprov"
$rm "${PWD}/out"
# remove target folders of every module
$rm "${PWD}/org.quantil.qprov.api/target"
$rm "${PWD}/org.quantil.qprov.core/target"
$rm "${PWD}/org.quantil.qprov.collector/target"

# confirmation
if [ "${1}" != "--really" ]; then
    echo -e "\n  ❌ ❌ ❌ please read a script before executing it! ❌ ❌ ❌\n"
    exit 1
fi

# build docker images
if ! $mvn ${MAVEN_ARGS} clean install spring-boot:build-image; then
    echo -e "\n  ❌ building docker images failed\n"
    exit 1
fi

# result info
echo -e "\n  ✅ holy shit, it works! ✅\n"
echo -e "  run with:\n"
echo -e "docker run --rm -it --name api -p 1337:1337 ${docker_registry}/${docker_image_prefix}/api:${docker_tag}\n"
echo -e "docker run --rm -it --name collector -p 7331:7331 ${docker_registry}/${docker_image_prefix}/collector:${docker_tag}\n"

exit 0
