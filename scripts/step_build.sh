#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

pack=$1

cd $DIR
[ ! -e run ] && mkdir run
cd run

[ ! -e "$DIR/run/$pack/$pack.hjson" ] && echo "pack does not exist" && exit -1

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

rm src/**/*.lock.json


echo
echo "building $1"
echo

$DIR/gradlew -p "$DIR" :voodoo:run --args "build '$pack/$pack.pack.hjson' -o '$pack/$pack.lock.json' $2"
if [ ! $? -eq 0 ]; then
    echo "Error Building $pack"
    exit 1
fi