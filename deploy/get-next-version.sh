#!/bin/sh
SCRIPT_DIR=$(dirname "$0")
BASE_DIR=$SCRIPT_DIR/../

BUMP_TYPE=$1;
if [ -z $BUMP_TYPE ]; then
    BUMP_TYPE='patch'
fi

containsElement () {
    local e match="$1"
    shift
    for e; do
        [[ "$e" == "$match" ]] && return 0;
    done
    return 1
}

CURRENT=$(sed -n 's/version = "\(.*\)"/\1/p' "$SCRIPT_DIR/../build.gradle")

VALID=('patch' 'minor' 'major' 'prerelease' 'preminor' 'prepatch')

if [[ "$BUMP_TYPE" == "no-bump" ]]; then
    echo $CURRENT;
    exit;
fi

if containsElement $BUMP_TYPE "${VALID[@]}"; then
    semver -i "$BUMP_TYPE" $CURRENT --preid 'rc'
else
    echo $1
fi
