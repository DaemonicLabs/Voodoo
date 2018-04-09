#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PWD=$(pwd)

cd $DIR

$DIR/gradlew :voodoo:build

[ ! -e run ] && mkdir run
cd run

echo
echo "flattening $1"
echo

java -jar "$DIR/voodoo/build/libs/voodoo-2.0.0.jar" flatten "$DIR/samples/$1.yaml"

echo
echo "building $1"
echo

java -jar "$DIR/voodoo/build/libs/voodoo-2.0.0.jar" build $1.json -o $1.lock.json --force

echo
echo "packaging $1"
echo

java -jar "$DIR/voodoo/build/libs/voodoo-2.0.0.jar" pack $1.lock.json sk