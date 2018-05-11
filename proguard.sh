#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $DIR

$DIR/gradlew :voodoo:clean :voodoo:proguard

if [ ! $? -eq 0 ]; then
    echo "Error building voodoo"
    exit 1
fi

[ ! -e run ] && mkdir run
cd run

echo
echo "flattening $1"
echo

java -jar "$DIR/voodoo/proguard.jar" flatten "$DIR/samples/$1.yaml"

echo
echo "building $1"
echo

java -jar "$DIR/voodoo/proguard.jar" build $1.json --save -o $1.lock.json

echo
echo "packaging $1"
echo

java -jar "$DIR/voodoo/proguard.jar" pack $1.lock.json sk