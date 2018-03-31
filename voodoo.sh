#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $DIR

$DIR/gradlew :flatten:build :builder:build :pack:build

[ ! -e run ] && mkdir run
cd run

echo
echo "flattening $1"
echo

java -jar "$DIR/flatten/build/libs/flatten.jar" $DIR/samples/$1.yaml

echo
echo "building $1"
echo

java -jar "$DIR/builder/build/libs/builder.jar" $1.json --save -o $1.lock.json

echo
echo "packaging $1"
echo

java -jar "$DIR/pack/build/libs/pack.jar" $1.lock.json sk