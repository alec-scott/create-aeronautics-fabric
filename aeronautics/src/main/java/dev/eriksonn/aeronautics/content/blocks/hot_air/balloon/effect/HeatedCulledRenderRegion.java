package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerData;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.util.LevelAccelerator;
import net.minecraft.core.BlockPos;
import org.lwjgl.system.NativeResource;

import java.util.Iterator;
import java.util.List;

/**
 * The hot-air volume of one client balloon, rebuilt by {@code ClientBalloon} whenever hot air is added or removed,
 * so the overlay grows as the balloon fills. Drawn by {@link ClientBalloonEffectRenderer}.
 *
 * <p>PORT-NOTE(mc26.1): was a stub on Fabric; the volume is now meshed with {@link HeatedCulledRenderRegionBuilder}.</p>
 */
public class HeatedCulledRenderRegion implements NativeResource {
    private final BlockPos origin;
    private final HeatedCulledRenderRegionBuilder builder;

    public HeatedCulledRenderRegion(final LevelAccelerator accelerator, final Balloon balloon) {
        final BoundingBox3ic bounds = balloon.getBounds();
        this.origin = new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
        final int gridSize = Math.max(bounds.maxX() - bounds.minX(), Math.max(bounds.maxY() - bounds.minY(), bounds.maxZ() - bounds.minZ())) + 1;
        this.builder = new HeatedCulledRenderRegionBuilder(this.origin, accelerator, Math.max(gridSize, 1));

        for (final List<BalloonLayerData> layers : balloon.getGraph().getAllLayers()) {
            if (layers == null) continue;
            for (final BalloonLayerData layer : layers) {
                final Iterator<BlockPos> it = layer.nonSolidBlockIterator();
                while (it.hasNext()) {
                    final BlockPos pos = it.next();
                    final int x = pos.getX() - this.origin.getX(), y = pos.getY() - this.origin.getY(), z = pos.getZ() - this.origin.getZ();
                    if (x >= 0 && y >= 0 && z >= 0 && x < gridSize && y < gridSize && z < gridSize)
                        this.builder.add(x, y, z);
                }
            }
        }
        // one cube per block, so each face shows one copy of the texture like the per-block UVs of the shader
        this.builder.buildNoGreedy();
    }

    /** The volume's minimum corner, in the coordinates of the level (or plot) the balloon lives in. */
    public BlockPos origin() {
        return this.origin;
    }

    public void render(final PoseStack.Pose pose, final VertexConsumer sides, final float scroll, final float alpha) {
        this.builder.renderOverlay(pose, sides, scroll, 0.0f, alpha);
    }

    @Override
    public void free() {
    }
}
