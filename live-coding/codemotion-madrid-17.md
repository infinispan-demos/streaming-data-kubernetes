<div id="table-of-contents">
<h2>Table of Contents</h2>
<div id="text-table-of-contents">
<ul>
<li><a href="#sec-1">1. Home folders</a>
<ul>
<li><a href="#sec-1-1">1.1. Codemotion Madrid 2017</a>
<ul>
<li><a href="#sec-1-1-1">1.1.1. OpenShifter set up</a></li>
<li><a href="#sec-1-1-2">1.1.2. Live coding</a></li>
</ul>
</li>
</ul>
</li>
<li><a href="#sec-2">2. First time</a>
<ul>
<li><a href="#sec-2-1">2.1. Create project on GCP</a></li>
<li><a href="#sec-2-2">2.2. Create OpenShift cluster on project</a></li>
<li><a href="#sec-2-3">2.3. If something fails, destroy the cluster and then recreate it</a></li>
</ul>
</li>
<li><a href="#sec-3">3. Pre talk</a>
<ul>
<li><a href="#sec-3-1">3.1. Go to Google Cloud console and start master</a></li>
<li><a href="#sec-3-2">3.2. Try accessing console</a></li>
<li><a href="#sec-3-3">3.3. Login with OpenShift client to Google Cloud</a></li>
</ul>
</li>
<li><a href="#sec-4">4. Live Coding</a>
<ul>
<li><a href="#sec-4-1">4.1. Create Infinispan data grid</a></li>
<li><a href="#sec-4-2">4.2. Start Visualizer</a></li>
<li><a href="#sec-4-3">4.3. Test Infinispan data grid</a></li>
<li><a href="#sec-4-4">4.4. Integrate data injector</a></li>
<li><a href="#sec-4-5">4.5. Add Continuous Query Listener</a></li>
</ul>
</li>
</ul>
</div>
</div>

# Home folders<a id="sec-1" name="sec-1"></a>

## Codemotion Madrid 2017<a id="sec-1-1" name="sec-1-1"></a>

### OpenShifter set up<a id="sec-1-1-1" name="sec-1-1-1"></a>

### Live coding<a id="sec-1-1-2" name="sec-1-1-2"></a>

# First time<a id="sec-2" name="sec-2"></a>

## Create project on GCP<a id="sec-2-1" name="sec-2-1"></a>

e.g.

    ./create_project.sh os-v1 me@here.com

## Create OpenShift cluster on project<a id="sec-2-2" name="sec-2-2"></a>

    cd ~/0/events/codemotion_17/openshifter
    docker run -e -ti -v `pwd`:/root/data docker.io/osevg/openshifter create cluster-streaming

## If something fails, destroy the cluster and then recreate it<a id="sec-2-3" name="sec-2-3"></a>

    docker run -e -ti -v \`pwd\`:/root/data docker.io/osevg/openshifter destroy cluster-streaming
    ./delete-resources.sh cluster-streaming
    docker ps -a | awk '{ print $1,$2 }' | grep openshifter | awk '{print $1 }' | xargs -I {} docker rm {}

# Pre talk<a id="sec-3" name="sec-3"></a>

## Go to Google Cloud console and start master<a id="sec-3-1" name="sec-3-1"></a>

    <openshift-master-ip>

## Try accessing console<a id="sec-3-2" name="sec-3-2"></a>

<https://console.cluster-streaming.<openshift-master-ip>.nip.io:8443/console/>

## Login with OpenShift client to Google Cloud<a id="sec-3-3" name="sec-3-3"></a>

    ./setup-gcp-openshift.sh <openshift-master-ip> cluster-streaming

# Live Coding<a id="sec-4" name="sec-4"></a>

## Create Infinispan data grid<a id="sec-4-1" name="sec-4-1"></a>

-   [ ] Homepage / Select from Project
-   [ ] Select 'myproject'
-   [ ] Click on 'infinispan-ephemeral' and click 'Next'
-   [ ] Add details:

-   [ ] APPLICATION<sub>NAME</sub>: datagrid
-   [ ] MANAGEMENT<sub>USER</sub>: developer
-   [ ] MANAGEMENT<sub>PASSWORD</sub>: developer
-   [ ] NUMBER<sub>OF</sub><sub>INSTANCES</sub>: 3

## Start Visualizer<a id="sec-4-2" name="sec-4-2"></a>

-   [ ] While data grid loads, start visualizer

```
    cd visual
    oc project myproject
    oc new-build --binary --name=visual
    oc start-build visual --from-dir=. --follow
    oc new-app visual
    oc expose service visual
