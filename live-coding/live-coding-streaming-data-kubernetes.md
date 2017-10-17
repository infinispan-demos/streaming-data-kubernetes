<div id="table-of-contents">
<h2>Table of Contents</h2>
<div id="text-table-of-contents">
<ul>
<li><a href="#sec-1">1. Pre Talk</a></li>
<li><a href="#sec-2">2. Live coding</a>
<ul>
<li><a href="#sec-2-1">2.1. Create Infinispan data grid</a></li>
<li><a href="#sec-2-2">2.2. Start Visualizer</a></li>
<li><a href="#sec-2-3">2.3. Test Infinispan data grid</a></li>
<li><a href="#sec-2-4">2.4. Integrate data injector</a></li>
<li><a href="#sec-2-5">2.5. Add Continuous Query Listener</a></li>
</ul>
</li>
<li><a href="#sec-3">3. Extras</a>
<ul>
<li><a href="#sec-3-1">3.1. Increase number of replicas and show visualizer dealing with it</a></li>
</ul>
</li>
</ul>
</div>
</div>

# Pre Talk<a id="sec-1" name="sec-1"></a>

Start OpenShift

    cd ~/1/streaming-data-kubernetes
    ./start-openshift.sh

Verify OpenShift is running

    oc project

# Live coding<a id="sec-2" name="sec-2"></a>

## Create Infinispan data grid<a id="sec-2-1" name="sec-2-1"></a>

Open <https://127.0.0.1:8443/> in Chrome

Log in as usr/pwd developer/developer

Click \`Infinispan Ephemeral\`

Explain differences between Ephemeral and Persistent

Change app name to \`datagrid\`

Change management usr/pwd to developer/developer

Change number of instances to 3

Click Next

Do not create binding and click Create

## Start Visualizer<a id="sec-2-2" name="sec-2-2"></a>

While data grid loads, start visualizer:

    cd visual
    oc project myproject
    oc new-build --binary --name=visual
    oc start-build visual --from-dir=. --follow
    oc new-app visual
    oc expose service visual

Verify visualizer is working:
<http://visual-myproject.127.0.0.1.nip.io/infinispan-visualizer/>

## Test Infinispan data grid<a id="sec-2-3" name="sec-2-3"></a>

Create a Main verticle in app project

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

Build and deploy app project

    oc project myproject
    oc new-build --binary --name=app
    mvn clean package
    oc start-build app --from-dir=. --follow
    oc new-app app
    oc expose service app

Switch visualizer to \`repl\` cache
Switch to terminal and make sure visualizer is in background
From terminal, execute:

    curl http://app-myproject.127.0.0.1.nip.io/test

## Integrate data injector<a id="sec-2-4" name="sec-2-4"></a>

Add a route for /inject and start the Injector verticle

    router.get("/inject").handler(this::inject);

    private void inject(RoutingContext ctx) {
      vertx.deployVerticle(Injector.class.getName(), new DeploymentOptions());
      ctx.response().end("Injector started");
    }

Redeploy the app

    mvn clean package
    oc start-build app --from-dir=. --follow

Switch visualizer to default cache

Switch to terminal and make sure visualizer is in background

From terminal, start the injector invoking:

    curl http://app-myproject.127.0.0.1.nip.io/inject

## Add Continuous Query Listener<a id="sec-2-5" name="sec-2-5"></a>

Implement continuous query listener

    private void addContinuousQuery(RemoteCache<String, Stop> stopsCache) {
      QueryFactory qf = Search.getQueryFactory(stopsCache);
    
      Query query = qf.from(Stop.class)
        .having("delayMin").gt(0)
        .build();
    
      ContinuousQueryListener<String, Stop> listener =
          new ContinuousQueryListener<String, Stop>() {
        @Override
        public void resultJoining(String key, Stop value) {
          vertx.runOnContext(x -> {
            vertx.eventBus().publish("delayed-trains", toJson(value));
          });
        }
      };
    
      continuousQuery = Search.getContinuousQuery(stopsCache);
      continuousQuery.addContinuousQueryListener(query, listener);
    }

Add evenbus route for sending events back to dashboard

    router.get("/eventbus/*").handler(AppUtils.sockJSHandler(vertx));

Make /inject route deploy the continuous query listener

    vertx.deployVerticle(Listener.class.getName(), new DeploymentOptions());

Redeploy the app

    mvn clean package
    oc start-build app --from-dir=. --follow

Switch to terminal and make sure visualizer is in background

From terminal, start the injector invoking:

    curl http://app-myproject.127.0.0.1.nip.io/inject

Run Dashboard from IDE and check that delayed trains are received

# Extras<a id="sec-3" name="sec-3"></a>

## Increase number of replicas and show visualizer dealing with it<a id="sec-3-1" name="sec-3-1"></a>
