#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

pack=$1
url=$2

cd $DIR
[ ! -e run ] && mkdir run
cd run

[ ! -e "$pack" ] && mkdir "$pack"
cd "$pack"

rm src/**/*.lock.json
rm src/**/*.entry.hjson

echo
echo "importing $pack"
echo

rm -rf $pack

$DIR/gradlew -p "$DIR"  :voodoo:run --args "import curse $url $pack"
if [ ! $? -eq 0 ]; then
    echo "Error importing $pack from curse"
    exit 1
fi