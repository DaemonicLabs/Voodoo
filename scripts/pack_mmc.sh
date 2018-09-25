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

echo
echo "packaging $1 voodoo mmc"
echo

kscript "$DIR/samples/$pack.kt" pack mmc
if [ ! $? -eq 0 ]; then
    echo "Error Packing $pack"
    exit 1
fi
