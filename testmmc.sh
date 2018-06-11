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
echo "testing $1 on multimc"
echo

java -jar "$DIR/voodoo/build/libs/voodoo.jar" test mmc $pack.lock.json
if [ ! $? -eq 0 ]; then
    echo "Error Testing $pack in multimc"
    exit 1
fi