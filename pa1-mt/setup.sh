#!/usr/bin/env bash
#
# TODO: You must run this script from this directory
# every time you login as follows:
#
#   source setup.sh
#

export LANG=en_US.utf8

#
# DO NOT CHANGE THESE PATHS!
#
export CORENLP="/afs/ir/class/cs224n/bin/corenlp"
export PHRASAL="/afs/ir/class/cs224n/bin/phrasal"

#
# Setup the CLASSPATH for Java and the PATH
# for Phrasal scripts
#
BASEDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export CLASSPATH="${CORENLP}/*:${PHRASAL}/*:${PHRASAL}/lib/*:${BASEDIR}/java/classes"
export PATH=${PATH}:${PHRASAL}/scripts

