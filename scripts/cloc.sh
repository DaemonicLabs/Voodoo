#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"
cd $DIR

cloc . --match-d="src/main" --not-match-d="run" --not-match-d=".gradle" --fullpath
cloc . --match-d="src/test" --not-match-d="run" --not-match-d=".gradle" --fullpath