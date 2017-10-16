package app;

import app.model.Station;
import app.model.Stop;
import app.model.Train;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import rx.Observable;
import rx.functions.Actions;
import rx.observables.StringObservable;
import rx.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static java.util.logging.Level.SEVERE;

public class Injector extends AbstractVerticle {

  static final Logger log = Logger.getLogger(Injector.class.getName());

  RemoteCacheManager client;

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    vertx.<RemoteCacheManager>rxExecuteBlocking(fut -> fut.complete(createRemoteCacheManager()))
      .doOnSuccess(rcm -> client = rcm)
      //.<Void>map(x -> null)
      .flatMap(v -> vertx.<RemoteCache<String, Stop>>rxExecuteBlocking(fut -> fut.complete(client.getCache())))
      //.doOnSuccess(remoteCache -> stopsCache = remoteCache).<Void>map(x -> null)
      .subscribe(cache -> {
        startFuture.complete(null);
        inject(cache);
      }, startFuture::fail);
  }

  @Override
  public void stop() throws Exception {
    if (client != null)
      client.stop();
  }

  private void inject(RemoteCache<String, Stop> stopsCache) {
    stopsCache.clear(); // Remove data on start, to start clean

    vertx.setPeriodic(5000L, l -> {
      vertx.executeBlocking(fut -> {
        log.info(String.format("Progress: stored=%d%n", stopsCache.size()));
        fut.complete();
      }, false, ar -> {});
    });

    rxReadGunzippedTextResource("cff-stop-2016-02-29__.jsonl.gz")
      .map(this::toEntry)
//      .doAfterTerminate(() -> {
//        final long duration = System.nanoTime() - loadStart;
//        log.info(String.format(
//          "Duration: %d(s) %n", TimeUnit.NANOSECONDS.toSeconds(duration)
//        ));
//        loadStart = System.nanoTime();
//        stopsLoaded.set(0);
//      })
      .repeat()
      .doOnNext(entry -> stopsCache.put(entry.getKey(), entry.getValue()))
      //.doOnNext(entry -> stopsLoaded.incrementAndGet())
      .subscribe(Actions.empty(),
        t -> log.log(SEVERE, "Error while loading station boards", t));
  }

  private RemoteCacheManager createRemoteCacheManager() {
    RemoteCacheManager client = new RemoteCacheManager(
      new ConfigurationBuilder().addServer()
        .host("datagrid-hotrod")
        .port(11222)
        .marshaller(ProtoStreamMarshaller.class)
        .build());

    SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(client);
    try {
      ctx.registerProtoFiles(FileDescriptorSource.fromResources("app-model.proto"));
      ctx.registerMarshaller(new Stop.Marshaller());
      ctx.registerMarshaller(new Station.Marshaller());
      ctx.registerMarshaller(new Train.Marshaller());
      return client;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Observable<String> rxReadGunzippedTextResource(String resource) {
    Objects.requireNonNull(resource);
    URL url = Injector.class.getClassLoader().getResource(resource);
    Objects.requireNonNull(url);

    return StringObservable
      .using(() -> {
        InputStream inputStream = url.openStream();
        InputStream gzipStream = new GZIPInputStream(inputStream);
        Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
        return new BufferedReader(decoder);
        }, StringObservable::from)
      .compose(StringObservable::byLine)
      .subscribeOn(Schedulers.io());
  }

  private Entry<String, Stop> toEntry(String line) {
    JsonObject json = new JsonObject(line);
    String trainName = json.getString("name");
    String trainTo = json.getString("to");
    String trainCat = json.getString("category");
    String trainOperator = json.getString("operator");

    Train train = Train.make(trainName, trainTo, trainCat, trainOperator);

    JsonObject jsonStop = json.getJsonObject("stop");
    JsonObject jsonStation = jsonStop.getJsonObject("station");
    long stationId = Long.parseLong(jsonStation.getString("id"));
    String stationName = jsonStation.getString("name");
    Station station = Station.make(stationId, stationName);

    Date departureTs = new Date(jsonStop.getLong("departureTimestamp") * 1000);
    int delayMin = orNull(jsonStop.getValue("delay"), 0);

    String stopId = String.format(
      "%s/%s/%s/%s",
      stationId, trainName, trainTo, jsonStop.getString("departure")
    );

    Stop stop = Stop.make(train, delayMin, station, departureTs);

    return new SimpleImmutableEntry<>(stopId, stop);
  }

  @SuppressWarnings("unchecked")
  static <T> T orNull(Object obj, T defaultValue) {
    return Objects.isNull(obj) ? defaultValue : (T) obj;
  }

}
