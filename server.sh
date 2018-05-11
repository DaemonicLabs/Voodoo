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
echo "packaging $1 server"
echo

java -jar "$DIR/voodoo/build/libs/voodoo-2.1.0.jar" pack $pack.lock.json server
if [ ! $? -eq 0 ]; then
    echo "Error Packing $pack"
    exit 1
fi