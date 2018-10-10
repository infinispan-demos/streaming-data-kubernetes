#!/usr/bin/env bash

set -e -x

minishift profile set streaming-data-kubernetes
minishift config set memory 8GB
minishift config set cpus 4
minishift config set disk-size 50g
minishift config set vm-driver xhyve
minishift config set image-caching true

minishift addon enable admin-user
minishift addon enable anyuid
