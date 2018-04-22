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

import static app.model.ModelUtils.TO_UTF8;
import static app.model.ModelUtils.bs;

@ProtoDoc("@Indexed")
@ProtoMessage(name = "Station")
public class Station {

  private long id;
  private byte[] name;

  // Required for proto schema builder
  public Station() {
  }

  public Station(long id, String name) {
    this.id = id;
    this.name = bs(name);
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 10, required = true)
  public long getId() {
    return id;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 20, required = false) // TODO: required should be true @adrian
  public byte[] getName() {
    return name;
  }

  public void setId(long id) {
    this.id = id;
  }

  public void setName(byte[] name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "Station{" +
      "id=" + id +
      ", name='" + TO_UTF8.apply(name) + '\'' +
      '}';
  }

}
