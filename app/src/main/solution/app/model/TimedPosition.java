package app.model;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;

@ProtoDoc("@Indexed")
@ProtoMessage(name = "TimedPosition") // TODO: Optional
public class TimedPosition {

  private long timestamp;
  private GeoLocBearing position;

  // Required for proto schema builder
  public TimedPosition() {
  }

  public TimedPosition(long timestamp, GeoLocBearing position) {
    this.timestamp = timestamp;
    this.position = position;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 10, required = true)
  public long getTimestamp() {
    return timestamp;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 20, required = true)
  public GeoLocBearing getPosition() {
    return position;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public void setPosition(GeoLocBearing position) {
    this.position = position;
  }

  @Override
  public String toString() {
    return "TimedPosition{" +
      "timestamp=" + timestamp +
      ", position=" + position +
      '}';
  }

}
