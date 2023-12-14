#!/bin/bash

bash ./_setup_env.sh
if [ "$?" -ne 0 ]; then
    echo "setup environment failed"
    exit 1
fi

bash ./_setup_exe.sh
