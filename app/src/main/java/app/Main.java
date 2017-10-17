package app;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
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
    router.get("/test").blockingHandler(this::test);
    router.get("/inject").handler(this::inject);
    router.route("/eventbus/*").handler(this.sockJSHandler());

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8080, ar -> {
        if (ar.succeeded()) {
          System.out.println("Server started");
          startFuture.complete();
        } else {
          startFuture.fail(ar.cause());
        }
      });
  }

  private void inject(RoutingContext ctx) {
    Future<String> injectorFuture = Future.future();
    vertx.deployVerticle(Injector.class.getName(), injectorFuture);
    Future<String> listenerFuture = Future.future();
    vertx.deployVerticle(Listener.class.getName(), listenerFuture);
    CompositeFuture.all(injectorFuture, listenerFuture).setHandler(ar -> {
      if (ar.succeeded()) {
        ctx.response().end("Injector started");
      } else {
        ctx.fail(ar.cause());
      }
    });
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

    client.getCache("repl").put("hello", "world");
    Object value = client.getCache("repl").get("hello");

    Set<SocketAddress> topology =
      client.getCache("repl").getCacheTopologyInfo().getSegmentsPerServer().keySet();

    JsonObject rsp = new JsonObject()
      .put("get", value)
      .put("topology", topology.toString());

    ctx.response()
        .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
        .end(rsp.encodePrettily());

    client.stop();
  }

}
