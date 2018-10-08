#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

cd $DIR

$DIR/gradlew clean publishToMavenLocal :poet
if [ ! $? -eq 0 ]; then
    echo "Error compiling voodoo"
    exit 1
fi

pack=$1

[ ! -e samples ] && mkdir samples
cd samples

[ ! -e run ] && mkdir run
cd run

[ ! -e "$DIR/run/$pack/$pack.lock.hjson" ] && echo "pack does not exist" && exit -1

rm -rf ".server/$pack"

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

echo
echo packing $pack server
echo

kscript "$DIR/samples/$pack.kt" pack server -o "$DIR/run/.server/$pack"
if [ ! $? -eq 0 ]; then
    echo "Error packing $pack server"
    exit 1
fi