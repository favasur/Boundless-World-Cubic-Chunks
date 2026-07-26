package io.github.opencubicchunks.cubicchunks.core.client;

import io.github.opencubicchunks.cubicchunks.api.util.MathUtil;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.api.worldgen.VanillaCompatibilityGeneratorProviderBase;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client.IGuiCreateWorld;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client.IGuiOptionsRowList;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client.IGuiScreen;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.IGuiVideoSettings;
import io.github.opencubicchunks.cubicchunks.core.server.ICubicPlayerList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.GuiOptionsRowList.Row;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Post;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.relauncher.Side;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ClientEventHandler {
   public ClientEventHandler() {
   }

   @SubscribeEvent
   public void onWorldClientTickEvent(ClientTickEvent evt) {
      ICubicWorldInternal world = (ICubicWorldInternal)FMLClientHandler.instance().getWorldClient();
      if (world != null && !Minecraft.func_71410_x().func_147113_T()) {
         if (evt.phase == Phase.END && world.isCubicWorld()) {
            world.tickCubicWorld();
         }
      }
   }

   @SubscribeEvent
   public void onServerTick(ServerTickEvent event) {
      ICubicPlayerList playerList = (ICubicPlayerList)FMLCommonHandler.instance().getMinecraftServerInstance().func_184103_al();
      int prevDist = playerList.getVerticalViewDistance();
      int newDist = CubicChunksConfig.verticalCubeLoadDistance;
      if (prevDist != newDist) {
         CubicChunks.LOGGER.info("Changing vertical view distance to {}, from {}", newDist, prevDist);
         playerList.setVerticalViewDistance(newDist);
      }
   }

   @SubscribeEvent
   public void initGuiEvent(Post event) {
      GuiScreen currentGui = event.getGui();
      if (currentGui instanceof GuiVideoSettings) {
         GuiVideoSettings gvs = (GuiVideoSettings)currentGui;
         if (!FMLClientHandler.instance().hasOptifine()) {
            IGuiOptionsRowList gowl = (IGuiOptionsRowList)((IGuiVideoSettings)gvs).getOptionsRowList();
            Row row = this.createRow(100, gvs.field_146294_l);
            gowl.getOptions().add(1, row);
         } else {
            int idx = 3;
            int btnSpacing = 20;
            ((IGuiScreen)gvs)
               .getButtonList()
               .add(
                  idx,
                  new ClientEventHandler.VertViewDistanceSlider(100, gvs.field_146294_l / 2 - 155 + 160, gvs.field_146295_m / 6 + btnSpacing * (idx / 2) - 12)
               );
            List<GuiButton> buttons = ((IGuiScreen)gvs).getButtonList();

            for (int i = 0; i < buttons.size() - 4; i++) {
               GuiButton btn = buttons.get(i);
               int x = gvs.field_146294_l / 2 - 155 + i % 2 * 160;
               int y = gvs.field_146295_m / 6 + 21 * (i / 2) - 12;
               btn.field_146128_h = x;
               btn.field_146129_i = y;
            }

            for (int i = buttons.size() - 4; i < buttons.size() - 1; i++) {
               GuiButton btn = buttons.get(i);
               int newBtnWidth = 100;
               int minX = gvs.field_146294_l / 2 - 155;
               int maxX = gvs.field_146294_l / 2 - 155 + 160 + btn.field_146120_f;
               int minXCenter = minX + newBtnWidth / 2;
               int maxXCenter = maxX - newBtnWidth / 2;
               int x = minXCenter + i % 3 * (maxXCenter - minXCenter) / 2 - newBtnWidth / 2;
               int y = gvs.field_146295_m / 6 + 21 * (buttons.size() - 4) / 2 - 12;
               btn.field_146128_h = x;
               btn.field_146129_i = y;
               btn.field_146120_f = newBtnWidth;
            }
         }
      }
   }

   private Row createRow(int buttonId, int width) {
      ClientEventHandler.VertViewDistanceSlider slider = new ClientEventHandler.VertViewDistanceSlider(buttonId, width / 2 - 155 + 160, 0);
      return new Row(slider, null);
   }

   private class VertViewDistanceSlider extends GuiButton {
      private final int MAX_VIEW_DIST = CubicChunks.hasOptifine() ? 64 : 32;
      private float sliderValue = MathUtil.unlerp((long)CubicChunksConfig.verticalCubeLoadDistance, 2L, (long)this.MAX_VIEW_DIST);
      public boolean dragging;

      public VertViewDistanceSlider(int buttonId, int x, int y) {
         super(buttonId, x, y, 150, 20, "");
         this.field_146126_j = this.createDisplayString();
      }

      protected int func_146114_a(boolean mouseOver) {
         return 0;
      }

      protected void func_146119_b(Minecraft mc, int mouseX, int mouseY) {
         if (this.field_146125_m) {
            if (this.dragging) {
               this.sliderValue = (float)(mouseX - (this.field_146128_h + 4)) / (float)(this.field_146120_f - 8);
               this.sliderValue = MathHelper.func_76131_a(this.sliderValue, 0.0F, 1.0F);
               CubicChunksConfig.setVerticalViewDistance(Math.round(MathUtil.lerp(this.sliderValue, 2.0F, (float)this.MAX_VIEW_DIST)));
               this.sliderValue = MathUtil.unlerp((long)CubicChunksConfig.verticalCubeLoadDistance, 2L, (long)this.MAX_VIEW_DIST);
               this.field_146126_j = this.createDisplayString();
            }

            mc.func_110434_K().func_110577_a(field_146122_a);
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            this.func_73729_b(this.field_146128_h + (int)(this.sliderValue * (float)(this.field_146120_f - 8)), this.field_146129_i, 0, 66, 4, 20);
            this.func_73729_b(this.field_146128_h + (int)(this.sliderValue * (float)(this.field_146120_f - 8)) + 4, this.field_146129_i, 196, 66, 4, 20);
         }
      }

      public boolean func_146116_c(Minecraft mc, int mouseX, int mouseY) {
         if (super.func_146116_c(mc, mouseX, mouseY)) {
            this.sliderValue = (float)(mouseX - (this.field_146128_h + 4)) / (float)(this.field_146120_f - 8);
            this.sliderValue = MathHelper.func_76131_a(this.sliderValue, 0.0F, 1.0F);
            CubicChunksConfig.setVerticalViewDistance(Math.round(MathUtil.lerp(this.sliderValue, 2.0F, (float)this.MAX_VIEW_DIST)));
            this.sliderValue = MathUtil.unlerp((long)CubicChunksConfig.verticalCubeLoadDistance, 2L, (long)this.MAX_VIEW_DIST);
            this.field_146126_j = this.createDisplayString();
            this.dragging = true;
            return true;
         } else {
            return false;
         }
      }

      private String createDisplayString() {
         return I18n.func_135052_a("cubicchunks.gui.vertical_cube_load_distance", new Object[]{CubicChunksConfig.verticalCubeLoadDistance});
      }

      public void func_146118_a(int mouseX, int mouseY) {
         this.dragging = false;
      }
   }

   @EventBusSubscriber(
      modid = "cubicchunks",
      value = {Side.CLIENT}
   )
   public static class WorldSelectionCubicChunks {
      private static final int MAP_TYPE_ID = 5;
      private static final int ALLOW_CHEATS_ID = 6;
      private static final int CUSTOMIZE_ID = 8;
      private static final int MORE_WORLD_OPTIONS = 3;
      private static final int CC_ENABLE_BUTTON_ID = 11;
      private static final List<ResourceLocation> LIST_OF_GEN_OPTIONS = new ArrayList<>();
      private static int CURRENT_GEN_OPTION = 0;

      public WorldSelectionCubicChunks() {
      }

      @SubscribeEvent
      public static void guiInit(Post event) {
         GuiScreen gui = event.getGui();
         if (isCreateWorldGui(gui)) {
            init((GuiCreateWorld)gui, event.getButtonList());
         }
      }

      private static void init(GuiCreateWorld gui, List<GuiButton> buttons) {
         if (!getButton(buttons, 11).isPresent()) {
            GuiButton enableCC = new GuiButton(11, 0, 0, 20, 20, "enable");
            enableCC.field_146125_m = false;
            buttons.add(enableCC);
            Optional<GuiButton> customizeButton = getButton(buttons, 8);
            Optional<GuiButton> allowCheats = getButton(buttons, 6);
            customizeButton.ifPresent(b -> allowCheats.ifPresent(c -> {
                  b.field_146129_i = c.field_146129_i - 21;
                  GuiButton mapTypeButton = getButton(buttons, 5).get();
                  enableCC.field_146128_h = c.field_146128_h;
                  enableCC.field_146129_i = b.field_146129_i;
                  enableCC.field_146120_f = c.field_146120_f;
                  enableCC.field_146121_g = c.field_146121_g;
                  enableCC.field_146125_m = mapTypeButton.field_146125_m;
                  refreshText(gui, enableCC);
               }));
            LIST_OF_GEN_OPTIONS.addAll(VanillaCompatibilityGeneratorProviderBase.REGISTRY.getKeys());
            CURRENT_GEN_OPTION = LIST_OF_GEN_OPTIONS.indexOf(new ResourceLocation(CubicChunksConfig.compatibilityGeneratorType));
         }
      }

      private static void refreshText(GuiCreateWorld gui, GuiButton enableBtn) {
         String txt;
         if (CubicChunksConfig.forceLoadCubicChunks == CubicChunksConfig.ForceCCMode.NONE) {
            txt = "cubicchunks.gui.worldmenu.cc_disable";
         } else {
            VanillaCompatibilityGeneratorProviderBase provider = (VanillaCompatibilityGeneratorProviderBase)VanillaCompatibilityGeneratorProviderBase.REGISTRY
               .getValue(new ResourceLocation(CubicChunksConfig.compatibilityGeneratorType));
            txt = provider.getUnlocalizedName();
         }

         enableBtn.field_146126_j = I18n.func_135052_a(txt, new Object[0]);
      }

      @SubscribeEvent
      public static void actionPerformed(net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Post event) {
         GuiScreen gui = event.getGui();
         GuiButton button = event.getButton();
         if (isCreateWorldGui(gui)) {
            switch (button.field_146127_k) {
               case 3:
                  init((GuiCreateWorld)gui, event.getButtonList());
               case 5:
                  GuiButton enableCC = null;
                  GuiButton mapType = null;

                  for (GuiButton b : event.getButtonList()) {
                     if (b.field_146127_k == 11) {
                        enableCC = b;
                     } else if (b.field_146127_k == 5) {
                        mapType = b;
                     }
                  }

                  assert enableCC != null;

                  boolean isCubicChunksType = WorldType.field_77139_a[((IGuiCreateWorld)gui).getSelectedIndex()] instanceof ICubicWorldType;
                  enableCC.field_146125_m = mapType != null && !isCubicChunksType && mapType.field_146125_m;
                  break;
               case 11:
                  CURRENT_GEN_OPTION++;
                  if (CURRENT_GEN_OPTION >= LIST_OF_GEN_OPTIONS.size()) {
                     CubicChunksConfig.disableCubicChunks();
                     CURRENT_GEN_OPTION = -1;
                  } else {
                     CubicChunksConfig.setGenerator(LIST_OF_GEN_OPTIONS.get(CURRENT_GEN_OPTION));
                  }

                  refreshText((GuiCreateWorld)gui, button);
            }
         }
      }

      private static boolean isCreateWorldGui(GuiScreen gui) {
         return gui instanceof GuiCreateWorld;
      }

      private static Optional<GuiButton> getButton(List<GuiButton> buttons, int id) {
         return buttons.stream().filter(b -> b.field_146127_k == id).findFirst();
      }
   }
}
