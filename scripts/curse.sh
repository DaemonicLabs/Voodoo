#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"
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
echo "packaging $1 voodoo.data.curse"
echo

java -jar "$DIR/build/libs/voodoo.jar" pack $pack.lock.json curse
if [ ! $? -eq 0 ]; then
    echo "Error Packing $pack"
    exit 1
fi