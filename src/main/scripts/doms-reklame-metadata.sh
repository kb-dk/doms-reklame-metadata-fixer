#!/bin/bash
SCRIPT_DIR="$(dirname $(readlink -f $0))"
java -Dlogback.configurationFile="$SCRIPT_DIR/../conf/logback.xml" -jar doms-reklame-metadata-fixer.jar "$@"