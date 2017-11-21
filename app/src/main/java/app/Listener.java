package app;

import app.model.Stop;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.query.api.continuous.ContinuousQuery;

import java.util.HashMap;
import java.util.Map;

import static app.AppUtils.createRemoteCacheManager;

public class Listener extends AbstractVerticle {

  RemoteCacheManager client;
  ContinuousQuery<String, Stop> continuousQuery;

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    vertx.<RemoteCacheManager>rxExecuteBlocking(fut -> fut.complete(createRemoteCacheManager()))
      .doOnSuccess(rcm -> client = rcm)
      .flatMap(v -> vertx.<RemoteCache<String, Stop>>rxExecuteBlocking(fut -> fut.complete(client.getCache())))
      .subscribe(cache -> {
        startFuture.complete(null);
        addContinuousQuery(cache);
      }, startFuture::fail);
  }

  private void addContinuousQuery(RemoteCache<String, Stop> stopsCache) {
    // TODO: live code
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

}