#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PWD=$(pwd)

cd $DIR

$DIR/gradlew :server-installer:build

if [ ! $? -eq 0 ]; then
    echo "Error building server-installer"
    exit 1
fi

pack=$1

[ ! -e run ] && mkdir run
cd run

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

[ ! -e ".server" ] && mkdir ".server"
cd ".server"

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

echo
echo "installing $1 server"
echo

pwd

rm -rf run/

java -jar "$DIR/server-installer/build/libs/server-installer-fat.jar" run
if [ ! $? -eq 0 ]; then
    echo "Error Installing $pack"
    exit 1
fi