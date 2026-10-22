package dev.simulated_team.simulated.mixin.shader_compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zurrtum.create.client.catnip.render.SuperRenderTypeBuffer;
import com.zurrtum.create.client.flywheel.lib.util.ShadersModHelper;
import dev.simulated_team.simulated.index.SimRenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Keeps Create's translucent placement ghosts textured while shader packs are active.
 *
 * <p>Some shader packs discard vertex alpha in their translucent block program,
 * while their entity program can interpret a world-space ghost as a refractive
 * surface. The dedicated ghost layer uses the block atlas through the translucent
 * particle program, preserving both texture color and per-vertex alpha.</p>
 *
 * <p>PORT-NOTE(mc26.1): Create Fly now picks the ghost buffer in {@code TransparentGhostBlockRenderer.Output.update},
 * using {@code RenderTypes.translucentMovingBlock()} instead of the translucent chunk layer.</p>
 */
@Mixin(targets = "com.zurrtum.create.client.catnip.ghostblock.GhostBlockRenderer$TransparentGhostBlockRenderer$Output")
public class GhostBlockRendererMixin {

    @WrapOperation(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/zurrtum/create/client/catnip/render/SuperRenderTypeBuffer;getEarlyBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private VertexConsumer simulated$useShaderCompatibleGhostLayer(final SuperRenderTypeBuffer buffer,
                                                                    final RenderType renderType,
                                                                    final Operation<VertexConsumer> original) {
        if (ShadersModHelper.isShaderPackInUse()) {
            return original.call(buffer, SimRenderTypes.ghostBlock());
        }

        return original.call(buffer, renderType);
    }
}
