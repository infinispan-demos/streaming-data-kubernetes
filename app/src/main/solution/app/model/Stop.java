/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package app.model;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;

import java.util.Date;

@ProtoDoc("@Indexed")
@ProtoMessage(name = "Stop")
public class Stop {

  public Train train;
  public int delayMin;
  public Station station;
  public Date departureTs;

  // Required for proto schema builder
  public Stop() {
  }

  public Stop(Train train, int delayMin, Station station, Date departureTs) {
    this.train = train;
    this.delayMin = delayMin;
    this.station = station;
    this.departureTs = departureTs;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 10, required = true)
  public Train getTrain() {
    return train;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 20, required = true)
  public int getDelayMin() {
    return delayMin;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 30, required = true)
  public Station getStation() {
    return station;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 40, required = true)
  public Date getDepartureTs() {
    return departureTs;
  }

  public void setTrain(Train train) {
    this.train = train;
  }

  public void setDelayMin(int delayMin) {
    this.delayMin = delayMin;
  }

  public void setStation(Station station) {
    this.station = station;
  }

  public void setDepartureTs(Date departureTs) {
    this.departureTs = departureTs;
  }

  @Override
  public String toString() {
    return "Stop{" +
      "train=" + train +
      ", delayMin=" + delayMin +
      ", station=" + station +
      ", departureTs=" + departureTs +
      '}';
  }

}
