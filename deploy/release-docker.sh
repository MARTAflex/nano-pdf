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

# Source the SDKMAN! init script to set up the environment
[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"

# make sure we have the right sdk versions
sdk env install
./gradlew clean build

printf ">>> Checkpoint: Are all tests green? press enter to continue"
read _

docker build -f deploy/Dockerfile -t martaflex/nano-pdf:latest . \
&& docker tag martaflex/nano-pdf $TAG \
&& docker tag martaflex/nano-pdf "$TAGBASE:latest" \
&& echo build finshed --- $FULL_VERSION

printf ">>> Press enter to push image"
read _

docker push $TAG \
&& docker push "$TAGBASE:latest"
