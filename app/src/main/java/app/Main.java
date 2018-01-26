package app;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Single;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.util.logging.Logger;

import static io.reactivex.Single.just;

public class Main extends AbstractVerticle {

  static final Logger log = Logger.getLogger(Main.class.getName());

  @Override
  public void start(io.vertx.core.Future<Void> future) {
    // TODO - live code
  }

  // TODO - live code

}
