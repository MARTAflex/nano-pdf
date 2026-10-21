#!/bin/bash
# stop on any failing command
set -e

USAGE="USAGE: ./release-docker.sh [bump_type] [image number]"

#if [ -z $1 ]
#then
#    echo $USAGE
#    exit;
#fi

IMAGE_VERSION=$2
if [ -z $2 ]
then
    IMAGE_VERSION=$(date +%Y%m%d%H%M%S)
fi

SCRIPT_DIR=$(dirname "$0")
REPODIR=$(realpath "$SCRIPT_DIR/../")

fail () {
    echo "ERROR: $1" >&2
    exit 1
}

# Source the SDKMAN! init script to set up the environment
[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] || fail "SDKMAN! not found in $HOME/.sdkman"
source "$HOME/.sdkman/bin/sdkman-init.sh"

# make sure we build with the versions from .sdkmanrc; this is checked before anything
# gets modified, since a version missing in SDKMAN! would otherwise silently fall back
# to the default jdk (e.g. tests failing on a newer java)
# NOTE: sdk env sets JAVA_HOME without checking that the jdk is installed, so check
#       that it is really there
pushd "$REPODIR" > /dev/null
sdk env install || fail "could not install the versions from .sdkmanrc"
sdk env || fail "could not switch to the versions from .sdkmanrc"
JAVA_VERSION=$(sed -n 's/^java=//p' .sdkmanrc)
[[ "$JAVA_HOME" == "$SDKMAN_CANDIDATES_DIR/java/$JAVA_VERSION" && -x "$JAVA_HOME/bin/java" ]] \
    || fail "java $JAVA_VERSION from .sdkmanrc is not installed; is it still available? (sdk list java)"
popd > /dev/null
# gradlew uses JAVA_HOME; java on PATH can be missing right after the first jdk install
"$JAVA_HOME/bin/java" -version

CODE_VERSION=$(bash get-next-version.sh $CURRENT_VERSION $1)

FULL_VERSION="$CODE_VERSION.$IMAGE_VERSION"
TAGBASE="ghcr.io/martaflex/nano-pdf"
TAG="$TAGBASE:$FULL_VERSION"

echo "IMAGE TAG:"
echo "    $TAG"
echo
#read -p ">>> Press enter to continue"
printf ">>> Press enter to continue"
read _

# FIXME: ctrl-c behavior is wierd ... kinda keeps going

sed -i -E "s/(nano-pdf-)[0-9]+\.[0-9]+\.[0-9]+/\1$CODE_VERSION/" ./Dockerfile

cd $REPODIR
sed -i 's/version = "\(.*\)"/version = "'$CODE_VERSION'"/' ./build.gradle
sed -i 's/version = "\(.*\)"/version = "'$CODE_VERSION'"/' ./src/main/routes/status.kt

./gradlew clean build || fail "build failed"

printf ">>> Checkpoint: Are all tests green? press enter to continue"
read _

# no && chains here: set -e ignores failures inside them
docker build -f deploy/Dockerfile -t martaflex/nano-pdf:latest .
docker tag martaflex/nano-pdf $TAG
docker tag martaflex/nano-pdf "$TAGBASE:latest"
echo build finshed --- $FULL_VERSION

printf ">>> Press enter to push image"
read _

docker push $TAG
docker push "$TAGBASE:latest"
