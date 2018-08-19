#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

pack=$1

cd $DIR
[ ! -e run ] && mkdir run
cd run

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

echo
echo "packaging $1"
echo


# $DIR/gradlew -p "$DIR" :voodoo:run --args "pack mmc-fat $pack/$pack.lock.json"
# if [ ! $? -eq 0 ]; then
#     echo "Error Packing $pack" fat
#     exit 1
# fi

$DIR/gradlew -p "$DIR" :voodoo:run --args "pack mmc $pack/$pack.lock.json"
if [ ! $? -eq 0 ]; then
    echo "Error Packing $pack"
    exit 1
fi