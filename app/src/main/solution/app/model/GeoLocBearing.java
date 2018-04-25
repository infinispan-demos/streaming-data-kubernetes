package app.model;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;

@ProtoDoc("@Indexed")
@ProtoMessage(name = "GeoLocBearing")
public class GeoLocBearing {

  private double lat;
  private double lng;
  private Double bearing; // nullable

  // Required for proto schema builder
  public GeoLocBearing() {
  }

  public GeoLocBearing(double lat, double lng, Double bearing) {
    this.lat = lat;
    this.lng = lng;
    this.bearing = bearing;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 10, required = true)
  public double getLat() {
    return lat;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 20, required = true)
  public double getLng() {
    return lng;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 30, required = false)
  public Double getBearing() {
    return bearing;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public void setLng(double lng) {
    this.lng = lng;
  }

  public void setBearing(Double bearing) {
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
