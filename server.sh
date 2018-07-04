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

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

rm -rf .server

echo
echo "packaging $1 server"
echo

java -jar "$DIR/voodoo/build/libs/voodoo.jar" pack server $pack.lock.json
if [ ! $? -eq 0 ]; then
    echo "Error Packing $pack"
    exit 1
fi