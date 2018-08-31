#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

pack=$1

$DIR/scripts/server.sh $pack

cd $DIR

[ ! -e run ] && mkdir run
cd run

[ ! -e "$DIR/run/.server/$pack" ] && echo "server pack does not exist" && exit -1
cd ".server/$pack"

echo
echo "installing $1 server"
echo

cd "$DIR/run/.server/$pack"

$DIR/gradlew -p "$DIR" :server-installer:run --args "'$DIR/run/.server/${pack}_run' --file '.server/$pack/pack.lock.json' --clean"
if [ ! $? -eq 0 ]; then
    echo "Error Installing $pack"
    exit 1
fi