```

-   [ ] Verify visualizer is working

```
<http://visual-myproject.apps.cluster-streaming.<openshift-master-ip>.nip.io/infinispan-visualizer/>
```

## Test Infinispan data grid<a id="sec-4-3" name="sec-4-3"></a>

-   [ ] Create a Main verticle in app project

```
    @Override
    public void start(Future<Void> startFuture) throws Exception {
      Router router = Router.router(vertx);
      router.get("/test").handler(this::test);
    
      vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(8080, ar -> {
          if (ar.succeeded())
            log.info("Server started");
    
          startFuture.handle(ar.mapEmpty());
        });
    }
    
    private void test(RoutingContext ctx) {
      RemoteCacheManager client = new RemoteCacheManager(
        new ConfigurationBuilder().addServer()
          .host("datagrid-hotrod")
          .port(11222)
          .build());
    
      RemoteCache<Object, Object> cache = client.getCache("repl");
      cache.put("hello", "world");
      Object v = cache.get("hello");
    
      Set<SocketAddress> topology =
        cache.getCacheTopologyInfo().getSegmentsPerServer().keySet();
    
      JsonObject json = new JsonObject()
        .put("get", v.toString())
        .put("topology", topology.toString());
    
      ctx.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
        .end(json.encodePrettily());
    
      client.stop();
    }
```

-   [ ] Build and deploy app project

```
    cd app
    mvn fabric8:deploy
```

-   [ ] Switch visualizer to \`repl\` cache
-   [ ] Switch to terminal and make sure visualizer is in background
-   [ ] From terminal, execute:

```
    curl http://app-myproject.apps.cluster-streaming.<openshift-master-ip>.nip.io/test
```

## Integrate data injector<a id="sec-4-4" name="sec-4-4"></a>

-   [ ] Add a route for /inject and start the Injector verticle

```
    router.get("/inject").handler(this::inject);

    private void inject(RoutingContext ctx) {
      vertx.deployVerticle(Injector.class.getName(), new DeploymentOptions());
      ctx.response().end("Injector started");
    }
```

-   [ ] Redeploy the app

```
    mvn fabric8:deploy
```

-   [ ] Switch visualizer to default cache
-   [ ] Switch to terminal and make sure visualizer is in background
-   [ ] From terminal, start the injector invoking:

```
    curl http://app-myproject.apps.cluster-streaming.<openshift-master-ip>.nip.io/inject
```

## Add Continuous Query Listener<a id="sec-4-5" name="sec-4-5"></a>

-   [ ] Implement continuous query listener

```
    private void addContinuousQuery(RemoteCache<String, Stop> stopsCache) {
      QueryFactory qf = Search.getQueryFactory(stopsCache);
    
      Query query = qf.from(Stop.class)
        .having("delayMin").gt(0)
        .build();
    
      ContinuousQueryListener<String, Stop> listener =
          new ContinuousQueryListener<String, Stop>() {
        @Override
        public void resultJoining(String key, Stop value) {
          vertx.eventBus().publish("delayed-trains", toJson(value));
        }
      };
    
      continuousQuery = Search.getContinuousQuery(stopsCache);
      continuousQuery.addContinuousQueryListener(query, listener);
    }
```

-   [ ] Add evenbus route for sending events back to dashboard

```
    router.get("/eventbus/*").handler(AppUtils.sockJSHandler(vertx));
```

-   [ ] Make /inject route deploy the continuous query listener

```
    vertx.deployVerticle(Listener.class.getName(), new DeploymentOptions());
```

-   [ ] Redeploy the app

```
    mvn fabric8:deploy
```

-   [ ] Switch to terminal and make sure visualizer is in background
-   [ ] From terminal, start the injector invoking:

```
    curl http://app-myproject.apps.cluster-streaming.<openshift-master-ip>.nip.io/inject
```

-   [ ] Run Dashboard from IDE and check that delayed trains are received, passing in:

```
    -Dhttp.host=<openshift-master-ip>
```
