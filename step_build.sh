#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PWD=$(pwd)

pack=$1

[ ! -e run ] && mkdir run
cd run

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

echo
echo "building $1"
echo

cd $DIR
$DIR/gradlew :voodoo:run --args "build '$DIR/run/$pack/$pack.pack.hjson' -o '$DIR/run/$pack/$pack.lock.json' $2"
if [ ! $? -eq 0 ]; then
    echo "Error Building $pack"
    exit 1
fi