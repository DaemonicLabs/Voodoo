#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

pack=$1

cd $DIR
[ ! -e run ] && mkdir run
cd run

[ ! -e "$DIR/run/$pack/$pack.lock.json" ] && echo "pack does not exist" && exit -1

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

echo
echo "testing $1 on multimc"
echo

$DIR/gradlew -p "$DIR" :run --args "test mmc $pack/$pack.lock.json"
if [ ! $? -eq 0 ]; then
    echo "Error Testing $pack in multimc"
    exit 1
fi