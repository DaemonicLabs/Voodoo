#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

pack=$1

cd $DIR

$DIR/gradlew server-installer:build
if [ ! $? -eq 0 ]; then
    echo "Error Compiling server-installer"
    exit 1
fi

$DIR/scripts/server.sh $pack

[ ! -e samples ] && mkdir samples
cd samples

[ ! -e run ] && mkdir run
cd run

[ ! -e "$DIR/samples/run/.server/$pack" ] && echo "server pack does not exist" && exit -1
cd "$DIR/samples/run/.server/$pack"

echo
echo "installing $1 server"
echo

cd "$DIR/run/.server/$pack"

java -jar $DIR/server-installer/build/libs/server-installer-dev.jar "$DIR/samples/run/.server/${pack}_run" --file pack.lock.hjson --clean
# $DIR/gradlew -p "$DIR" :server-installer:run --args "'$DIR/run/.server/${pack}_run' --file '.server/$pack/pack.lock.hjson' --clean"
if [ ! $? -eq 0 ]; then
    echo "Error Installing $pack"
    exit 1
fi
