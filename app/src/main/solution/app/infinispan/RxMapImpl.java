package app.infinispan;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.reactivex.core.Context;
import io.vertx.reactivex.core.Vertx;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.api.continuous.ContinuousQuery;
import org.infinispan.query.api.continuous.ContinuousQueryListener;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

final class RxMapImpl<K, V> implements InfinispanRxMap<K, V> {

  private final RemoteCache<K, V> cache;
  private final Vertx vertx;

  private ContinuousQuery<K, V> continuousQuery;

  RxMapImpl(RemoteCache<K, V> cache, Vertx vertx) {
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
          : Maybe.empty()
      );
  }

  @Override
  public Single<Integer> size() {
    return getContext()
      .rxExecuteBlocking(
        fut -> fut.complete(cache.size())
      );
  }

  @Override
  public Flowable<Map.Entry<K, V>> continuousQuery(String queryString) {
    return Flowable
      .<Map.Entry<K, V>>create(
        emitter -> {
          QueryFactory queryFactory = Search.getQueryFactory(cache);
          Query query = queryFactory.create(queryString);

          ContinuousQueryListener listener = new ContinuousQueryListener() {
            @Override
            public void resultJoining(Object key, Object value) {
              emitter.onNext(
                new SimpleImmutableEntry<K, V>((K) key, (V) value)
              );
            }
          };

          if (continuousQuery == null)
            continuousQuery = Search.getContinuousQuery(cache);

          continuousQuery.addContinuousQueryListener(query, listener);
        }, BackpressureStrategy.BUFFER)
      .subscribeOn(Schedulers.io());
  }

  @Override
  public Completable removeContinuousQueries() {
    if (continuousQuery != null) {
      return getContext()
        .rxExecuteBlocking(
          fut -> {
            continuousQuery.removeAllListeners();
            fut.complete();
          }
        )
        .toCompletable();
    }

    return Completable.complete();
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
