#!/bin/bash

# Runs OpenEphyra in command line mode.
# Usage: OpenEphyra.sh [assert_dir]

# The '-server' option of the Java VM improves the runtime of Ephyra.
# We recommend using 'java -server' if your VM supports this option.

export CLASSPATH=bin:lib/ml/maxent.jar:lib/ml/minorthird.jar:lib/nlp/jwnl.jar:lib/nlp/lingpipe.jar:lib/nlp/opennlp-tools.jar:lib/nlp/plingstemmer.jar:lib/nlp/snowball.jar:lib/nlp/stanford-ner.jar:lib/nlp/stanford-parser.jar:lib/nlp/stanford-postagger.jar:lib/qa/javelin.jar:lib/search/bing-search-java-sdk.jar:lib/search/googleapi.jar:lib/search/indri.jar:lib/search/yahoosearch.jar:lib/util/commons-logging.jar:lib/util/gson.jar:lib/util/htmlparser.jar:lib/util/log4j.jar:lib/util/trove.jar:lib/util/commons-codec-1.9.jar
export ASSERT=$1

cd ..

source ./config.inc

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/thrift/thrift-0.9.3/lib/java/build
export CLASSPATH=$CLASSPATH:/usr/local/thrift/thrift-0.9.3/lib/java/build
export JAVA_CLASS_PATH=$JAVA_CLASS_PATH:/usr/local/thrift/thrift-0.9.3/lib/java/build

java -Djava.library.path=lib/search/ -server -Xms512m -Xmx1500m info.ephyra.OpenEphyra "$*"
