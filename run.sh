#!/bin/bash

set -e -x

APP=datagrid
USR=developer
PASS=developer
NUM_NODES=3
NS=myproject

oc login -u developer -p developer
oc create -f openshift/infinispan-centos7-imagestream.json || true
oc create -f openshift/infinispan-ephemeral-template.json || true

oc project ${NS}

# Create datagrid
oc process -n ${NS} infinispan-ephemeral -p \
  NUMBER_OF_INSTANCES=${NUM_NODES} \
  NAMESPACE=${NS} \
  APPLICATION_NAME=${APP} \
  APPLICATION_USER=${USR} \
  APPLICATION_PASSWORD=${PASS} \
  MANAGEMENT_USER=${USR} \
  MANAGEMENT_PASSWORD=${PASS} \
  | oc create -f - || true

# Deploy visualizer
(cd ./visual; ./deploy.sh)

# Deploy app
(cd ./app; ./first-deploy.sh)

# TODO: Wait until curl returns success

# Test
# curl http://app-myproject.127.0.0.1.nip.io/test

# Inject
# curl http://app-myproject.127.0.0.1.nip.io/inject

echo "Inject and start dashboard"

# Deploy map
(cd ./web-viewer/; ./start.sh)
