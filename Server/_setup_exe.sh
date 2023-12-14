#!/bin/bash

EXE_FOLDER="$(dirname "$(readlink -f "$0")")"
EXE_PATH="$EXE_FOLDER/spear_server"
echo $EXE_PATH
nohup $EXE_PATH -t tunSpear -a 10.233.0.0 -p 22333 -d 8.8.8.8 -f 22334 -m 256 > /dev/null 2>&1 &
