package app.model;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;

import static app.model.ModelUtils.FROM_UTF8;
import static app.model.ModelUtils.TO_UTF8;

@ProtoDoc("@Indexed")
@ProtoMessage(name = "TrainPosition") // TODO: Optional
public class TrainPosition {

  private String trainId;
  private String trainName;
  private int delay;
  private byte[] category;
  private byte[] lastStopName;
  private TimedPosition current;
  // public final List<TimedPosition> futurePositions;

  // Required for proto schema builder
  public TrainPosition() {
  }

  public TrainPosition(
      String trainId
      , String trainName
      , int delay
      , String category
      , String lastStopName
      , TimedPosition current
  ) {
    this.trainId = trainId;
    this.trainName = trainName;
    this.delay = delay;
    this.category = FROM_UTF8.apply(category);
    this.lastStopName = FROM_UTF8.apply(lastStopName);
    this.current = current;
  }

  @ProtoDoc("@IndexedField")
  @ProtoField(number = 10, required = true)
  public String getTrainId() {
    return trainId;
  }

  @ProtoDoc("@IndexedField")
  @ProtoField(number = 20, required = true)
  public String getTrainName() {
    return trainName;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 30, required = true)
  public int getDelay() {
    return delay;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 40, required = false) // TODO: required should be true @adrian
  public byte[] getCategory() {
    return category;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 50, required = false) // TODO: required should be true @adrian
  public byte[] getLastStopName() {
    return lastStopName;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 60, required = true)
  public TimedPosition getCurrent() {
    return current;
  }

  public void setTrainId(String trainId) {
    this.trainId = trainId;
  }

  public void setTrainName(String trainName) {
    this.trainName = trainName;
  }

  public void setDelay(int delay) {
    this.delay = delay;
  }

  public void setCategory(byte[] category) {
    this.category = category;
  }

  public void setLastStopName(byte[] lastStopName) {
    this.lastStopName = lastStopName;
  }

  public void setCurrent(TimedPosition current) {
    this.current = current;
  }

  @Override
  public String toString() {
    return "TrainPosition{" +
      "trainId=" + trainId +
      ", trainName=" + trainName +
      ", delay=" + delay +
      ", category=" + TO_UTF8.apply(category) +
      ", lastStopName=" + TO_UTF8.apply(lastStopName) +
      ", current=" + current +
      '}';
  }

}
