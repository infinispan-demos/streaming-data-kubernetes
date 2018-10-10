#!/usr/bin/env bash

set -e -x

# Application
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=app=app || true

# Visualizer
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=app=datagrid-visualizer || true
oc delete imagestream datagrid-visualizer || true
oc delete buildconfig datagrid-visualizer || true

# Datagrid
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=application=datagrid || true

# Templates and image streams
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=infinispan-ephemeral || true
oc delete template infinispan-ephemeral || true
oc delete imagestream infinispan || true
