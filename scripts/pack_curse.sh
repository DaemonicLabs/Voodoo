#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"
PWD=$(pwd)

cd $DIR

$DIR/gradlew clean publishToMavenLocal :poet
if [ ! $? -eq 0 ]; then
    echo "Error compiling voodoo"
    exit 1
fi

pack=$1

[ ! -e samples ] && mkdir samples
cd samples

./gradlew "$pack" --args "build - pack curse"
if [ ! $? -eq 0 ]; then
    echo "Error importing $pack"
    exit 1
fi
