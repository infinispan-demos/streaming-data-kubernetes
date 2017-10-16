package app;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.net.SocketAddress;
import java.util.Set;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public class Main extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    Router router = Router.router(vertx);
    router.get("/test").handler(this::test);

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8080, ar -> {
        if (ar.succeeded())
          System.out.println("Server started");

        startFuture.handle(ar.mapEmpty());
      });

    vertx.deployVerticle(Injector.class.getName(), new DeploymentOptions());
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
