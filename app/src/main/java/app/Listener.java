package app;

import app.model.Stop;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.api.continuous.ContinuousQuery;
import org.infinispan.query.api.continuous.ContinuousQueryListener;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static app.AppUtils.createRemoteCacheManager;
import static org.infinispan.query.remote.client.ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME;

public class Listener extends AbstractVerticle {

  RemoteCacheManager client;
  ContinuousQuery<String, Stop> continuousQuery;

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    // TODO: Do I need Rx here?
    // TSE: you don't, but Rx makes coordinating async code easier
    vertx.<RemoteCacheManager>rxExecuteBlocking(fut -> fut.complete(createRemoteCacheManager()))
      .doOnSuccess(rcm -> client = rcm)
      .flatMap(x -> vertx.rxExecuteBlocking(fut -> fut.complete(addModelToServer())))
      .flatMap(v -> vertx.<RemoteCache<String, Stop>>rxExecuteBlocking(fut -> fut.complete(client.getCache())))
      .subscribe(cache -> {
        startFuture.complete(null);
        addContinuousQuery(cache);
      }, startFuture::fail);
  }

  private void addContinuousQuery(RemoteCache<String, Stop> stopsCache) {
    QueryFactory qf = Search.getQueryFactory(stopsCache);

    org.infinispan.query.dsl.Query query = qf.from(Stop.class)
      .having("delayMin").gt(0L)
      .build();

    ContinuousQueryListener<String, Stop> listener =
      new ContinuousQueryListener<String, Stop>() {
        @Override
        public void resultJoining(String id, Stop stop) {
          vertx.runOnContext(x -> {
            vertx.eventBus().publish("delayed-trains", toJson(stop));
          });
        }

        @Override
        public void resultUpdated(String id, Stop stop) {
        }

        @Override
        public void resultLeaving(String id) {
        }
      };

    continuousQuery = Search.getContinuousQuery(stopsCache);
    continuousQuery.addContinuousQueryListener(query, listener);
  }

  private Object addModelToServer() {
    InputStream is = getClass().getResourceAsStream("/app-model.proto");
    RemoteCache<String, String> metaCache = client.getCache(PROTOBUF_METADATA_CACHE_NAME);
    metaCache.put("app-model.proto", readInputStream(is));

    String errors = metaCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
    if (errors != null)
      throw new AssertionError("Error in proto file");

    return null;
  }

  @Override
  public void stop() throws Exception {
    if (continuousQuery != null)
      continuousQuery.removeAllListeners();

    if (client != null)
      client.stop();
  }

  private static String toJson(Stop stop) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", stop.train.getCategory());
    map.put("departure", String.format("%tR", stop.departureTs));
    map.put("station", stop.station.getName());
    map.put("destination", stop.train.getTo());
    map.put("delay", stop.delayMin);
    map.put("trainName", stop.train.getName());
    return new JsonObject(map).encode();
  }

  private static String readInputStream(InputStream is) {
    try {
      try {
        final Reader reader = new InputStreamReader(is, "UTF-8");
        StringWriter writer = new StringWriter();
        char[] buf = new char[1024];
        int len;
        while ((len = reader.read(buf)) != -1) {
          writer.write(buf, 0, len);
        }
        return writer.toString();
      } finally {
        is.close();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
