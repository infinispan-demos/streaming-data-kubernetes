package app;

import app.infinispan.InfinispanRxMap;
import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.util.logging.Level;
import java.util.logging.Logger;

import static io.reactivex.Single.just;

public class Main extends AbstractVerticle {

  static final Logger log = Logger.getLogger(Main.class.getName());

  private Disposable injector;

  private String stationBoardsVerticleId;
  private String trainPositionsVerticleId;

  @Override
  public void start(io.vertx.core.Future<Void> future) {
    Router router = Router.router(vertx);
    router.get("/test").handler(this::test);
    router.get("/inject").handler(this::inject);
    router.get("/inject/stop").handler(this::injectStop);

    // Backup in case fully offline and Google Maps not available
    router.get("/positions").handler(this::positions);

    router.route("/eventbus/delayed-trains/*")
      .handler(AppUtils.sockJSHandlerAndInject("delayed-trains", vertx));

    router.route("/eventbus/delayed-positions/*")
      .handler(AppUtils.sockJSHandler("delayed-positions", vertx));

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

  private void positions(RoutingContext rc) {
    // TODO: Customise this generated block
    rc.response().end("TODO");
  }

  private void inject(RoutingContext rc) {
    if (injector != null)
      injector.dispose();

    injector = undeployVerticles()
      .andThen(vertx.rxDeployVerticle(StationBoardsVerticle.class.getName()))
      .doOnSuccess(id -> stationBoardsVerticleId = id)
      .flatMap(x -> vertx.rxDeployVerticle(TrainPositionsVerticle.class.getName()))
      .doOnSuccess(id -> trainPositionsVerticleId = id)
      .subscribe(
        x ->
          rc.response().end("Injectors started")
        ,
        failure -> {
          log.log(Level.SEVERE, "Failure starting injectors", failure);
          rc.response().end("Failure starting injectors: " + failure);
        }
      );
  }

  private Completable undeployVerticles() {
    return undeployVerticle(stationBoardsVerticleId)
      .andThen(undeployVerticle(trainPositionsVerticleId))
      .onErrorComplete();
  }

  private Completable undeployVerticle(String id) {
    if (id != null) {
      log.info("Undeploy verticle with id: " + id);
      return vertx.rxUndeploy(id);
    }

    return Completable.complete();
  }

  private void injectStop(RoutingContext rc) {
    undeployVerticles()
      .subscribe(
        () -> rc.response().end("Injectors stopped")
        , t -> {
          log.log(Level.SEVERE, "Failure stopping injectors", t);
          rc.response().end("Failure stopping injectors: " + t);
        }
      );
  }

  private void test(RoutingContext rc) {
    ConfigurationBuilder testCfg = new ConfigurationBuilder();

    testCfg
      .addServer()
          .host("datagrid-hotrod")
          .port(11222);

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
