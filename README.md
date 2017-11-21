# Introduction

This is a repository containing a demo based on transport.opendata.ch data to show how to working with streaming data.
The aim of the demo is to show how to use OpenShift, together with Vert.x and Infinispan to analyse such streaming data.

# Pre-requisites

* OpenShift Origin 3.7
* Docker 1.13.1 (recommended, but should work with higher versions too)
* Maven 3

This demo could be run alternatively using [Minishift](https://www.openshift.org/minishift).

# Running Demo

Start by running OpenShift by executing the `start-local-openshift.sh` script at the root of this demo:

    ./start-local-openshift.sh

This script starts OpenShift together with the service catalog, and installs Infinispan ephemeral and persistent templates. 
Infinispan ephemeral stores all data in memory and persistent template provides persistent volume for the data as well as storing it in memory. 

Next, head to OpenShift Console in [https://127.0.0.1:8443/console](https://127.0.0.1:8443/console) from Chrome.

Once there, select Infinispan ephemeral service and give it these parameters, leaving the rest of parameters as they are:

* `APPLICATION_NAME`: datagrid
* `MANAGEMENT_PASSWORD`: developer
* `MANAGEMENT_USER`: developer
* `NUMBER_OF_INSTANCES`: 3

Click `Next`, then click `Close`.

While the data grid loads, start the graphical [Infinispan visualizer]():

    cd visual
    oc project myproject
    oc new-build --binary --name=visual
    oc start-build visual --from-dir=. --follow
    oc new-app visual
    oc expose service visual

Verify the visualizer shows 3 nodes via  [http://visual-myproject.127.0.0.1.nip.io/infinispan-visualizer](http://visual-myproject.127.0.0.1.nip.io/infinispan-visualizer) URL.

Next, build and deploy the main application:

    cd app
    mvn fabric8:deploy

Go to the visualizer and switch to `repl` cache. 

Then, switch to terminal and execute:

    curl http://app-myproject.127.0.0.1.nip.io/test

It should return something similar to this:

    {
      "get" : "world",
      "topology" : "[172.17.0.5:11222, 172.17.0.9:11222, 172.17.0.6:11222]"
    }

You should also see one dot appearing in each of the nodes in the visualizer for cache `repl`.
This represents that each node has an entry, which is what the test endpoint does.

Next, switch the visualizer to the "default" cache.

Call up the injector so that data is injected:

    curl http://app-myproject.127.0.0.1.nip.io/inject

Once the injector started, check the visualizer and see how each node has more and more dots around it.

Finally, execute `dashboard.Dashboard` from the IDE and you should see a dashboard appearing with delayed trains. 
