#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PWD=$(pwd)

cd $DIR

$DIR/gradlew :voodoo:build

if [ ! $? -eq 0 ]; then
    echo "Error building voodoo"
    exit 1
fi

pack=$1

[ ! -e run ] && mkdir run
cd run


echo
echo "creating modlist for $1"
echo

# java -jar "$DIR/voodoo/build/libs/voodoo.jar" tome changelog $(ls history/$pack.*.lock.json) --sort
java -jar "$DIR/voodoo/build/libs/voodoo.jar" tome credits $pack.lock.json template.md --sort
if [ ! $? -eq 0 ]; then
    echo "Error Packing $pack"
    exit 1
fi