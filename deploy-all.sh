#!/bin/bash

set -e -x

(cd ./app; ./first-deploy.sh)
(cd ./visual; ./deploy.sh)
