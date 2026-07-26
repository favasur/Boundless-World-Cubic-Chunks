package cubicchunks.regionlib.api.region.key;

public class RegionKey {
   private String name;

   public RegionKey(String name) {
      if (name == null) {
         throw new NullPointerException("name");
      } else {
         this.name = name;
      }
   }

   public String getName() {
      return this.name;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         RegionKey regionKey = (RegionKey)o;
         return this.name.equals(regionKey.name);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.name.hashCode();
   }
}
