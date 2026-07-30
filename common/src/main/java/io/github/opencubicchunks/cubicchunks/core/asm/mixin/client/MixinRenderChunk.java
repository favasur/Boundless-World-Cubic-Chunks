package io.github.opencubicchunks.cubicchunks.core.asm.mixin.client;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla 1.21.1's {@code RenderChunk.<init>(LevelChunk)} iterates over
 * {@code chunk.getSections()} and at bytecode offset 92 unconditionally invokes
 * {@code LevelChunkSection.hasOnlyAir()} on each entry. Cubic-chunks worlds
 * routinely leave {@code sections[i]} null for Y slots vanilla never wrote to
 * (the column has no in-window setBlockState / setFluidState / feature call),
 * so the very first null entry — picked by iterating from index 0 — NPEs the
 * client during render-region compilation.
 *
 * <p>Mirror fix of {@code MixinLevelChunk.cc$getFluidState} and the
 * cube-preemption null-section path: pre-fill null slots with a fresh all-air
 * {@link LevelChunkSection} so the iteration never dereferences a null. We
 * pre-fill in-place so vanilla's later
 * {@code MixinLevelChunk.cc$redirectSetSection} setBlockState flow sees a
 * non-null slot and our redirect's "no cube yet" write-back path returns the
 * SAME pre-filled instance (no double-allocation, no shared-singleton mutation
 * hazard across chunks).
 *
 * <p>Per-slot allocation, not a shared singleton: a shared empty section would
 * be safe for renderer reads, but our redirect on setBlockState routes writes
 * through the same LevelChunkSection reference — a shared singleton would mean
 * a creative-mode block break writes AIR into a single instance that's also
 * held by every other pre-allocated chunk in the render distance.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.RenderChunk")
public abstract class MixinRenderChunk {

    @Redirect(
            method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/LevelChunk;getSections()[Lnet/minecraft/world/level/chunk/LevelChunkSection;")
    )
    private LevelChunkSection[] cc$preAllocateNullSections(LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        // Single pass: lazy registry lookup on first null slot, so a fully-populated
        // column never pays for the registryOrThrow call.
        Registry<Biome> registry = null;
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] == null) {
                if (registry == null) {
                    registry = chunk.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
                }
                sections[i] = new LevelChunkSection(registry);
            }
        }
        return sections;
    }
}
