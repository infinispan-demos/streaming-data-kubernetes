package app;

import app.model.Station;
import app.model.Stop;
import app.model.Train;
import infinispan.rx.InfinispanRxMap;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.EventBus;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class Injector extends AbstractVerticle {

  static final Logger log = Logger.getLogger(Injector.class.getName());

  private InfinispanRxMap<String, Stop> map;
  private Disposable injector;

  private long progressTimer;
  private Disposable progressSize;

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
      .doOnSuccess(map -> this.map = map)
      .flatMapCompletable(x -> eventBusConsumer())
      .subscribe(
        () -> {
          inject(map);
          future.complete();
        }
        , future::fail
      );
  }

  private Completable eventBusConsumer() {
    EventBus eb = vertx.eventBus();

    eb.consumer("injector", message -> {
      log.info("Received message to stop injector");
      stopInject();
    });

    return Completable.complete();
  }

  private void inject(InfinispanRxMap<String, Stop> map) {
    stopInject();

    injector = map
      .clear()
      .andThen(trackProgress(map))
      .andThen(rxReadFile("cff-stop-2016-02-29__.jsonl.gz"))
      .zipWith(throttle(), (item, interval) -> item)
      .map(Injector::toEntry)
      .map(e -> map.put(e.getKey(), e.getValue()))
      .to(src -> Completable.merge(src, 100))
      .subscribe(
        () -> log.info("Reached end")
        , t -> log.log(
          Level.SEVERE
          , "Error while loading station boards"
          , t
        )
      );
  }

  private void stopInject() {
    if (injector != null) {
      injector.dispose();
      progressSize.dispose();
      vertx.cancelTimer(progressTimer);
    }
  }

  private static Flowable<Long> throttle() {
    return Flowable.interval(5, TimeUnit.MILLISECONDS).onBackpressureDrop();
  }

  private Completable trackProgress(InfinispanRxMap<String, Stop> map) {
    progressTimer = vertx.setPeriodic(5000L, l -> {
      progressSize =
        map
          .size()
          .subscribe(
            size -> log.info(String.format("Progress: stored=%d%n", size))
          );
    });

    return Completable.complete();
  }

  @Override
  public void stop(io.vertx.core.Future<Void> future) throws Exception {
    this.map
      .close()
      .subscribe(
        CompletableHelper.toObserver(future)
      );
  }

//  private void inject(RemoteCache<String, Stop> cache) {
//    stopsCache = cache;
//    stopsCache.clear(); // Remove data on start, to start clean
//
//    vertx.setPeriodic(5000L, l -> {
//      vertx.executeBlocking(fut -> {
//        log.info(String.format("Progress: stored=%d%n", stopsCache.size()));
//        fut.complete();
//      }, false, ar -> {});
//    });
//
//    rxReadGunzippedTextResource("cff-stop-2016-02-29__.jsonl.gz")
//      .map(this::toEntry)
//      .repeatWhen(notification -> notification.map(terminal -> {
//        log.info("Reached end of file, clear and restart");
//        stopsCache.clear(); // If it reaches the end of the file, start again
//        return Notification.createOnNext(null);
//      }))
//      .zipWith(Observable.interval(10, TimeUnit.MILLISECONDS), (item, interval) -> item)
//      .doOnNext(entry -> stopsCache.put(entry.getKey(), entry.getValue()))
//      .subscribe(Actions.empty(),
//        t -> log.log(SEVERE, "Error while loading station boards", t));
//  }

  private static Flowable<String> rxReadFile(String resource) {
    Objects.requireNonNull(resource);
    URL url = Injector.class.getClassLoader().getResource(resource);
    Objects.requireNonNull(url);

    return Flowable.<String, BufferedReader>generate(() -> {
      InputStream inputStream = url.openStream();
      InputStream gzipStream = new GZIPInputStream(inputStream);
      Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
      return new BufferedReader(decoder);
    }, (bufferedReader, emitter) -> {
      String line = bufferedReader.readLine();
      if (line != null) {
        emitter.onNext(line);
      } else {
        emitter.onComplete();
      }
    }, BufferedReader::close)
      .subscribeOn(Schedulers.io());
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

  @SuppressWarnings("unchecked")
  private static <T> T orNull(Object obj, T defaultValue) {
    return Objects.isNull(obj) ? defaultValue : (T) obj;
  }

}
