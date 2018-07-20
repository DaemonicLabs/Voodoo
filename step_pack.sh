#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

pack=$1

cd $DIR
[ ! -e run ] && mkdir run
cd run

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

echo
echo "packaging $1"
echo


$DIR/gradlew -p "$DIR" :voodoo:run --args "pack sk $pack.lock.json"
if [ ! $? -eq 0 ]; then
    echo "Error Packing $pack"
    exit 1
fi

# echo validating skpackaging with original
# java -jar /home/nikky/dev/Launcher/launcher-builder/build/libs/launcher-builder-4.3-SNAPSHOT-all.jar --version 1.0 --input $DIR/run/$pack/workspace/$pack --output $DIR/run/$pack/workspace/_upload2 --manifest-dest $DIR/run/$pack/workspace/_upload2/$pack.json
