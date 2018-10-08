#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"
PWD=$(pwd)

cd $DIR

$DIR/gradlew clean
$DIR/gradlew publishToMavenLocal
if [ ! $? -eq 0 ]; then
    echo "Error compiling voodoo"
    exit 1
fi

pack=$1

[ ! -e samples ] && mkdir samples
cd samples

./gradlew "$pack" --args "build - pack sk"
if [ ! $? -eq 0 ]; then
    echo "Error importing $pack"
    exit 1
fi

#
# [ ! -e run ] && mkdir run
# cd run
#
# [ ! -e "$pack" ] && mkdir "$pack"
# cd "$pack"
#
# echo
# echo "building $1"
# echo
#
# find . -name \*.entry.hjson -type f -delete
# find . -name \*.lock.hjson -type f -delete
#
# kscript "$DIR/samples/$pack.kt" build - pack sk
# if [ ! $? -eq 0 ]; then
#     echo "Error importing $pack"
#     exit 1
# fi
#