package cubicchunks.regionlib.impl.save;

import cubicchunks.regionlib.api.region.IRegionProvider;
import cubicchunks.regionlib.api.storage.SaveSection;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.lib.ExtRegion;
import cubicchunks.regionlib.lib.provider.SharedCachedRegionProvider;
import cubicchunks.regionlib.lib.provider.SimpleRegionProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

public class SaveSection3D extends SaveSection<SaveSection3D, EntryLocation3D> {
   public SaveSection3D(IRegionProvider<EntryLocation3D>... regionProvider) {
      super(Arrays.asList(regionProvider));
   }

   public static SaveSection3D createAt(Path directory) {
      return new SaveSection3D(
         new SharedCachedRegionProvider<>(SimpleRegionProvider.createDefault(new EntryLocation3D.Provider(), directory, 512)),
         new SharedCachedRegionProvider<>(
            new SimpleRegionProvider<>(
               new EntryLocation3D.Provider(),
               directory,
               (keyProvider, regionKey) -> new ExtRegion<>(directory, Collections.emptyList(), keyProvider, regionKey),
               (dir, key) -> Files.exists(dir.resolve(key.getRegionKey().getName() + ".ext"))
            )
         )
      );
   }
}
