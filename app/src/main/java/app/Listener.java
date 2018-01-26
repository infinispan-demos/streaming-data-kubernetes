package app;

import app.model.Stop;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.api.continuous.ContinuousQuery;
import org.infinispan.query.api.continuous.ContinuousQueryListener;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import java.util.HashMap;
import java.util.Map;

public class Listener extends AbstractVerticle {

  RemoteCache<String, Stop> stopsCache;
  ContinuousQuery<String, Stop> continuousQuery;

  @Override
  public void start(Future<Void> startFuture) {
    vertx
      .rxExecuteBlocking(AppUtils::remoteCacheManager)
      .flatMap(remote -> vertx.rxExecuteBlocking(AppUtils.remoteCache(remote)))
      .subscribe(
        cache -> {
          startFuture.complete();
          addContinuousQuery(cache);
        }
        , startFuture::fail
      );
  }

  private void addContinuousQuery(RemoteCache<String, Stop> cache) {
    stopsCache = cache;

    // TODO - live code
  }

  @Override
  public void stop() throws Exception {
    if (continuousQuery != null)
      continuousQuery.removeAllListeners();

    if (stopsCache != null)
      stopsCache.getRemoteCacheManager().stop();
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