package streaming.data.model;

public class GeoLocation {

   public final double lat;
   public final double lng;
   public final Double bearing; // nullable

   public GeoLocation(double lat, double lng, Double bearing) {
      this.lat = lat;
      this.lng = lng;
      this.bearing = bearing;
   }

   @Override
   public String toString() {
      return "GeoLocBearing{" +
         "lat=" + lat +
         ", lng=" + lng +
         ", bearing=" + bearing +
         '}';
   }

}
