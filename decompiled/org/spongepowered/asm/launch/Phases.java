package org.spongepowered.asm.launch;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase;
import java.util.EnumSet;

public final class Phases {
   public static final EnumSet<Phase> NONE = EnumSet.noneOf(Phase.class);
   public static final EnumSet<Phase> BEFORE_ONLY = EnumSet.of(Phase.BEFORE);
   public static final EnumSet<Phase> AFTER_ONLY = EnumSet.of(Phase.AFTER);

   private Phases() {
   }
}
