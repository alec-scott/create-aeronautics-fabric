package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.ryanhcode.sable.render.region.SimpleCulledRenderRegionBuilder;
import dev.ryanhcode.sable.util.LevelAccelerator;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * The faces of a balloon's hot-air volume, in balloon-local coordinates.
 *
 * <p>PORT-NOTE(mc26.1): upstream drew these with the Veil {@code hot_air_overlay} shader into a framebuffer and
 * soft-light blended it onto the scene. Veil has no 26.1 build, so the shader's vertex logic (scrolling texture, fade below
 * {@code CutoffY}) is done here on the CPU and drawn with a vanilla translucent
 * emissive render type. Faces are nudged inward so they sit just in front of the envelope's inner faces.</p>
 */
public class HeatedCulledRenderRegionBuilder extends SimpleCulledRenderRegionBuilder {
    /** How far faces are pulled inside the volume, so they win the depth test against the envelope's inner faces. */
    private static final float INSET = 0.004f;

    private final BlockPos worldOrigin;
    private final LevelAccelerator accelerator;

    public HeatedCulledRenderRegionBuilder(final BlockPos worldOrigin, final LevelAccelerator accelerator, final int gridSize) {
        super(gridSize);
        this.worldOrigin = worldOrigin;
        this.accelerator = accelerator;
    }

    /**
     * Draws the volume's boundary faces.
     *
     * @param sides   consumer textured with the heat overlay
     * @param scroll  how far the texture has risen, in texture heights
     * @param cutoffY local height below which faces fade out
     * @param alpha   overall opacity, 0-1
     */
    public void renderOverlay(@NotNull final PoseStack.Pose pose, @NotNull final VertexConsumer sides,
                              final float scroll, final float cutoffY, final float alpha) {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // pixel-aligned like hot_air_overlay.vsh; negated so the texture rises (upstream scrolled downward)
        final float offset = Mth.frac(-Mth.floor(scroll * 16.0f) / 16.0f);

        for (final Cube cube : this.getCubes()) {
            final float x0 = cube.x(), y0 = cube.y(), z0 = cube.z();
            final float x1 = x0 + cube.sizeX(), y1 = y0 + cube.sizeY(), z1 = z0 + cube.sizeZ();

            if (this.shouldFaceRender(cube, Direction.NORTH))
                this.side(pose, sides, x0, z0 + INSET, x1, z0 + INSET, y0, y1, Direction.NORTH, offset, cutoffY, alpha);
            if (this.shouldFaceRender(cube, Direction.SOUTH))
                this.side(pose, sides, x1, z1 - INSET, x0, z1 - INSET, y0, y1, Direction.SOUTH, offset, cutoffY, alpha);
            if (this.shouldFaceRender(cube, Direction.WEST))
                this.side(pose, sides, x0 + INSET, z1, x0 + INSET, z0, y0, y1, Direction.WEST, offset, cutoffY, alpha);
            if (this.shouldFaceRender(cube, Direction.EAST))
                this.side(pose, sides, x1 - INSET, z0, x1 - INSET, z1, y0, y1, Direction.EAST, offset, cutoffY, alpha);

            // PORT-NOTE(mc26.1): upstream gave horizontal faces the lava strip (Sampler1); they use the side heat
            // texture and scroll instead, so the ceiling and floor match the walls.
            if (this.shouldFaceRender(cube, Direction.UP))
                this.flatSide(pose, sides, x0, x1, y1 - INSET, z0, z1, Direction.UP, offset, cutoffY, alpha);
            if (this.shouldFaceRender(cube, Direction.DOWN)
                    && this.accelerator.getBlockState(pos.set(cube.x(), cube.y() - 1, cube.z()).offset(this.worldOrigin)).is(AeroTags.BlockTags.AIRTIGHT))
                this.flatSide(pose, sides, x0, x1, y0 + INSET, z0, z1, Direction.DOWN, offset, cutoffY, alpha);
        }
    }

    /** A horizontal face with the side texture, scrolling along z and split at the wrap seam like {@link #side}. */
    private void flatSide(final PoseStack.Pose pose, final VertexConsumer consumer, final float x0, final float x1, final float y,
                          final float z0, final float z1, final Direction dir, final float offset, final float cutoffY, final float alpha) {
        final float seam = z0 + (1.0f - offset) * (z1 - z0);
        this.flatQuad(pose, consumer, x0, x1, y, z0, seam, offset, 1.0f, dir, cutoffY, alpha);
        if (offset > 0.0f)
            this.flatQuad(pose, consumer, x0, x1, y, seam, z1, 0.0f, offset, dir, cutoffY, alpha);
    }

    private void flatQuad(final PoseStack.Pose pose, final VertexConsumer consumer, final float x0, final float x1, final float y,
                          final float za, final float zb, final float va, final float vb, final Direction dir, final float cutoffY, final float alpha) {
        if (zb <= za) return;
        this.vertex(pose, consumer, x0, y, za, 0, va, dir, cutoffY, alpha);
        this.vertex(pose, consumer, x0, y, zb, 0, vb, dir, cutoffY, alpha);
        this.vertex(pose, consumer, x1, y, zb, 1, vb, dir, cutoffY, alpha);
        this.vertex(pose, consumer, x1, y, za, 1, va, dir, cutoffY, alpha);
    }

    /** A vertical face from (ax, az) to (bx, bz); the texture scrolls upward and wraps, so the face is split at the seam. */
    private void side(final PoseStack.Pose pose, final VertexConsumer consumer, final float ax, final float az, final float bx, final float bz,
                      final float y0, final float y1, final Direction dir, final float offset, final float cutoffY, final float alpha) {
        // v runs from offset at y0 to offset + 1 at y1; the seam is where it passes 1
        final float seam = y0 + (1.0f - offset) * (y1 - y0);
        this.quad(pose, consumer, ax, az, bx, bz, y0, seam, offset, 1.0f, dir, cutoffY, alpha);
        if (offset > 0.0f)
            this.quad(pose, consumer, ax, az, bx, bz, seam, y1, 0.0f, offset, dir, cutoffY, alpha);
    }

    private void quad(final PoseStack.Pose pose, final VertexConsumer consumer, final float ax, final float az, final float bx, final float bz,
                      final float ya, final float yb, final float va, final float vb, final Direction dir, final float cutoffY, final float alpha) {
        if (yb <= ya) return;
        this.vertex(pose, consumer, ax, ya, az, 0, va, dir, cutoffY, alpha);
        this.vertex(pose, consumer, ax, yb, az, 0, vb, dir, cutoffY, alpha);
        this.vertex(pose, consumer, bx, yb, bz, 1, vb, dir, cutoffY, alpha);
        this.vertex(pose, consumer, bx, ya, bz, 1, va, dir, cutoffY, alpha);
    }

    private void vertex(final PoseStack.Pose pose, final VertexConsumer consumer, final float x, final float y, final float z,
                        final float u, final float v, final Direction dir, final float cutoffY, final float alpha) {
        // hot_air_overlay.vsh: vertexColor.a = clamp(Position.y - CutoffY, 0, 1)
        final int a = (int) (255.0f * alpha * Mth.clamp(y - cutoffY, 0.0f, 1.0f));
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, dir.getStepX(), dir.getStepY(), dir.getStepZ());
    }
}
