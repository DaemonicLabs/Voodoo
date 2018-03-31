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

java -jar "$DIR/voodoo/build/libs/voodoo.jar" flatten "$PWD/$1.yaml"

echo
echo "building $1"
echo

java -jar "$DIR/voodoo/build/libs/voodoo.jar" build $1.json --save -o $1.lock.json

echo
echo "packaging $1"
echo

java -jar "$DIR/voodoo/build/libs/voodoo.jar" pack $1.lock.json sk