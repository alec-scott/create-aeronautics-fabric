package dev.simulated_team.simulated.mixin.sable_render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.client.content.contraptions.render.ContraptionEntityRenderer;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Flywheel visualizes contraptions at their plot-space positions, which Sable does not transform.
 * Contraptions retained in a sub-level (propeller bearings, bearings, pistons...) use Create's
 * buffered renderer instead, which goes through the sub-level-aware entity render path.
 */
@Mixin(ContraptionEntityRenderer.class)
public class ContraptionEntityRendererMixin {

    @WrapOperation(
            method = "extractRenderState(Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;Lcom/zurrtum/create/client/content/contraptions/render/ContraptionEntityRenderer$AbstractContraptionState;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/zurrtum/create/client/flywheel/api/visualization/VisualizationManager;supportsVisualization(Lnet/minecraft/world/level/LevelAccessor;)Z"
            )
    )
    private boolean simulated$useFallbackRendererInSubLevel(
            final LevelAccessor level,
            final Operation<Boolean> original,
            final AbstractContraptionEntity entity
    ) {
        if (Sable.HELPER.getContainingClient(entity) != null) {
            return false;
        }
        return original.call(level);
    }
}
