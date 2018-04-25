#!/bin/bash

set -e -x

oc cluster down
oc cluster up

oc login -u system:admin
oc adm policy add-scc-to-user hostaccess system:serviceaccount:myproject:default -n myproject

oc login -u developer -p developer https://127.0.0.1:8443
oc create -f openshift/infinispan-centos7-imagestream.json || true
oc create -f openshift/infinispan-ephemeral-template.json || true
