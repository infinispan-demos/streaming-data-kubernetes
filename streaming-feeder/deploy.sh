#!/usr/bin/env bash

set -e -x

docker-compose build
docker-compose up
