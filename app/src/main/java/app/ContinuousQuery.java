package app;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

public class ContinuousQuery extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    startFuture.complete();
  }

}
