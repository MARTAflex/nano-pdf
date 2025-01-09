#!/bin/bash
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

CODE_VERSION=$(bash get-next-version.sh $CURRENT_VERSION $1)

FULL_VERSION="$CODE_VERSION.$IMAGE_VERSION"
TAG="nanopdf:$FULL_VERSION"

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

# Source the SDKMAN! init script to set up the environment
[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"

# make sure we have the right sdk versions
sdk env install
./gradlew clean build


docker build -f deploy/Dockerfile -t martaflex/nanopdf:latest . \
&& docker tag martaflex/nanopdf $TAG \
&& echo build finshed --- $FULL_VERSION


