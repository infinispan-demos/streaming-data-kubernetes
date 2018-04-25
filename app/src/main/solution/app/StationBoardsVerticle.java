package app;

import app.infinispan.InfinispanRxMap;
import app.model.Station;
import app.model.Stop;
import app.model.Train;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import static app.AppUtils.rxReadFile;
import static app.model.ModelUtils.TO_UTF8;
import static app.model.ModelUtils.orNull;

public class StationBoardsVerticle extends AbstractVerticle {

  static final Logger log = Logger.getLogger(StationBoardsVerticle.class.getName());

  private InfinispanRxMap<String, Stop> stationBoardsMap;
  private Disposable injectorDisposable;

  private long progressTimer;
  private Disposable progressDisposable;

  private Disposable continuousQueryDisposable;

  @Override
  public void start(io.vertx.core.Future<Void> future) {
    log.info("Start station boards verticle");

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
      .flatMap(x -> addContinuousQuery())
      .doOnSuccess(disposable -> continuousQueryDisposable = disposable)
      .flatMap(x -> inject())
      .doOnSuccess(disposable -> injectorDisposable = disposable)
      .subscribe(
        x -> {
          log.info("Station boards verticle started");
          future.complete();
        }
        , future::fail
      );
  }

  private Single<Disposable> addContinuousQuery() {
    final Disposable disposable =
      stationBoardsMap.continuousQuery("FROM Stop s WHERE s.delayMin > 0")
      .subscribe(
        pair ->
          vertx.eventBus()
            .publish("delayed-trains", toJson(pair.getValue()))
        , t ->
          log.log(Level.SEVERE, "Error adding continuous query", t)
      );

    return Single.just(disposable);
  }

  private Single<Disposable> inject() {
    final String fileName = "/data/cff-stop-2016-02-29__.jsonl.gz";

    final Disposable disposable =
      stationBoardsMap.clear()
      .andThen(trackProgress(stationBoardsMap))
      .andThen(rxReadFile(fileName))
      .map(StationBoardsVerticle::toEntry)
      .flatMapCompletable(e -> stationBoardsMap.put(e.getKey(), e.getValue()))
      .subscribe(
        () -> log.info("Reached end")
        , t -> log.log(
          Level.SEVERE
          , "Error while loading station boards"
          , t
        )
      );

    return Single.just(disposable);
  }

  private Completable trackProgress(InfinispanRxMap<String, Stop> map) {
    progressTimer = vertx.setPeriodic(5000L, l -> {
      progressDisposable =
        map.size()
          .subscribe(
            size -> log.info("[station-boards] Progress: stored=" + size)
          );
    });

    return Completable.complete();
  }

  @Override
  public void stop(io.vertx.core.Future<Void> future) {
    if (progressDisposable != null) {
      progressDisposable.dispose();
      vertx.cancelTimer(progressTimer);
    }

    if (continuousQueryDisposable != null)
      continuousQueryDisposable.dispose();

    if (injectorDisposable != null)
      injectorDisposable.dispose();

    stationBoardsMap.removeContinuousQueries()
      .andThen(stationBoardsMap.close())
      .subscribe(
        () -> {
          log.info("Stopped station boards verticle");
          future.complete();
        }
        , future::fail
      );
  }

  private static Entry<String, Stop> toEntry(String line) {
    JsonObject json = new JsonObject(line);
    String trainName = json.getString("name");
    String trainTo = json.getString("to");
    String trainCat = json.getString("category");
    String trainOperator = json.getString("operator");

    Train train = new Train(trainName, trainTo, trainCat, trainOperator);

    JsonObject jsonStop = json.getJsonObject("stop");
    JsonObject jsonStation = jsonStop.getJsonObject("station");
    long stationId = Long.parseLong(jsonStation.getString("id"));
    String stationName = jsonStation.getString("name");
    Station station = new Station(stationId, stationName);

    Date departureTs = new Date(jsonStop.getLong("departureTimestamp") * 1000);
    int delayMin = orNull(jsonStop.getValue("delay"), 0);

    String stopId = String.format(
      "%s/%s/%s/%s",
      stationId, trainName, trainTo, jsonStop.getString("departure")
    );

    Stop stop = new Stop(train, delayMin, station, departureTs);

    return new SimpleImmutableEntry<>(stopId, stop);
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
