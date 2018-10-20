# Introduction

This is a repository containing a demo based on transport.opendata.ch and sbb.ch data to show how to work with streaming data.
The aim of the demo is to show how to use OpenShift, together with Vert.x and Infinispan to analyse such streaming data.
It uses RxJava2 APIs for transforming data and coordinate the actions.


# Pre-requisites

The necessary tooling varies depending on your target environment.


## Local pre-requisites 

* OpenShift Origin 3.7.2
* Docker 1.13.1 (recommended, but should work with higher versions too).
This demo has been tested with Docker 17.09 too.
* Maven 3
* Node.js 4.2 or higher.
If not already using Node.js, you can install 
[Node Version Manager](https://github.com/creationix/nvm)
to easily switch between different versions.

This demo could be run alternatively using [Minishift](https://www.openshift.org/minishift).


# Paths and URLs

## Local paths and URLs

When running in a local environment, use these paths or URLS for accessing components: 

* OpenShift Console: 
  * [`https://127.0.0.1:8443/console`](https://127.0.0.1:8443/console)
  * It is recommended that you use Google Chrome or Firefox for accessing it.

* Infinispan visualizer:
  * [`http://visual-myproject.127.0.0.1.nip.io/infinispan-visualizer`](http://visual-myproject.127.0.0.1.nip.io/infinispan-visualizer)  

* Application URL:
  * [`http://app-myproject.127.0.0.1.nip.io`](http://app-myproject.127.0.0.1.nip.io)

* Dashboard HTTP host path:
  * `-Dhttp.host=app-myproject.127.0.0.1.nip.io`


# Running Demo

Start OpenShift by calling:

```bash
./setup-local-openshift.sh
```

Head to OpenShift Console, click on `Select from Project` and select `My Project`.
Select Infinispan ephemeral template and give it these parameters, leaving the rest of parameters as they are:

* `APPLICATION_NAME`: datagrid
* `MANAGEMENT_PASSWORD`: developer
* `MANAGEMENT_USER`: developer
* `NUMBER_OF_INSTANCES`: 3

Click `Next`, then click `Close`.

While the data grid loads, start the graphical Infinispan visualizer:

```bash
cd visual
./deploy.sh
```

Verify the visualizer shows 3 nodes via Infinispan visualizer URL.

Next, build and deploy the main application:

```bash
cd app
./first-deploy.sh
```

Go to the visualizer and switch to `repl` cache. 

Then, switch to terminal and execute:

```bash
curl <app-url>/test
```

It should return something similar to this:

```
Value was: world
```

You should also see one dot appearing in each of the nodes in the visualizer for cache `repl`.
This represents that each node has an entry, which is what the test endpoint does.

Next, switch the visualizer to the `station-boards` cache.

Call up the injector so that data is injected:

```bash
curl <app-url>/inject
```

Once the injector started, check the visualizer and see how each node has more and more dots around it.

Next, execute `dashboard.Dashboard` from the IDE passing in the correct `-Dhttp.host` value. 
You should see a dashboard appearing with delayed trains.

For the final visualization, there's a Google Maps based web application that tracks positions of delayed trains.
First, start the web application by calling:

```bash
cd web-viewer
npm install
npm start
```

Alternatively, if using Node Version Manager you can simply call `start.sh` script.

Then, open a browser to address 
[`http://localhost:3000`](http://localhost:3000).
It should show an empty Google Maps.

Next, restart the dashboard and you should start seeing blue dots representing positions of delayed trains. 


# Live Events
 
Here's a list of conferences and user groups where this demo has been presented.
The `live-coding` folder contains step-by-step live coding instructions of the demos, as presented in these live events:

* 19th October 2017 - Basel One
(
[slides](https://speakerdeck.com/galderz/streaming-data-analysis-with-kubernetes)
|
video NA
|
[live demo steps](live-coding/basel-one-17.md)
)
* 24th November 2017 - Codemotion Madrid
(
[slides](https://speakerdeck.com/galderz/streaming-data-ni-pierdas-el-tren-ni-esperes-en-balde)
|
[video](https://www.youtube.com/watch?v=eu5LrbMm-KU)
|
[live demo steps](live-coding/codemotion-madrid-17.md)
)
* 6th February 2018 - JFokus 2018
(
[slides](https://speakerdeck.com/galderz/streaming-data-analysis-with-kubernetes-1)
|
video NA
|
[live demo steps](live-coding/jfokus-18.org)
)
* 26th April 2018 - Great Indian Developer Summit 2018
(
[slides](https://speakerdeck.com/galderz/streaming-data-analysis-with-kubernetes-2)
|
[screencast](https://www.youtube.com/watch?v=oU0oduarh94)
|
[live demo steps](live-coding/gids-18.org)
)
* 20th October 2018 - Voxxed Days Ticino 2018
(
[slides](https://speakerdeck.com/galderz/principles-and-patterns-for-streaming-data-analysis)
|
video NA
|
[live demo steps](live-coding/voxxed-ticino-18.org)
)
