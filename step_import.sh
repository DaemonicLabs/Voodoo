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
url=$2

[ ! -e run ] && mkdir run
cd run

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

echo
echo "importing $pack"
echo

java -jar "$DIR/voodoo/build/libs/voodoo.jar" import yaml "$DIR/samples/$pack.yaml" .
if [ ! $? -eq 0 ]; then
    echo "Error importing $pack from yaml"
    exit 1
fi