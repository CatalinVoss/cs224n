#!/bin/bash
python compile.py .
if [ "$?" -ne 0 ]; then echo "Invalid Syntax";exit 1;
fi

python error_wrap.py

parse -g *.grammar $@
