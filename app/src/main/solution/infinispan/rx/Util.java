package infinispan.rx;

import app.Injector;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.core.Vertx;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;

import java.io.IOException;
import java.util.logging.Logger;

import static org.infinispan.query.remote.client.ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX;
import static org.infinispan.query.remote.client.ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME;

final class Util {

  static final Logger log = Logger.getLogger(Util.class.getName());

  private Util() {
  }

  static <K, V> Single<RemoteCache<K, V>> getRemoteCache(
      String cacheName
      , ConfigurationBuilder cfg
      , Vertx vertx
  ) {
    return vertx
      .rxExecuteBlocking(remoteCacheManager(cfg))
      .flatMap(
        remote -> vertx.rxExecuteBlocking(remoteCache(cacheName, remote))
      );
  }

  static <K, V> Single<RemoteCache<K, V>> getIndexedRemoteCache(
    String cacheName
    , Class<?>[] indexed
    , ConfigurationBuilder cfg
    , Vertx vertx
  ) {
    return vertx
      .rxExecuteBlocking(indexedRemoteCacheManager(cfg))
      .flatMap(
        remote -> vertx.rxExecuteBlocking(addIndexed(indexed, remote))
      )
      .flatMap(
        remote -> vertx.rxExecuteBlocking(remoteCache(cacheName, remote))
      );
  }

  private static Handler<Future<RemoteCacheManager>> addIndexed(
      Class<?>[] indexed
      , RemoteCacheManager remote
  ) {
    return f -> {
      SerializationContext serialCtx =
        ProtoStreamMarshaller.getSerializationContext(remote);

      RemoteCache<String, String> metadataCache =
        remote.getCache(PROTOBUF_METADATA_CACHE_NAME);

      ProtoSchemaBuilder schemaBuilder = new ProtoSchemaBuilder();

      try {
        for (Class<?> clazz : indexed) {
          String fileName = clazz.getSimpleName().toLowerCase() + ".proto";
          String file =
            buildProtoFile(fileName, clazz, schemaBuilder, serialCtx);

          log.info("Add indexed file " + fileName);
          metadataCache.put(fileName, file);

          String errors = metadataCache.get(ERRORS_KEY_SUFFIX);
          if (errors != null)
            f.fail(new AssertionError("Error in proto file"));
          else
            log.info("Added indexed file " + fileName);
        }
        f.complete(remote);
      } catch (IOException e) {
        f.fail(e);
      }

    };
  }

  private static String buildProtoFile(
      String fileName
      , Class<?> clazz
      , ProtoSchemaBuilder schemaBuilder
      , SerializationContext serialCtx
  ) throws IOException {
    return schemaBuilder
      .fileName(fileName)
      .addClass(clazz)
      .build(serialCtx);
  }

  private static Handler<Future<RemoteCacheManager>> remoteCacheManager(
      ConfigurationBuilder cfg
  ) {
    return f -> f.complete(new RemoteCacheManager(cfg.build()));
  }

  private static Handler<Future<RemoteCacheManager>> indexedRemoteCacheManager(
      ConfigurationBuilder cfg
  ) {
    return f -> {
      cfg.marshaller(ProtoStreamMarshaller.class);
      f.complete(new RemoteCacheManager(cfg.build()));
    };
  }

  private static <K, V> Handler<Future<RemoteCache<K, V>>> remoteCache(
      String cacheName
      , RemoteCacheManager remote
  ) {
    return f -> f.complete(remote.getCache(cacheName));
  }

}
