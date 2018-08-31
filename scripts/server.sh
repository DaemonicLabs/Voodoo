#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

cd $DIR

pack=$1

[ ! -e run ] && mkdir run
cd run

[ ! -e "$DIR/run/$pack/$pack.lock.json" ] && echo "pack does not exist" && exit -1

rm -rf ".server/$pack"

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

echo
echo packing $pack server
echo

$DIR/gradlew -p "$DIR" :voodoo:run --args "pack server $pack/$pack.lock.json -o '$DIR/run/.server/$pack'"
if [ ! $? -eq 0 ]; then
    echo "Error packing $pack server"
    exit 1
fi