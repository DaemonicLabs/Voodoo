#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"
PWD=$(pwd)

cd $DIR

$DIR/gradlew clean publishToMavenLocal
if [ ! $? -eq 0 ]; then
    echo "Error compiling voodoo"
    exit 1
fi

id=$1
url=$2

[ ! -e samples ] && mkdir samples
cd samples

./gradlew import --id "$id" --url "$url" -Si
if [ ! $? -eq 0 ]; then
    echo "Error packing curse of $pack"
    exit 1
fi
