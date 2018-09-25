#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"
PWD=$(pwd)

cd $DIR

$DIR/gradlew clean publishToMavenLocal :cursePoet
if [ ! $? -eq 0 ]; then
    echo "Error compiling voodoo"
    exit 1
fi

pack=$1

[ ! -e run ] && mkdir run
cd run

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

echo
echo "testing $1 on multimc"
echo

kscript "$DIR/samples/$pack.kt" test mmc
if [ ! $? -eq 0 ]; then
    echo "Error Testing $pack in multimc"
    exit 1
fi