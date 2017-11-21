#!/bin/bash

set -e -x

./start-local-openshift.sh

APP=datagrid
USR=developer
PASS=developer
NUM_NODES=3
NS=myproject

oc project ${NS}

# Create datagrid
oc process -n openshift infinispan-ephemeral -p \
  NUMBER_OF_INSTANCES=${NUM_NODES} \
  NAMESPACE=${NS} \
  APPLICATION_NAME=${APP} \
  APPLICATION_USER=${USR} \
  APPLICATION_PASSWORD=${PASS} | oc create -f -

# Deploy app
cd app
mvn fabric8:deploy

# TODO: Wait until curl returns success

# Test
curl http://app-myproject.127.0.0.1.nip.io/test

# Inject
curl http://app-myproject.127.0.0.1.nip.io/inject

echo "Start dashboard"
