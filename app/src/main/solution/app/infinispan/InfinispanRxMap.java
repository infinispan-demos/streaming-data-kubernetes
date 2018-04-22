package app.infinispan;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

public interface InfinispanRxMap<K, V> {

  Completable put(K key, V value);

  Maybe<V> get(K key);

  Single<Integer> size();

  Completable clear();

  Completable close();

  static <K, V> Single<InfinispanRxMap<K, V>> create(
    String cacheName
    , ConfigurationBuilder cfg
    , Vertx vertx
  ) {
    return Utils.getRemoteCache(cacheName, cfg, vertx)
      .map(
        cache ->
          new RxMapImpl<>((RemoteCache<K, V>) cache, vertx)
      );
  }

  static <K, V> Single<InfinispanRxMap<K, V>> createIndexed(
    String cacheName
    , Class<?>[] indexed
    , ConfigurationBuilder cfg
    , Vertx vertx
  ) {
    return Utils.getIndexedRemoteCache(cacheName, indexed, cfg, vertx)
      .map(
        cache ->
          new RxMapImpl<>((RemoteCache<K, V>) cache, vertx)
      );
  }

}
