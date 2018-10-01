#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

cd $DIR

$DIR/gradlew :generateDependencyGraph

dot -Tpng build/reports/dependency-graph/dependency-graph.dot > dependency-graph.png
echo $DIR/dependency-graph.png
