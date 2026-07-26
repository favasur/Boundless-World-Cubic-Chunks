package cubicchunks.regionlib.impl.save;

import cubicchunks.regionlib.api.region.IRegionProvider;
import cubicchunks.regionlib.api.storage.SaveSection;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.lib.ExtRegion;
import cubicchunks.regionlib.lib.provider.SharedCachedRegionProvider;
import cubicchunks.regionlib.lib.provider.SimpleRegionProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

public class SaveSection2D extends SaveSection<SaveSection2D, EntryLocation2D> {
   public SaveSection2D(IRegionProvider<EntryLocation2D> regionProvider) {
      super(regionProvider);
   }

   public SaveSection2D(IRegionProvider<EntryLocation2D>... regionProvider) {
      super(Arrays.asList(regionProvider));
   }

   public static SaveSection2D createAt(Path directory) {
      return new SaveSection2D(
         new SharedCachedRegionProvider<>(SimpleRegionProvider.createDefault(new EntryLocation2D.Provider(), directory, 512)),
         new SharedCachedRegionProvider<>(
            new SimpleRegionProvider<>(
               new EntryLocation2D.Provider(),
               directory,
               (keyProvider, regionKey) -> new ExtRegion<>(directory, Collections.emptyList(), keyProvider, regionKey),
               (dir, key) -> Files.exists(dir.resolve(key.getRegionKey().getName() + ".ext"))
            )
         )
      );
   }
}
