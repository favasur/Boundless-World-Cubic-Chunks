package cubicchunks.regionlib.api.region.key;

public interface IKeyProvider<K extends IKey<K>> {
   K fromRegionAndId(RegionKey var1, int var2) throws IllegalArgumentException;

   int getKeyCount(RegionKey var1);

   boolean isValid(RegionKey var1);
}
