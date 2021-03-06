#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

cd ${DIR}

${DIR}/gradlew clean
${DIR}/gradlew publishToMavenLocal
if [[ ! $? -eq 0 ]]; then
    echo "Error compiling voodoo"
    exit 1
fi

pack=$1

[[ ! -e samples ]] && mkdir samples
cd samples

./gradlew "$pack" --args "build" -S
if [[ ! $? -eq 0 ]]; then
    echo "Error building $pack"
    exit 1
fi
