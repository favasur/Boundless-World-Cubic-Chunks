package cubicchunks.regionlib.impl.save;

import cubicchunks.regionlib.api.region.IRegionProvider;
import cubicchunks.regionlib.api.storage.SaveSection;
import cubicchunks.regionlib.impl.MinecraftChunkLocation;
import cubicchunks.regionlib.impl.header.TimestampHeaderEntryProvider;
import cubicchunks.regionlib.lib.Region;
import cubicchunks.regionlib.lib.provider.SharedCachedRegionProvider;
import cubicchunks.regionlib.lib.provider.SimpleRegionProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class MinecraftSaveSection extends SaveSection<MinecraftSaveSection, MinecraftChunkLocation> {
   public MinecraftSaveSection(IRegionProvider<MinecraftChunkLocation> regionProvider) {
      super(regionProvider);
   }

   public static MinecraftSaveSection createAt(Path directory, MinecraftSaveSection.MinecraftRegionType type) {
      return new MinecraftSaveSection(
         new SharedCachedRegionProvider<>(
            new SimpleRegionProvider<>(
               new MinecraftChunkLocation.Provider(type.name().toLowerCase()),
               directory,
               (keyProvider, regionKey) -> Region.<MinecraftChunkLocation>builder()
                     .setDirectory(directory)
                     .setSectorSize(4096)
                     .setKeyProvider(keyProvider)
                     .setRegionKey(regionKey)
                     .addHeaderEntry(new TimestampHeaderEntryProvider<>(TimeUnit.MILLISECONDS))
                     .build(),
               (dir, key) -> Files.exists(dir.resolve(key.getRegionKey().getName()))
            )
         )
      );
   }

   public static enum MinecraftRegionType {
      MCR,
      MCA;

      private MinecraftRegionType() {
      }
   }
}
