#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"
PWD=$(pwd)

cd $DIR

$DIR/gradlew :build -x test

if [ ! $? -eq 0 ]; then
    echo "Error building voodoo"
    exit 1
fi

pack=$1

[ ! -e "$DIR/samples/$pack.yaml" ] && echo "pack does not exist" && exit -1

[ ! -e run ] && mkdir run
cd run

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

echo
echo "importing $1"
echo

rm src/**/*.lock.json
rm src/**/*.entry.hjson

java -jar "$DIR/build/libs/voodoo.jar" import yaml "$DIR/samples/$pack.yaml"

echo
echo "building $1"
echo


java -jar "$DIR/build/libs/voodoo.jar" build $pack.pack.hjson -o $pack.lock.json
while [ $? -eq 0 ]; do
    rm src/**/*.lock.json
    java -jar "$DIR/build/libs/voodoo.jar" build $pack.pack.hjson -o $pack.lock.json
done