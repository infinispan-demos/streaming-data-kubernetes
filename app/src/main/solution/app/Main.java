package app;

import infinispan.rx.InfinispanRxMap;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.util.logging.Logger;

import static io.reactivex.Single.just;

public class Main extends AbstractVerticle {

  static final Logger log = Logger.getLogger(Main.class.getName());

  @Override
  public void start(io.vertx.core.Future<Void> future) {
    Router router = Router.router(vertx);
    router.get("/test").handler(this::test);
    router.get("/inject").handler(this::inject);
    router.get("/eventbus/*").handler(AppUtils.sockJSHandler(vertx));

    vertx
      .createHttpServer()
      .requestHandler(router::accept)
      .rxListen(8080)
      .subscribe(
        server -> {
          log.info("Http server started");
          future.complete();
        }
        , future::fail
      );
  }

  private void inject(RoutingContext rc) {
    vertx
      .rxDeployVerticle(Injector.class.getName())
      .flatMap(x -> vertx.rxDeployVerticle(Listener.class.getName()))
      .subscribe(
        x ->
          rc.response().end("Injector and listener started")
        ,
        failure ->
          rc.response().end("Failure: " + failure)
      );
  }

  private void test(RoutingContext rc) {
    Configuration testCfg =
      new ConfigurationBuilder()
        .addServer()
          .host("datagrid-hotrod")
          .port(11222)
        .build();

    InfinispanRxMap
      .create("repl", testCfg, vertx)
      .flatMap(map ->
        map
          .put("hello", "world")
          .andThen(just(map))
      )
      .flatMapMaybe(map ->
        map
          .get("hello")
          .doFinally(map::close)
      )
      .subscribe(
        value ->
          rc.response().end("Value was: " + value)
        , failure ->
          rc.response().end("Failed: " + failure)
        , () ->
          rc.response().end("No value returned")
      );
  }

}
