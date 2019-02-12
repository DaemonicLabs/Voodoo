#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

cd $DIR

$DIR/gradlew processMDTemplates
if [ ! $? -eq 0 ]; then
    echo "Error Compiling server-installer"
    exit 1
fi

cd wiki

git add *
git commit -m "updating wiki"
git push
