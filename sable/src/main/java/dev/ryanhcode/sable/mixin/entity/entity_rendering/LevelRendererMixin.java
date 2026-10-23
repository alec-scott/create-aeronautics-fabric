package dev.ryanhcode.sable.mixin.entity.entity_rendering;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Entities retained in a sub-level sit in plot sections, which are never part of the world's section
 * graph (and are not compiled at all on Sodium's immediate path). Their visibility is already decided
 * by the sub-level-aware frustum check in {@link EntityRendererMixin}, so skip the section gate.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @WrapOperation(
            method = "extractVisibleEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;)Z")
    )
    private boolean sable$subLevelEntitySectionVisible(final LevelRenderer instance, final BlockPos blockPos, final Operation<Boolean> original, @Local final Entity entity) {
        if (Sable.HELPER.getContainingClient(entity) != null) {
            return true;
        }
        return original.call(instance, blockPos);
    }
}
