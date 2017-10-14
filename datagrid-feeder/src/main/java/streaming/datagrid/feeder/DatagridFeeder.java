package streaming.datagrid.feeder;

import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import rx.Observable;
import streaming.data.model.GeoLocation;
import streaming.data.model.Train;
import streaming.data.model.TrainPosition;

import java.util.logging.Level;
import java.util.logging.Logger;

import static io.vertx.core.http.HttpMethod.GET;

public class DatagridFeeder extends AbstractVerticle {

   private static final Logger log = Logger.getLogger(DatagridFeeder.class.getName());

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
            return Observable.just(Buffer.buffer()).mergeWith(resp.toObservable());
         })
         .reduce(Buffer::appendBuffer) // Reduce all buffers in a single buffer
         .map(buffer -> buffer.toString("UTF-8")) // Turn in to a string
         .subscribe(data -> {
            String[] lines = data.split("\\n");
            //log.info("Received snapshot: " + data);
            for (String line : lines) {
               if (!line.startsWith("train_id")) {
                  String[] s = line.split("\\t");
                  //log.info("Line: " + line);
                  //log.info("Length: " + s.length);
                  Train train = extractTrain(s);
                  GeoLocation pos = extractPosition(s);
                  int delay = Integer.valueOf(s[7]);
                  TrainPosition trainPosition = new TrainPosition(train, pos, delay);
                  log.info("Train position: "  + trainPosition);
               }
            }
         }, err -> {
            log.log(Level.SEVERE, "Error", err);
         });

      // End request
      req.end();
   }

   private GeoLocation extractPosition(String[] s) {
      return new GeoLocation(Double.valueOf(s[4]), Double.valueOf(s[5]),
         s[6].isEmpty() ? 0 : Double.valueOf(s[6]));
   }

   private Train extractTrain(String[] s) {
      return Train.make(s[0], s[1], s[2], s[3]);
   }

   public static void main(String[] args) {
      Vertx vertx = Vertx.vertx();
      vertx.deployVerticle(DatagridFeeder.class.getName());
   }

}
