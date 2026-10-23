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
 * soft-light blended it onto the scene. Veil has no 26.1 build, so the shader's vertex logic (scrolling side texture,
 * animated top texture, fade below {@code CutoffY}) is done here on the CPU and drawn with a vanilla translucent
 * emissive render type. Faces are nudged inward so they sit just in front of the envelope's inner faces.</p>
 */
public class HeatedCulledRenderRegionBuilder extends SimpleCulledRenderRegionBuilder {
    /** How far faces are pulled inside the volume, so they win the depth test against the envelope's inner faces. */
    private static final float INSET = 0.004f;
    /** {@code lava_still.png} is a strip of 20 frames. */
    private static final int TOP_FRAMES = 20;

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
     * @param sides   consumer textured with the heat overlay, for vertical faces
     * @param caps    consumer textured with the lava strip, for horizontal faces
     * @param scroll  vertical texture offset of the side faces, in texture heights
     * @param frame   frame of the lava strip to show on horizontal faces
     * @param cutoffY local height below which faces fade out
     * @param alpha   overall opacity, 0-1
     */
    public void renderOverlay(@NotNull final PoseStack.Pose pose, @NotNull final VertexConsumer sides, @NotNull final VertexConsumer caps,
                              final float scroll, final int frame, final float cutoffY, final float alpha) {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final float offset = Mth.frac(Mth.floor(scroll * 16.0f) / 16.0f);
        final float v0 = (float) Math.floorMod(frame, TOP_FRAMES) / TOP_FRAMES;
        final float v1 = v0 + 1.0f / TOP_FRAMES;

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

            if (this.shouldFaceRender(cube, Direction.UP))
                this.cap(pose, caps, x0, x1, y1 - INSET, z0, z1, Direction.UP, v0, v1, cutoffY, alpha);
            if (this.shouldFaceRender(cube, Direction.DOWN)
                    && this.accelerator.getBlockState(pos.set(cube.x(), cube.y() - 1, cube.z()).offset(this.worldOrigin)).is(AeroTags.BlockTags.AIRTIGHT))
                this.cap(pose, caps, x0, x1, y0 + INSET, z0, z1, Direction.DOWN, v0, v1, cutoffY, alpha);
        }
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

    private void cap(final PoseStack.Pose pose, final VertexConsumer consumer, final float x0, final float x1, final float y,
                     final float z0, final float z1, final Direction dir, final float v0, final float v1, final float cutoffY, final float alpha) {
        this.vertex(pose, consumer, x0, y, z1, 0, v1, dir, cutoffY, alpha);
        this.vertex(pose, consumer, x1, y, z1, 1, v1, dir, cutoffY, alpha);
        this.vertex(pose, consumer, x1, y, z0, 1, v0, dir, cutoffY, alpha);
        this.vertex(pose, consumer, x0, y, z0, 0, v0, dir, cutoffY, alpha);
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
