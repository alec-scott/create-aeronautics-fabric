package dev.simulated_team.simulated.fabric.mixin.physics_staff;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.client.render.FirstPersonItemFocus;
import dev.simulated_team.simulated.content.blocks.lasers.IrisLaserRenderQueue;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffItem;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffRenderHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Quaternionf;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    // PORT-NOTE(mc26.1): the projection matrix is no longer passed to LevelRenderer.renderLevel; capture the local instead.
    private static final String RENDER_LEVEL = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZLnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;)V";

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = RENDER_LEVEL))
    private void simulated$captureWorldProjection(final DeltaTracker deltaTracker, final CallbackInfo ci,
                                                  @Local(ordinal = 0) final Matrix4f projection) {
        FirstPersonItemFocus.captureWorldProjection(projection);
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void simulated$replaceVanillaOutlineForPhysicsStaff(
            final CallbackInfoReturnable<Boolean> cir) {
        if (this.minecraft.player != null && PhysicsStaffItem.isHolding(this.minecraft.player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = RENDER_LEVEL,
                    shift = At.Shift.AFTER
            )
    )
    private void simulated$renderPhysicsStaffShaderOverlay(final DeltaTracker deltaTracker,
                                                            final CallbackInfo ci) {
        final Camera camera = this.minecraft.gameRenderer.getMainCamera();
        final PoseStack poseStack = new PoseStack();
        poseStack.mulPose(camera.rotation().conjugate(new Quaternionf()));

        final MultiBufferSource.BufferSource bufferSource = this.minecraft.renderBuffers().bufferSource();
        IrisLaserRenderQueue.drawAfterShaderComposite(bufferSource);
        if (PhysicsStaffRenderHandler.renderAfterShaderComposite(bufferSource, poseStack, camera)) {
            bufferSource.endBatch();
        }
    }
}
