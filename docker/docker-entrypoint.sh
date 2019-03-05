#!/bin/bash

: ${GRAPH_FILE_TARGET_PATH="/code/otpdata/norway/Graph.obj"}
: ${FILENAME="https://storage.googleapis.com/marduk-production/graphs/1.3.4.RB-SNAPSHOT/netex-20190228041737-Graph.obj"}
: ${ROUTER_CONFIG_TARGET_PATH="/code/otpdata/norway/router-config.json"}
: ${ROUTER_CONFIG_URL="https://storage.googleapis.com/marduk/graphs/neon-graph/router-config.json"}

echo "GRAPH_FILE_TARGET_PATH: $GRAPH_FILE_TARGET_PATH"

echo "Downloading $FILENAME"
wget -nv $FILENAME -O $GRAPH_FILE_TARGET_PATH
echo "Downloading $ROUTER_CONFIG_TARGET_PATH"
wget -nv $ROUTER_CONFIG_URL -O $ROUTER_CONFIG_TARGET_PATH

exec "$@"