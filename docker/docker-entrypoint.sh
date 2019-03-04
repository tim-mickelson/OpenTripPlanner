#!/bin/bash

: ${GRAPH_FILE_TARGET_PATH="/code/otpdata/norway/"}
: ${FILE_TMP_PATH="/tmp/graph_obj_from_gcs/Graph.obj"}
: ${ROUTER_CONFIG="router-config.json"}
# Notice ending slash here, it is correct
: ${FILENAME="https://storage.googleapis.com/marduk-production/graphs/1.3.4.RB-SNAPSHOT/netex-20190228041737-Graph.obj"}


echo "GRAPH_FILE_TARGET_PATH: $GRAPH_FILE_TARGET_PATH"


DOWNLOAD="${FILENAME}"
echo "Downloading $DOWNLOAD"
wget -nv $DOWNLOAD -P $GRAPH_FILE_TARGET_PATH

exec "$@"