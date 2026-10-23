package dev.ryanhcode.sable.mixin.compatibility.entity_model_features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * EMF skips animation frames based on distance to the player. Block entities on a sub-level sit at
 * plot coordinates millions of blocks away, so their animations (e.g. chest lids) never advanced.
 * Measure from where the block actually is in the world instead.
 */
@Pseudo
@Mixin(targets = "traben.entity_model_features.utils.EMFLODHandler")
public class EMFLODHandlerMixin {

    @ModifyExpressionValue(
            method = "distanceOfEntityFrom",
            at = @At(
                    value = "INVOKE",
                    target = "Ltraben/entity_model_features/models/animation/state/EMFEntityRenderState;blockPos()Lnet/minecraft/core/BlockPos;"
            )
    )
    private static BlockPos sable$useWorldPosition(final BlockPos pos) {
        final ClientSubLevel subLevel = Sable.HELPER.getContainingClient(pos);
        if (subLevel == null) {
            return pos;
        }

        final Vector3d worldPos = subLevel.logicalPose().transformPosition(new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        return BlockPos.containing(worldPos.x, worldPos.y, worldPos.z);
    }
}
