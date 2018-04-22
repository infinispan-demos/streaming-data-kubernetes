package app.infinispan;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.reactivex.core.Context;
import io.vertx.reactivex.core.Vertx;
import org.infinispan.client.hotrod.RemoteCache;

final class RxMapImpl<K, V> implements InfinispanRxMap<K, V> {

  private final RemoteCache<K, V> cache;
  private final Vertx vertx;

  public RxMapImpl(RemoteCache<K, V> cache, Vertx vertx) {
    this.cache = cache;
    this.vertx = vertx;
  }

  @Override
  public Completable put(K key, V value) {
    return getContext()
      .rxExecuteBlocking(
        f -> {
          cache.put(key, value);
          f.complete();
        }
      )
      .toCompletable();
  }

  @Override
  public Maybe<V> get(K key) {
    return getContext()
      .<V>rxExecuteBlocking(
        f -> {
          V v = cache.get(key);
          f.complete(v);
        }
      )
      .flatMapMaybe(value ->
        value != null
          ? Maybe.just(value)
          : Maybe.empty())
      ;
  }

  @Override
  public Single<Integer> size() {
    return getContext()
      .rxExecuteBlocking(
        fut -> fut.complete(cache.size())
      );
  }

  @Override
  public Completable clear() {
    return getContext()
      .rxExecuteBlocking(
        fut -> {
          cache.clear();
          fut.complete();
        }
      )
      //.doOnError(err -> LOGGER.error("Error on remove", err))
      .toCompletable();
  }

  @Override
  public Completable close() {
    return getContext()
      .rxExecuteBlocking(
        f -> {
          cache.getRemoteCacheManager().stop();
          f.complete();
        }
      )
      .toCompletable();
  }

  private Context getContext() {
    return vertx.getOrCreateContext();
  }

}
