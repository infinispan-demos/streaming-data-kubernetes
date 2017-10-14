package streaming.data.model;

public class TrainPosition {

   final Train train;
   final GeoLocation position;
   final int delay;

   public TrainPosition(Train train, GeoLocation position, int delay) {
      this.train = train;
      this.position = position;
      this.delay = delay;
   }

   @Override
   public String toString() {
      return "TrainPosition{" +
         "train=" + train +
         ", position=" + position +
         ", delay=" + delay +
         '}';
   }

}
