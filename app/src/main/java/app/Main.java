package app;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.net.SocketAddress;
import java.util.Set;
import java.util.logging.Logger;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public class Main extends AbstractVerticle {

  static final Logger log = Logger.getLogger(Main.class.getName());

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Router router = Router.router(vertx);
    router.get("/test").handler(this::test);
    router.route("/eventbus/*").handler(this.sockJSHandler());

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8080, ar -> {
        if (ar.succeeded())
          System.out.println("Server started");

        startFuture.handle(ar.mapEmpty());
      });

    vertx.deployVerticle(Injector.class.getName(), new DeploymentOptions());
    vertx.deployVerticle(Listener.class.getName(), new DeploymentOptions());
  }

  private Handler<RoutingContext> sockJSHandler() {
    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    PermittedOptions outPermit = new PermittedOptions().setAddress("delayed-trains");
    BridgeOptions options = new BridgeOptions().addOutboundPermitted(outPermit);
    sockJSHandler.bridge(options, be -> {
      if (be.type() == BridgeEventType.REGISTER)
        log.info("SockJs: client connected");

      be.complete(true);
    });
    return sockJSHandler;
  }

  private void test(RoutingContext ctx) {
    RemoteCacheManager client = new RemoteCacheManager(
      new ConfigurationBuilder().addServer()
        .host("datagrid-hotrod")
        .port(11222).build());

    client.getCache().put("hello", "world");
    Object value = client.getCache().get("hello");

    Set<SocketAddress> topology =
      client.getCache().getCacheTopologyInfo().getSegmentsPerServer().keySet();

    JsonObject rsp = new JsonObject()
      .put("get", value)
      .put("topology", topology.toString());

    ctx.response()
        .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
        .end(rsp.encodePrettily());

    client.stop();
  }

}
