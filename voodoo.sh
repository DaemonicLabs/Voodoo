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

echo
echo "importing $1"
echo

java -jar "$DIR/voodoo/build/libs/voodoo.jar" import yaml "$DIR/samples/$pack.yaml" .
if [ ! $? -eq 0 ]; then
    echo "Error importing $pack"
    exit 1
fi

echo
echo "building $1"
echo

java -jar "$DIR/voodoo/build/libs/voodoo.jar" build $pack.pack.hjson -o $pack.lock.json $2
if [ ! $? -eq 0 ]; then
    echo "Error Building $pack"
    exit 1
fi

echo
echo "packaging $1"
echo

java -jar "$DIR/voodoo/build/libs/voodoo.jar" pack sk $pack.lock.json
if [ ! $? -eq 0 ]; then
    echo "Error Packing $pack"
    exit 1
fi