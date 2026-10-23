package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ClientBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Draws the heat shimmer inside filled hot-air balloons: the heat overlay rising up the envelope's inner walls and
 * across its ceiling, fading out at the bottom of the balloon.
 *
 * <p>PORT-NOTE(mc26.1): upstream used Veil (a {@code hot_air_overlay} shader into a framebuffer, then a
 * {@code soft_light} post blend), which has no 26.1 build; on Fabric 1.21.11 it was stubbed out. This draws the same
 * geometry with vanilla translucent emissive render types at the end of the main level pass. The soft-light blend
 * becomes plain alpha blending.</p>
 */
public class ClientBalloonEffectRenderer {

    private static final Identifier SIDE_TEXTURE = Aeronautics.path("textures/special/heat_overlay.png");

    private static final RenderType SIDES = RenderTypes.entityTranslucentEmissive(SIDE_TEXTURE, false);

    /** Texture heights the overlay rises per tick. */
    private static final float SCROLL_SPEED = 1.0f / 40.0f;
    private static final float SIDE_ALPHA = 0.55f;

    private static final Quaternionf ORIENTATION = new Quaternionf();

    public static void render(final PoseStack poseStack, final float partialTick) {
        final Minecraft mc = Minecraft.getInstance();
        final ClientLevel level = mc.level;
        if (level == null) return;

        final BalloonMap map = BalloonMap.MAP.get(level);
        if (map == null || map.isEmpty()) return;

        final Camera camera = mc.gameRenderer.getMainCamera();
        final Vec3 cam = camera.position();
        final float time = level.getGameTime() + partialTick;
        final float scroll = time * SCROLL_SPEED;

        final MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean drew = false;

        for (final Balloon balloon : map.getBalloons()) {
            if (!(balloon instanceof final ClientBalloon clientBalloon) || !clientBalloon.isValid()) continue;
            final HeatedCulledRenderRegion region = clientBalloon.getRenderRegion();
            if (region == null) continue;

            final BlockPos origin = region.origin();
            poseStack.pushPose();
            final ClientSubLevel subLevel = Sable.HELPER.getContainingClient(origin);
            if (subLevel != null) {
                // the balloon is on a ship: its blocks are at plot coordinates, so place it with the ship's render pose
                final Pose3dc pose = subLevel.renderPose();
                final Vec3 physical = pose.transformPosition(Vec3.atLowerCornerOf(origin));
                poseStack.translate(physical.x - cam.x, physical.y - cam.y, physical.z - cam.z);
                poseStack.mulPose(ORIENTATION.set(pose.orientation()));
                poseStack.scale((float) pose.scale().x(), (float) pose.scale().y(), (float) pose.scale().z());
            } else {
                poseStack.translate(origin.getX() - cam.x, origin.getY() - cam.y, origin.getZ() - cam.z);
            }

            region.render(poseStack.last(), buffers.getBuffer(SIDES), scroll, SIDE_ALPHA);
            poseStack.popPose();
            drew = true;
        }

        if (drew) {
            buffers.endBatch(SIDES);
        }
    }
}
