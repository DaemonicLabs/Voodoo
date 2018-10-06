#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"
PWD=$(pwd)

cd $DIR

$DIR/gradlew clean publishToMavenLocal :poet
if [ ! $? -eq 0 ]; then
    echo "Error compiling voodoo"
    exit 1
fi

pack=$1

[ ! -e run ] && mkdir run
cd run

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

echo
echo "building $1"
echo

# find . -name \*.entry.hjson -type f -delete
# find . -name \*.lock.hjson -type f -delete

kscript "$DIR/samples/$pack.kt" pack sk
if [ ! $? -eq 0 ]; then
    echo "Error importing $pack"
    exit 1
fi

echo validating skpackaging with original
java -jar /home/nikky/dev/Launcher/launcher-builder/build/libs/launcher-builder-4.3-SNAPSHOT-all.jar \
  --version 1.0 \
  --input $DIR/run/$pack/workspace/$pack \
  --output $DIR/run/$pack/workspace/_upload2 \
  --manifest-dest $DIR/run/$pack/workspace/_upload2/$pack.json
