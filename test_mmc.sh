#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

pack=$1

cd $DIR
[ ! -e run ] && mkdir run
cd run

echo
echo "testing $1 on multimc"
echo

$DIR/gradlew -p "$DIR" :voodoo:run --args "test mmc $pack/$pack.lock.json"
if [ ! $? -eq 0 ]; then
    echo "Error Testing $pack in multimc"
    exit 1
fi