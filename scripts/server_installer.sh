#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"
PWD=$(pwd)

cd $DIR

pack=$1

[ ! -e run ] && mkdir run
cd run

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

rm -rf "$PWD/.server/$pack"
pwd
echo
echo packing $pack server
echo

$DIR/gradlew -p "$DIR" :voodoo:run --args "pack server $pack/$pack.lock.json -o '.server'"
if [ ! $? -eq 0 ]; then
    echo "Error packing $pack server"
    exit 1
fi

cd "$PWD/.server/$pack"

echo
echo "installing $1 server"
echo

cd "$DIR/run/.server/$pack"

$DIR/gradlew -p "$DIR" :server-installer:run --args ".server/$pack/run --file '.server/$pack/pack.lock.json'"
if [ ! $? -eq 0 ]; then
    echo "Error Installing $pack"
    exit 1
fi
