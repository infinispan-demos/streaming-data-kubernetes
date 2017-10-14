package streaming.data.model;

import static streaming.data.model.ModelUtils.bs;
import static streaming.data.model.ModelUtils.str;

public class Train {

   final byte[] id;
   final byte[] category;
   final byte[] name;
   final byte[] lastStopName;

   public static Train make(String id, String category, String name, String lastStopName) {
      return new Train(bs(id), bs(category), bs(name), bs(lastStopName));
   }

   private Train(byte[] id, byte[] category, byte[] name, byte[] lastStopName) {
      this.id = id;
      this.category = category;
      this.name = name;
      this.lastStopName = lastStopName;
   }

   public String getId() {
      return str(id);
   }

   public String getName() {
      return str(name);
   }

   public String getCategory() {
      return str(category);
   }

   public String getLastStopName() {
      return str(lastStopName);
   }

   @Override
   public String toString() {
      return "Train{" +
         "id=" + getId() +
         ", category=" + getCategory() +
         ", name=" + getName() +
         ", lastStopName=" + getLastStopName() +
         '}';
   }

}
