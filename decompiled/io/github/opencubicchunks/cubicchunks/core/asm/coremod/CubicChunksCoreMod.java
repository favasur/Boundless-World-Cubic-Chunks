package io.github.opencubicchunks.cubicchunks.core.asm.coremod;

import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.SortingIndex;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IEnvironmentTokenProvider;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@MCVersion("1.12.2")
@SortingIndex(5000)
public class CubicChunksCoreMod implements IFMLLoadingPlugin {
   public CubicChunksCoreMod() {
      initMixin();
   }

   public String[] getASMTransformerClass() {
      return new String[]{"io.github.opencubicchunks.cubicchunks.core.asm.transformer.CubicChunksWorldEditTransformer"};
   }

   @Nullable
   public String getModContainerClass() {
      return "io.github.opencubicchunks.cubicchunks.core.asm.CubicChunksCoreContainer";
   }

   @Nullable
   public String getSetupClass() {
      return null;
   }

   public void injectData(Map<String, Object> data) {
   }

   @Nullable
   public String getAccessTransformerClass() {
      return null;
   }

   public static void initMixin() {
      MixinBootstrap.init();
      Mixins.addConfiguration("cubicchunks.mixins.core.json");
      Mixins.addConfiguration("cubicchunks.mixins.fixes.json");
      Mixins.addConfiguration("cubicchunks.mixins.selectable.json");
      Mixins.addConfiguration("cubicchunks.mixins.noncritical.json");
      MixinEnvironment.getDefaultEnvironment()
         .registerTokenProviderClass("io.github.opencubicchunks.cubicchunks.core.asm.coremod.CubicChunksCoreMod$TokenProvider");
   }

   public static final class TokenProvider implements IEnvironmentTokenProvider {
      public TokenProvider() {
      }

      @Override
      public int getPriority() {
         return 1000;
      }

      @Override
      public Integer getToken(String token, MixinEnvironment env) {
         if ("FORGE".equals(token)) {
            return ForgeVersion.getBuildVersion();
         } else if ("FML".equals(token)) {
            String fmlVersion = Loader.instance().getFMLVersionString();
            int build = Integer.parseInt(fmlVersion.substring(fmlVersion.lastIndexOf(46) + 1));
            return build;
         } else {
            return "MC_FORGE".equals(token) ? 23 : null;
         }
      }
   }
}
