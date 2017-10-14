#!/usr/bin/env bash

set -e -x

./activator clean docker:stage docker:publishLocal
