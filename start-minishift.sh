#!/usr/bin/env bash

set -e -x

minishift profile set streaming-data-kubernetes
minishift stop
minishift start
minishift ssh -- sudo setenforce 0

eval $(minishift oc-env)
eval $(minishift docker-env)

oc login -u system:admin
oc adm policy add-scc-to-user hostaccess system:serviceaccount:myproject:default -n myproject

BINARIES_HOME="swiss-transport-binaries"
if [ ! -d "$BINARIES_HOME" ]; then
  git clone https://github.com/infinispan-demos/swiss-transport-binaries
fi

minishift hostfolder add \
  -t sshfs \
  --source /Users/g/1/streaming-data-kubernetes/swiss-transport-binaries \
  --target /mnt/sda1/swiss-transport-binaries \
  swiss-transport-binaries \
  || true

minishift hostfolder mount swiss-transport-binaries

minishift console
