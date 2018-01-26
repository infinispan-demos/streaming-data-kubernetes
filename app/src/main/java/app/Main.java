package app;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Single;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.util.logging.Level;
import java.util.logging.Logger;

import static io.reactivex.Single.just;

public class Main extends AbstractVerticle {

  static final Logger log = Logger.getLogger(Main.class.getName());

  @Override
  public void start(io.vertx.core.Future<Void> future) {
    Router router = Router.router(vertx);
    router.get("/test").handler(this::test);

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .rxListen(8080)
      .subscribe(
        server -> {
          log.info("HTTP server started");
          future.complete();
        },
        future::fail
      );
  }

  private void test(RoutingContext rc) {
    vertx
      .rxExecuteBlocking(Main::remoteCacheManager)
      .flatMap(remote -> vertx.rxExecuteBlocking(remoteCache(remote)))
      .flatMap(cache -> CompletableInterop.fromFuture(cache.putAsync("hello", "world")).andThen(just(cache)))
      .flatMap(cache -> Single.fromFuture(cache.getAsync("hello")))
      .subscribe(
        value ->
          rc.response().end(value)
        , failure ->
          rc.response().end("Failure: " + failure.toString())
      )
    ;
  }

  private static void remoteCacheManager(Future<RemoteCacheManager> f) {
    f.complete(
      new RemoteCacheManager(
        new ConfigurationBuilder().addServer()
          .host("datagrid-hotrod")
          .port(11222)
          .build()));
  }

  private static Handler<Future<RemoteCache<String, String>>> remoteCache(RemoteCacheManager remote) {
    return f -> f.complete(remote.getCache("repl"));
  }

}
