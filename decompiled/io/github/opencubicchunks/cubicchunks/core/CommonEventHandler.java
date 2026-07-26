package io.github.opencubicchunks.cubicchunks.core;

import com.google.common.collect.ImmutableList;
import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldSettings;
import io.github.opencubicchunks.cubicchunks.core.network.PacketCubicWorldData;
import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.core.server.VanillaNetworkHandler;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.util.ReflectionUtil;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.WorldSavedCubicChunksData;
import io.github.opencubicchunks.cubicchunks.core.world.provider.ICubicWorldProvider;
import java.io.IOException;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent.Unload;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommonEventHandler {
   private final List<Class<?>> allowedServerWorldClasses = ImmutableList.copyOf(
      new Class[]{
         WorldServer.class,
         WorldServerMulti.class,
         ReflectionUtil.getClassOrDefault("WorldServerOF", Object.class),
         ReflectionUtil.getClassOrDefault("WorldServerMultiOF", Object.class),
         ReflectionUtil.getClassOrDefault("net.optifine.override.WorldServerOF", Object.class),
         ReflectionUtil.getClassOrDefault("net.optifine.override.WorldServerMultiOF", Object.class),
         ReflectionUtil.getClassOrDefault("com.forgeessentials.multiworld.WorldServerMultiworld", Object.class)
      }
   );
   private final List<Class<? extends IChunkProvider>> allowedServerChunkProviderClasses = ImmutableList.copyOf(new Class[]{ChunkProviderServer.class});

   public CommonEventHandler() {
   }

   @SubscribeEvent
   public void onWorldAttachCapabilities(AttachCapabilitiesEvent<World> evt) {
      if (!((World)evt.getObject()).field_72995_K && evt.getObject() instanceof WorldServer) {
         WorldServer world = (WorldServer)evt.getObject();
         WorldSavedCubicChunksData savedData = (WorldSavedCubicChunksData)((World)evt.getObject())
            .getPerWorldStorage()
            .func_75742_a(WorldSavedCubicChunksData.class, "cubicChunksData");
         boolean ccWorldType = ((World)evt.getObject()).func_175624_G() instanceof ICubicWorldType;
         boolean ccGenerator = ccWorldType && ((ICubicWorldType)((World)evt.getObject()).func_175624_G()).hasCubicGeneratorForWorld((World)evt.getObject());
         boolean savedCC = savedData != null && savedData.isCubicChunks;
         boolean ccWorldInfo = ((ICubicWorldSettings)world.func_72912_H()).isCubic() && (savedData == null || savedData.isCubicChunks);
         boolean excludeCC = CubicChunksConfig.isDimensionExcluded(((World)evt.getObject()).field_73011_w.getDimension());
         boolean forceExclusions = CubicChunksConfig.forceDimensionExcludes;
         boolean impossible = ccGenerator && !ccWorldType;
         if (impossible) {
            throw new Error("Trying to use cubic chunks generator without cubic chunks world type.");
         } else {
            boolean isCC = ccGenerator || ccWorldType && !excludeCC || savedCC && !excludeCC || savedCC && !forceExclusions || ccWorldInfo && !excludeCC;
            if (CubicChunksConfig.forceLoadCubicChunks == CubicChunksConfig.ForceCCMode.LOAD_NOT_EXCLUDED && !excludeCC
               || CubicChunksConfig.forceLoadCubicChunks == CubicChunksConfig.ForceCCMode.ALWAYS) {
               isCC = true;
            }

            if (savedData == null) {
               int minY = CubicChunksConfig.defaultMinHeight;
               int maxY = CubicChunksConfig.defaultMaxHeight;
               if (world.field_73011_w.getDimension() != 0) {
                  WorldSavedCubicChunksData overworld = (WorldSavedCubicChunksData)DimensionManager.getWorld(0)
                     .getPerWorldStorage()
                     .func_75742_a(WorldSavedCubicChunksData.class, "cubicChunksData");
                  if (overworld != null) {
                     minY = overworld.minHeight;
                     maxY = overworld.maxHeight;
                  }
               }

               savedData = new WorldSavedCubicChunksData("cubicChunksData", isCC, minY, maxY);
            }

            savedData.func_76185_a();
            ((World)evt.getObject()).getPerWorldStorage().func_75745_a("cubicChunksData", savedData);
            ((World)evt.getObject()).getPerWorldStorage().func_75744_a();
            if (isCC) {
               if (this.shouldSkipWorld(world)) {
                  CubicChunks.LOGGER
                     .info(
                        "Skipping world "
                           + evt.getObject()
                           + " with type "
                           + ((World)evt.getObject()).func_175624_G()
                           + " due to potential compatibility issues"
                     );
               } else {
                  CubicChunks.LOGGER.info("Initializing world " + evt.getObject() + " with type " + ((World)evt.getObject()).func_175624_G());
                  IntRange generationRange = new IntRange(0, ((ICubicWorldProvider)world.field_73011_w).getOriginalActualHeight());
                  WorldType type = ((World)evt.getObject()).func_175624_G();
                  if (type instanceof ICubicWorldType && ((ICubicWorldType)type).hasCubicGeneratorForWorld(world)) {
                     generationRange = ((ICubicWorldType)type).calculateGenerationHeightRange(world);
                  }

                  int minHeight = savedData.minHeight;
                  int maxHeight = savedData.maxHeight;
                  ((ICubicWorldInternal.Server)world).initCubicWorldServer(new IntRange(minHeight, maxHeight), generationRange);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onWorldServerTick(WorldTickEvent evt) {
      WorldServer world = (WorldServer)evt.world;
      if (evt.phase == Phase.END && ((ICubicWorld)world).isCubicWorld() && evt.side == Side.SERVER) {
         ((ICubicWorldInternal)world).tickCubicWorld();
      }
   }

   @SubscribeEvent
   public void onPlayerJoinWorld(EntityJoinWorldEvent evt) {
      if (evt.getEntity() instanceof EntityPlayerMP && ((ICubicWorld)evt.getWorld()).isCubicWorld()) {
         PacketDispatcher.sendTo(new PacketCubicWorldData((WorldServer)evt.getWorld()), (EntityPlayerMP)evt.getEntity());
      }
   }

   @SubscribeEvent
   public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      VanillaNetworkHandler.removeBedrockPlayer((EntityPlayerMP)event.player);
   }

   private boolean shouldSkipWorld(World world) {
      return !this.allowedServerWorldClasses.contains(world.getClass()) || !this.allowedServerChunkProviderClasses.contains(world.func_72863_F().getClass());
   }

   @SubscribeEvent
   public void onWorldUnload(Unload event) {
      if (!event.getWorld().field_72995_K && ((ICubicWorld)event.getWorld()).isCubicWorld()) {
         ICubicWorld world = (ICubicWorld)event.getWorld();
         if (world.isCubicWorld()) {
            ICubeIO io = ((ICubeProviderInternal.Server)world.getCubeCache()).getCubeIO();

            try {
               io.close();
            } catch (IOException var5) {
               CubicChunks.LOGGER.catching(var5);
            }
         }
      }
   }
}
