#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

cd $DIR

$DIR/gradlew :generateDependencyGraph

if [ ! $? -eq 0 ]; then
    dot -Tpng build/reports/dependency-graph/dependency-graph.dot > dependency-graph.png
fi