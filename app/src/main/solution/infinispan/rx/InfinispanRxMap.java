package infinispan.rx;

import infinispan.rx.impl.RxMapImpl;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.Configuration;

public interface InfinispanRxMap<K, V> {

  Completable put(K key, V value);

  Maybe<V> get(K key);

  Completable close();

  static <K, V> Single<InfinispanRxMap<K, V>> create(
      String cacheName
      , Configuration cfg
      , Vertx vertx
  ) {
    return Util.getRemoteCache(cacheName, cfg, vertx)
      .map(cache -> new RxMapImpl<>((RemoteCache<K, V>) cache, vertx));
  }

}
