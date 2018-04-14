#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PWD=$(pwd)

cd $DIR

$DIR/gradlew :voodoo:build

pack=$1

[ ! -e run ] && mkdir run
cd run


echo
echo "creating modlist for $1"
echo

java -jar "$DIR/voodoo/build/libs/voodoo-2.0.0.jar" tome $pack.lock.json template.md --header header.md --footer footer.md
if [ ! $? -eq 0 ]; then
    echo "Error Packing $pack"
    exit 1
fi