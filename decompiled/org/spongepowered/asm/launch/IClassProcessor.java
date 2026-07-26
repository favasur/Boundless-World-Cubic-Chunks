package org.spongepowered.asm.launch;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase;
import java.util.EnumSet;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public interface IClassProcessor {
   EnumSet<Phase> handlesClass(Type var1, boolean var2, String var3);

   boolean processClass(Phase var1, ClassNode var2, Type var3, String var4);
}
