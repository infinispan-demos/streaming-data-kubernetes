# Introduction

This is a repository containing a demo based on transport.opendata.ch data to show how to working with streaming data.
The aim of the demo is to show how to use OpenShift, together with Vert.x and Infinispan to analyse such streaming data.


# Pre-requisites

The necessary tooling varies depending on your target environment.

## Local pre-requisites 

* OpenShift Origin 3.7
* Docker 1.13.1 (recommended, but should work with higher versions too)
* Maven 3

This demo could be run alternatively using [Minishift](https://www.openshift.org/minishift).

## Google Cloud pre-requisites

* [Google Cloud](https://cloud.google.com) account.
* [Google Cloud SDK](https://cloud.google.com/sdk/) installed locally.


# Set up

Before the demo can be run, there's some set up to be done.
Once again, the set up varies depending on your target environment:

## Local set up

Start OpenShift by executing the `start-local-openshift.sh` script at the root of this demo:

    ./start-local-openshift.sh

This script starts OpenShift together with the service catalog, and installs Infinispan ephemeral template. 
Infinispan ephemeral stores all data in memory.

## Google Cloud set up

Create a project in Google Cloud.
The easiest thing is to use
[create-project.sh](google/create_project.sh)
script to do that:

    ./create_project.sh <name-project> <email-address>

Example:

    ./create_project.sh openshift-v1 me@here.com

At the Google Cloud level, the name of project needs to be unique. 
So the script combines the given project name with the project name to create the actual Google Cloud project name.
Given the example values above, the project name would be:

    openshift-v1-me-here-com

When you execute this script, a JSON file is generated in the folder where it's executed from.
This file will allow the process that creates the OpenShift cluster to authenticate with Google Cloud.
The name follows this pattern:

    <google-cloud-project-name>.json
    
Example:

    openshift-v1-me-here-com.json    

You will need to add name of this file (without json ending) to the OpenShift cluster definition file. 

To be able to create an OpenShift cluster, it's necessary to provide an SSH key.
The simplest thing is to create one in the folder where the OpenShift cluster descriptor will be located:

    $ ssh-keygen -f openshift-key

Finally, you need to missing details to OpenShift cluster definition file.
This project contains a sample descriptor called 
[cluster-streaming.yml](google/cluster-streaming.yml)
which you can use as starting point.
It already assumes that the SSH key is called `openshift-key`.
Using that file, the missing details to add are:

    gce:
      account: <google-cloud-project-name>.json
      ...
      project: <google-cloud-project-name>

With the example values above, these would be:

    gce:
      account: openshift-v1-me-here-com.json
      ...
      project: openshift-v1-me-here-com

Finally, you need to call create on
[OpenShifter](https://github.com/openshift-evangelists/openshifter)
project to create an OpenShift cluster with the details of YAML file.
Assuming you're calling `create` from the same directory where the YAML and SSH key files are located, this would be:

    docker run -e -ti -v `pwd`:/root/data docker.io/osevg/openshifter create cluster-streaming

If you get any errors, you can destroy the OpenShift cluster executing:

    docker run -e -ti -v `pwd`:/root/data docker.io/osevg/openshifter destroy cluster-streaming

In case you get any errors when destroying the cluster, you can fully remove all the elements by doing:

    ./delete-resources.sh cluster-streaming

Once you have your OpenShift cluster running, the Infinispan template needs to be installed.
You can do this by calling `start-gcp-openshift.sh` script:

    ./start-gcp-openshift.sh <openshift-master-IP-address> cluster-streaming

This script connects to the OpenShift cluster and installs Infinispan ephemeral template.
Infinispan ephemeral stores all data in memory.


# Paths and URLs

## Local paths and URLs

When running in a local environment, use these paths or URLS for accessing components: 

* OpenShift Console: [https://127.0.0.1:8443/console](https://127.0.0.1:8443/console).
It is recommended that you use Google Chrome or Firefox for accessing it.

* Infinispan visualizer: [http://visual-myproject.127.0.0.1.nip.io/infinispan-visualizer](http://visual-myproject.127.0.0.1.nip.io/infinispan-visualizer).  

* Application URL: [http://app-myproject.127.0.0.1.nip.io](http://app-myproject.127.0.0.1.nip.io).

* Dashboard HTTP host path:`-Dhttp.host=app-myproject.127.0.0.1.nip.io`

## Google Cloud paths and URLs

When running on Google, use these paths or URLS for accessing components: 

* OpenShift Console: [https://console.cluster-streaming.<openshift-master-ip>.nip.io:8443/console](https://console.cluster-streaming.<openshift-master-IP-address>.nip.io:8443/console).
It is recommended that you use Google Chrome or Firefox for accessing it.

* Infinispan visualizer: [http://visual-myproject.apps.cluster-streaming.<openshift-master-ip>.nip.io/infinispan-visualizer](http://visual-myproject.apps.cluster-streaming.<<openshift-master-ip>>.nip.io/infinispan-visualizer).  

* Application URL: [http://app-myproject.apps.cluster-streaming.<openshift-master-ip>.nip.io](http://app-myproject.apps.cluster-streaming.<openshift-master-ip>.nip.io).

* Dashboard HTTP host path:`-Dhttp.host=app-myproject.apps.cluster-streaming.<openshift-master-ip>.nip.io`


# Running Demo

Head to OpenShift Console, click on `Select from Project` and select `My Project`.
Select Infinispan ephemeral template and give it these parameters, leaving the rest of parameters as they are:

* `APPLICATION_NAME`: datagrid
* `MANAGEMENT_PASSWORD`: developer
* `MANAGEMENT_USER`: developer
* `NUMBER_OF_INSTANCES`: 3

Click `Next`, then click `Close`.

While the data grid loads, start the graphical Infinispan visualizer:

    cd visual
    oc project myproject
    oc new-build --binary --name=visual
    oc start-build visual --from-dir=. --follow
    oc new-app visual
    oc expose service visual

Verify the visualizer shows 3 nodes via Infinispan visualizer URL.

Next, build and deploy the main application:

    cd app
    mvn fabric8:deploy

Go to the visualizer and switch to `repl` cache. 

Then, switch to terminal and execute:

    curl <app-url>/test

It should return something similar to this:

    {
      "get" : "world",
      "topology" : "[172.17.0.5:11222, 172.17.0.9:11222, 172.17.0.6:11222]"
    }

You should also see one dot appearing in each of the nodes in the visualizer for cache `repl`.
This represents that each node has an entry, which is what the test endpoint does.

Next, switch the visualizer to the `default` cache.

Call up the injector so that data is injected:

    curl <app-url>/inject

Once the injector started, check the visualizer and see how each node has more and more dots around it.

Finally, execute `dashboard.Dashboard` from the IDE passing in the correct `-Dhttp.host` value. 
You should see a dashboard appearing with delayed trains. 
