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
@ProtoMessage(name = "Train")
public class Train {

  private byte[] name;
  private byte[] to;
  private byte[] category;
  private byte[] operator; // nullable

  public Train() {
  }

  public Train(String name, String to, String category, String operator) {
    this.name = bs(name);
    this.to = bs(to);
    this.category = bs(category);
    this.operator = bs(operator);
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 10, required = false) // TODO: required should be true @adrian
  public byte[] getName() {
    return name;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 20, required = false) // TODO: required should be true @adrian
  public byte[] getTo() {
    return to;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 30, required = false) // TODO: required should be true @adrian
  public byte[] getCategory() {
    return category;
  }

  @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
  @ProtoField(number = 40, required = false)
  public byte[] getOperator() {
    return operator;
  }

  public void setName(byte[] name) {
    this.name = name;
  }

  public void setTo(byte[] to) {
    this.to = to;
  }

  public void setCategory(byte[] cat) {
    this.category = cat;
  }

  public void setOperator(byte[] operator) {
    this.operator = operator;
  }

  @Override
  public String toString() {
    return "Train{" +
      "name='" + TO_UTF8.apply(name) + '\'' +
      ", to='" + TO_UTF8.apply(to) + '\'' +
      ", category='" + TO_UTF8.apply(category) + '\'' +
      ", operator='" + TO_UTF8.apply(operator) + '\'' +
      '}';
  }

}
