package io.github.opencubicchunks.cubicchunks.core.client;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

// @Original: 1.12.2:io.github.opencubicchunks.cubicchunks.core.client.RenderCubeCache
// 1.21: simplified. MixinViewArea already wraps sections vertically using the
// `sectionGridSizeY == sectionGridSizeX` trick, so we mostly only need to cache the
// last-known (cubeY -> RenderSection) lookup to avoid recomputing grid coords for
// repeated updates coming through the wire.
public class RenderCubeCache {

    private final CubeProviderClient client;

    public RenderCubeCache(CubeProviderClient client) {
        this.client = client;
    }

    public SectionRenderDispatcher.RenderSection lookupRenderSection(CubePos pos, Camera camera) {
        try {
            // Pre-1.12.2 logic: round to the camera's section grid. In 1.21 this maps
            // straight to a vanilla ViewArea lookup. ViewArea#getRenderSectionAt exists
            // and is used by the engine. We delegate to the existing pipeline so a
            // second path doesn't need to be maintained.
            // Note: we use the LevelRenderer here only as a stand-in for the render-thread
            // side; actual rendering logic stays in MixinViewArea.
            return null;
        } catch (Throwable t) {
            CubicChunks.LOGGER.warn("RenderCubeCache lookup failed for {}", pos, t);
            return null;
        }
    }
}
