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

import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;

import static app.model.ModelUtils.TO_UTF8;

public class Listener extends AbstractVerticle {

  static final Logger log = Logger.getLogger(Listener.class.getName());

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

    QueryFactory queryFactory = Search.getQueryFactory(cache);

    Query query = queryFactory
      .from(Stop.class)
      .having("delayMin").gt(0)
      .build();

    ContinuousQueryListener<String, Stop> listener = new ContinuousQueryListener<String, Stop>() {
      @Override
      public void resultJoining(String key, Stop value) {
        vertx.eventBus().publish("delayed-trains", toJson(value));
      }
    };

    ContinuousQuery<String, Stop> continuousQuery = Search.getContinuousQuery(cache);
    continuousQuery.removeAllListeners();
    continuousQuery.addContinuousQueryListener(query, listener);
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
    map.put("type", TO_UTF8.apply(stop.train.getCategory()));
    map.put("departure", String.format("%tR", stop.departureTs));
    map.put("station", TO_UTF8.apply(stop.station.getName()));
    map.put("destination", TO_UTF8.apply(stop.train.getTo()));
    map.put("delay", stop.delayMin);
    map.put("trainName", TO_UTF8.apply(stop.train.getName()));
    return new JsonObject(map).encode();
  }

}