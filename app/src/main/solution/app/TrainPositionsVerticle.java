package app;

import app.infinispan.InfinispanRxMap;
import app.model.GeoLocBearing;
import app.model.TimedPosition;
import app.model.TrainPosition;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static app.AppUtils.rxReadFile;
import static app.model.ModelUtils.TO_UTF8;
import static app.model.ModelUtils.orNull;

public class TrainPositionsVerticle extends AbstractVerticle {

  static final Logger log = Logger.getLogger(TrainPositionsVerticle.class.getName());

  private InfinispanRxMap<String, TrainPosition> trainPositionsMap;
  private Disposable injectorDisposable;

  private long progressTimer;
  private Disposable progressDisposable;

  private Map<String, String> trainNamesToIds = new HashMap<>();
  private Disposable delayedTrainsDisposable;

  private Long trainPositionsTimer;
  private Disposable trainPositionsDisposable;

  @Override
  public void start(io.vertx.core.Future<Void> future) {
    log.info("Start train positions verticle");

    ConfigurationBuilder cfg = new ConfigurationBuilder();

    cfg
      .addServer()
      .host("datagrid-hotrod")
      .port(11222);

    InfinispanRxMap
      .<String, TrainPosition>createIndexed(
        "train-positions"
        , new Class[] {
            GeoLocBearing.class
            , TimedPosition.class
            , TrainPosition.class
        }
        , cfg
        , vertx
      )
      .doOnSuccess(map -> this.trainPositionsMap = map)
      .flatMap(x -> addEventBusConsumer())
      .doOnSuccess(d -> delayedTrainsDisposable = d)
      .flatMap(x -> startTrainPositionsPublisher())
      .doOnSuccess(timer -> trainPositionsTimer = timer)
      .flatMap(x -> inject())
      .doOnSuccess(d -> injectorDisposable = d)
      .subscribe(
        x -> {
          log.info("Train positions verticle started");
          future.complete();
        }
        , future::fail
      );
  }

  private Single<Disposable> addEventBusConsumer() {
    Disposable disposable = vertx.eventBus()
      .<String>consumer("delayed-trains")
      .toFlowable()
      .subscribe(
        msg -> {
          final JsonObject json = new JsonObject(msg.body());
          trainNamesToIds.put(json.getString("trainName"), "");
        }
        , t -> log.log(Level.SEVERE, "Error consuming event bus messages", t)
        , () -> log.info("Reached end")
      );

    return Single.just(disposable);
  }

  private Single<Long> startTrainPositionsPublisher() {
    long timer = vertx.setPeriodic(3000, l -> publishPositions());
    return Single.just(timer);
  }

  private void publishPositions() {
    trainPositionsDisposable = showTrainPositions()
      .subscribe(
        positions -> {
          log.info("Publishing positions:");
          log.info(positions);
          vertx.eventBus().publish("delayed-positions", positions);
        }
      );
  }

  private Single<String> showTrainPositions() {
    List<Single<String>> trainIdLookups = trainNamesToIds.entrySet().stream()
        .map(this::getTrainId)
        .collect(Collectors.toList());

    return Flowable
      .fromIterable(trainIdLookups)
      .flatMapSingle(s -> s) // Flatten the flowable
      .filter(trainId -> !trainId.isEmpty()) // Ignore empty
      .flatMapMaybe(trainId -> trainPositionsMap.get(trainId))
      .map(trainPosition ->
        String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s",
          trainPosition.getTrainId()
          , TO_UTF8.apply(trainPosition.getCategory())
          , trainPosition.getTrainName()
          , TO_UTF8.apply(trainPosition.getLastStopName())
          , trainPosition.getCurrent().getPosition().getLat()
          , trainPosition.getCurrent().getPosition().getLng()
          , trainPosition.getCurrent().getPosition().getBearing()
        )
      )
      .toList()
      .map(l ->
        "train_id\ttrain_category\ttrain_name\ttrain_lastStopName\tposition_lat\tposition_lng\tposition_bearing\n"
          + l.stream().collect(Collectors.joining("\n"))
      )
    ;
  }

  private Single<String> getTrainId(Map.Entry<String, String> trainEntry) {
    if (!trainEntry.getValue().isEmpty())
      return Single.just(trainEntry.getValue());

    String trainName = trainEntry.getKey();

    final String queryString =
      "select tp.trainId from TrainPosition tp where trainName = :name";

    Map<String, Object> queryParams = new HashMap<>();
    queryParams.put("name", trainName);

    return trainPositionsMap
      .<Object[]>query(queryString, queryParams)
      .map(train -> train[0]) // only interested in first field
      .cast(String.class) // field is String
      .doOnNext(trainId -> trainNamesToIds.put(trainName, trainId))
      .first("");
  }

  private Single<Disposable> inject() {
    final String fileName = "/data/cff_train_position-2016-02-29__.jsonl.gz";

    final Disposable disposable =
      trainPositionsMap.clear()
        .andThen(trackProgress(trainPositionsMap))
        .andThen(rxReadFile(fileName))
        .map(TrainPositionsVerticle::toEntry)
        .flatMapCompletable(e -> trainPositionsMap.put(e.getKey(), e.getValue()))
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

  private Completable trackProgress(InfinispanRxMap<String, TrainPosition> map) {
    progressTimer = vertx.setPeriodic(5000L, l -> {
      progressDisposable =
        map.size()
          .subscribe(
            size -> log.info("[train-positions] Progress: stored=" + size)
          );
    });

    return Completable.complete();
  }

  private static Map.Entry<String, TrainPosition> toEntry(String line) {
    JsonObject json = new JsonObject(line);

    String trainId = json.getString("trainid");
    long ts = json.getLong("timeStamp");
    String name = json.getString("name").trim();
    String cat = json.getString("category").trim();
    String lastStopName = json.getString("lstopname").trim();
    int delay = Integer.valueOf(orNull(json.getString("delay"), "0"));

    double y = Double.parseDouble(json.getString("y")) / 1000000;
    double x = Double.parseDouble(json.getString("x")) / 1000000;
    String dirOrEmpty = json.getString("direction");
    Double direction = dirOrEmpty.isEmpty() ? null : Double.parseDouble(dirOrEmpty) * 10;
    TimedPosition current = new TimedPosition(ts, new GeoLocBearing(y, x, direction));

    // TODO: Parse future positions to get continuous move (poly field)

    TrainPosition trainPosition =
        new TrainPosition(trainId, name, delay, cat, lastStopName, current);
    return new AbstractMap.SimpleImmutableEntry<>(trainId, trainPosition);
  }

  @Override
  public void stop(io.vertx.core.Future<Void> future) {
    if (progressDisposable != null) {
      progressDisposable.dispose();
      vertx.cancelTimer(progressTimer);
    }

    if (delayedTrainsDisposable != null)
      delayedTrainsDisposable.dispose();

    if (trainPositionsDisposable != null) {
      trainPositionsDisposable.dispose();
      vertx.cancelTimer(trainPositionsTimer);
    }

    if (injectorDisposable != null)
      injectorDisposable.dispose();

    trainPositionsMap.close()
      .subscribe(
        () -> {
          log.info("Stopped station boards verticle");
          future.complete();
        }
        , future::fail
      );
  }

}
