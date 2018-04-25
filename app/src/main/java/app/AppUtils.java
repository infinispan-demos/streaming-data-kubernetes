package app;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.Handler;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.logging.Level.SEVERE;

public class AppUtils {

  static final Logger log = Logger.getLogger(AppUtils.class.getName());

  static Handler<RoutingContext> sockJSHandlerAndInject(String address, Vertx vertx) {
    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    BridgeOptions options = new BridgeOptions();
    options.addOutboundPermitted(new PermittedOptions().setAddress(address));

    sockJSHandler.bridge(options, be -> {
      if (be.type() == BridgeEventType.REGISTER) {
        log.info("SockJs client connected, start injectors");
        invokeInjectAndComplete("/inject", vertx, be);
      } else if (be.type() == BridgeEventType.SOCKET_CLOSED) {
        invokeInjectAndComplete("/inject/stop", vertx, be);
      } else {
        be.complete(true);
      }
    });

    return sockJSHandler;
  }

  static Handler<RoutingContext> sockJSHandler(String address, Vertx vertx) {
    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    BridgeOptions options = new BridgeOptions();
    options.addOutboundPermitted(new PermittedOptions().setAddress(address));
    sockJSHandler.bridge(options);
    return sockJSHandler;
  }

  private static void invokeInjectAndComplete(
    String uri
    , Vertx vertx
    , BridgeEvent be
  ) {
    httpGet("localhost", uri, vertx)
      .subscribe(
        reply -> {
          log.info("HTTP GET replied: " + reply.body());
          be.complete(true);
        }
        , be::fail
      );
  }

  private static Single<HttpResponse<String>> httpGet(
    String host
    , String uri
    , Vertx vertx
  ) {
    log.info("Call HTTP GET " + host + uri);
    WebClient client = WebClient.create(vertx);
    return client
      .get(8080, host, uri)
      .as(BodyCodec.string())
      .rxSend();
  }

  static Flowable<String> rxReadFile(String resource) {
    Objects.requireNonNull(resource);
    URL url = getUrl(resource);
    Objects.requireNonNull(url);

    return Flowable
      .<String, BufferedReader>generate(
        () -> {
          InputStream inputStream = url.openStream();
          InputStream gzipStream = new GZIPInputStream(inputStream);
          Reader decoder = new InputStreamReader(gzipStream, UTF_8);
          return new BufferedReader(decoder);
        }
        , (bufferedReader, emitter) -> {
          String line = bufferedReader.readLine();
          if (line != null) {
            emitter.onNext(line);
          } else {
            emitter.onComplete();
          }
        }
        , BufferedReader::close
      )
      .zipWith(throttle(), (item, interval) -> item) // throttle
      .subscribeOn(Schedulers.io());
  }

  private static URL getUrl(String resource) {
    try {
      return new File(resource).toURI().toURL();
    } catch (MalformedURLException e) {
      log.log(SEVERE, "Error resolving file URL", e);
      return null;
    }
  }

  private static Flowable<Long> throttle() {
    return Flowable.interval(5, TimeUnit.MILLISECONDS).onBackpressureDrop();
  }

}
