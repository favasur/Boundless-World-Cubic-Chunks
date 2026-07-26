package cubicchunks.regionlib.api.region.key;

public interface IKey<K extends IKey<K>> {
   int getId();

   RegionKey getRegionKey();
}
