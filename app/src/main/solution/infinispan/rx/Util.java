package infinispan.rx;

import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.core.Vertx;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;

public final class Util {

  private Util() {
  }

  static <K, V> Single<RemoteCache<K, V>> getRemoteCache(
      String cacheName
      , Configuration cfg
      , Vertx vertx
  ) {
    return vertx
      .rxExecuteBlocking(remoteCacheManager(cfg))
      .flatMap(
        remote -> vertx.rxExecuteBlocking(remoteCache(cacheName, remote))
      );
  }

  private static Handler<Future<RemoteCacheManager>> remoteCacheManager(
      Configuration cfg
  ) {
    return f -> f.complete(new RemoteCacheManager(cfg));
  }

  private static <K, V> Handler<Future<RemoteCache<K, V>>> remoteCache(
      String cacheName
      , RemoteCacheManager remote
  ) {
    return f -> f.complete(remote.getCache(cacheName));
  }

}
