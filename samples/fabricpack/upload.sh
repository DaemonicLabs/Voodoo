#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

rsync -vvr $DIR/_upload/voodoo/ nikky@nikky.moe:/usr/share/nginx/nikky/html/.mc/experimental/