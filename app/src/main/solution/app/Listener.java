package app;

import app.infinispan.InfinispanRxMap;
import app.model.Station;
import app.model.Stop;
import app.model.Train;
import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;

import static app.model.ModelUtils.TO_UTF8;

public class Listener extends AbstractVerticle {

  static final Logger log = Logger.getLogger(Listener.class.getName());

  private InfinispanRxMap<String, Stop> stationBoardsMap;
  private Disposable continuousQueryDisposable;

  @Override
  public void start(io.vertx.core.Future<Void> future) {
    ConfigurationBuilder cfg = new ConfigurationBuilder();

    cfg
      .addServer()
      .host("datagrid-hotrod")
      .port(11222);

    InfinispanRxMap
      .<String, Stop>createIndexed(
        "station-boards"
        , new Class[]{Train.class, Station.class, Stop.class}
        , cfg
        , vertx
      )
      .doOnSuccess(map -> this.stationBoardsMap = map)
      .flatMapCompletable(this::addContinuousQuery)
      .subscribe(
        future::complete
        , future::fail
      );
  }

  private Completable addContinuousQuery(InfinispanRxMap<String, Stop> map) {
    stopContinuousQuery();

    continuousQueryDisposable =
      map
        .removeContinuousQueries()
        .andThen(map.continuousQuery("FROM Stop s WHERE s.delayMin > 0"))
        .subscribe(
          pair ->
            vertx.eventBus()
              .publish("delayed-trains", toJson(pair.getValue()))
          , t ->
            log.log(Level.SEVERE, "Error adding continuous query", t))
        ;

    return Completable.complete();
  }

  private void stopContinuousQuery() {
    if (continuousQueryDisposable != null)
      continuousQueryDisposable.dispose();
  }

  @Override
  public void stop(io.vertx.core.Future<Void> future) {
    stopContinuousQuery();

    this.stationBoardsMap
      .removeContinuousQueries()
      .andThen(this.stationBoardsMap.close())
      .subscribe(
        CompletableHelper.toObserver(future)
      );
  }

  private static String toJson(Stop stop) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", TO_UTF8.apply(stop.train.getCategory()));
    map.put("departure", String.format("%tR", stop.departureTs));
    map.put("station", TO_UTF8.apply(stop.station.getName()));
    map.put("destination", TO_UTF8.apply(stop.train.getTo()));
    map.put("delay", stop.delayMin);
    map.put("trainName", TO_UTF8.apply(stop.train.getName()));
    return new JsonObject(map).encode();
  }

}