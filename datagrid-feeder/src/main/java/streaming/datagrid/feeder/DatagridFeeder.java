package streaming.datagrid.feeder;

import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;

import static io.vertx.core.http.HttpMethod.GET;

public class DatagridFeeder extends AbstractVerticle {

   @Override
   public void start() throws Exception {
      vertx.setPeriodic(3000L, l -> {
         requestSnapshot();
      });
   }

   private void requestSnapshot() {
      HttpClient client = vertx.createHttpClient();
      HttpClientRequest req = client.request(GET, 9000, "localhost", "/position");
      req.toObservable()
         .flatMap(resp -> {
            if (resp.statusCode() != 200) {
               throw new RuntimeException("Wrong status code " + resp.statusCode());
            }
            return resp.toObservable();
         })
         .subscribe(data ->
            System.out.println("Server content " + data.toString("UTF-8")));

      // End request
      req.end();
   }

   public static void main(String[] args) {
      Vertx vertx = Vertx.vertx();
      vertx.deployVerticle(DatagridFeeder.class.getName());
   }

}
