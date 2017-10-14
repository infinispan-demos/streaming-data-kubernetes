#!/usr/bin/env bash

set -e -x

(cd ./streaming-backend; ./deploy.sh)

(cd ./streaming-feeder; ./deploy.sh)
