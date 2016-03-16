#!/usr/bin/env bash
# Rather than forking a subshell, execute all commands
# in java-config.sh in the current shell.
source ./config.inc

# Build OpenEphyra
echo -e "./compile-openephyra.sh: `pwd`"
echo -e "./compile-openephyra.sh: Building OpenEphyra..."

ant

echo "OpenEphyra done"

