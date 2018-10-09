#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

cd $DIR

$DIR/gradlew clean publishToMavenLocal
if [ ! $? -eq 0 ]; then
    echo "Error compiling voodoo"
    exit 1
fi

pack=$1

[ ! -e samples ] && mkdir samples
cd samples

./gradlew "$pack" --args "pack server -o '$DIR/run/.server/$pack'"
if [ ! $? -eq 0 ]; then
    echo "Error packing $pack server"
    exit 1
fi
