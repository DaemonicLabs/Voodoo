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
echo "packaging $1"
echo

java -jar "$DIR/voodoo/build/libs/voodoo.jar" pack sk $pack.lock.json
if [ ! $? -eq 0 ]; then
    echo "Error Packing $pack"
    exit 1
fi

echo validating skpackaging with original
java -jar /home/nikky/dev/Launcher/launcher-builder/build/libs/launcher-builder-4.3-SNAPSHOT-all.jar --version 1.0 --input $DIR/run/$pack/workspace/$pack --output $DIR/run/$pack/workspace/_upload2 --manifest-dest $DIR/run/$pack/workspace/_upload2/$pack.json
