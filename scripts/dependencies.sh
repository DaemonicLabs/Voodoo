#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"

cd $DIR

$DIR/gradlew :cleanGenerateDependencyGraph :generateDependencyGraph

dot -Tsvg build/reports/dependency-graph/dependency-graph.dot > dependency-graph.svg
echo $DIR/dependency-graph.png
