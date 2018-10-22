#!/bin/bash

: ${GRAPH_FILE_TARGET_PATH="/code/otpdata/norway/Graph.obj"}
: ${FILE_TMP_PATH="/tmp/graph_obj_from_gcs"}
: ${ROUTER_CONFIG="router-config.json"}
# Notice ending slash here, it is correct
: ${MARDUK_GCP_BASE="gs://marduk/"}
: ${FILENAME="graphs/neon-graph/Graph.obj"}


echo "GRAPH_FILE_TARGET_PATH: $GRAPH_FILE_TARGET_PATH"

echo "Activating marduk blobstore service account"
/code/google-cloud-sdk/bin/gcloud auth activate-service-account --key-file /etc/marduk/marduk-blobstore-credentials.json

DOWNLOAD="${MARDUK_GCP_BASE}${FILENAME}"
echo "Downloading $DOWNLOAD"
/code/google-cloud-sdk/bin/gsutil cp $DOWNLOAD $FILE_TMP_PATH

# Testing exists and has a size greater than zero
if [ -s $FILE_TMP_PATH ] ;
then
  echo "Overwriting $GRAPH_FILE_TARGET_PATH"
  mv $FILE_TMP_PATH $GRAPH_FILE_TARGET_PATH
else
  echo "** WARNING: Downloaded file ($FILE_TMP_PATH) is empty or not present**"
  echo "** Not overwriting $GRAPH_FILE_TARGET_PATH**"
  wget -q --header 'Content-Type: application/json' --post-data='{"source":"otp", "message":":no_entry: Downloaded file is empty or not present. This makes OTP fail! Please check logs"}' http://hubot/hubot/say/
  echo "Now sleeping 5m in the hope that this will be manually resolved in the mean time, and then restarting."
  sleep 5m
  exit 1
fi

echo "Using router config $ROUTER_CONFIG"
if [ -s $ROUTER_CONFIG ] ;
then
  cp /code/$ROUTER_CONFIG /code/otpdata/norway/router-config.json
else
  echo "** WARNING:($ROUTER_CONFIG) is empty or not present**"
  wget -q --header 'Content-Type: application/json' --post-data='{"source":"otp", "message":":no_entry: Could not find router config file: $ROUTER_CONFIG."}' http://hubot/hubot/say/
  sleep 10m
fi

exec "$@"