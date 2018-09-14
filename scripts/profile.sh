#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"
PWD=$(pwd)

cd $DIR

$DIR/gradlew clean :build -x test

if [ ! $? -eq 0 ]; then
    echo "Error building voodoo"
    exit 1
fi

pack=$1
mkdir -p "$DIR/profile"
PROFILE="$DIR/profile/$pack.profile.txt"
rm $PROFILE

[ ! -e "$DIR/samples/$pack.yaml" ] && echo "pack does not exist" && exit -1

[ ! -e run ] && mkdir run
cd run

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

echo
echo "importing $1"
echo

rm src/**/*.lock.hjson
rm src/**/*.entry.hjson

# TIMEFORMAT="%E"
exec 3>&1 4>&2
import_time=$({ time java -jar "$DIR/build/libs/voodoo.jar" import yaml "$DIR/samples/$pack.yaml" 1>&3 2>&4; } 2>&1)
status=$?
exec 3>&- 4>&-
if [ ! $status -eq 0 ]; then
    echo "Error importing $pack"
    exit 1
fi

echo
echo "building $1"
echo

exec 3>&1 4>&2
build_time=$({ time java -jar "$DIR/build/libs/voodoo.jar" build $pack.pack.hjson -o $pack.lock.hjson $2 1>&3 2>&4; } 2>&1)
status=$?
exec 3>&- 4>&-
if [ ! $status -eq 0 ]; then
    echo "Error Building $pack"
    exit 1
fi

echo
echo "packaging $1"
echo


exec 3>&1 4>&2
pack_time=$({ time java -jar "$DIR/build/libs/voodoo.jar" pack sk $pack.lock.hjson 1>&3 2>&4; } 2>&1)
status=$?
exec 3>&- 4>&-
if [ ! $status -eq 0 ]; then
    echo "Error Packing $pack"
    exit 1
fi

cd $DIR
cat >$PROFILE <<EOL
import time: $import_time
build time: $build_time
pack time: $pack_time
EOL
cat $PROFILE