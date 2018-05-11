#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PWD=$(pwd)

cd $DIR

$DIR/gradlew :server-installer:clean :server-installer:build

if [ ! $? -eq 0 ]; then
    echo "Error building voodoo"
    exit 1
fi

pack=$1

[ ! -e run ] && mkdir run
cd run

[ ! -e server ] && mkdir server
cd server

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

echo
echo "installing $1"
echo

pwd

java -jar "$DIR/server-installer/build/libs/server-installer-0.1.0.jar" "../install/$pack"
if [ ! $? -eq 0 ]; then
    echo "Error installing $pack"
    exit 1
fi