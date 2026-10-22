package dev.eriksonn.aeronautics.mixin.levitite;

import dev.eriksonn.aeronautics.index.AeroTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bridges eye submersion in Levitite Blend to vanilla air handling. */
// PORT-NOTE(mc26.1): Levitite fluid tracking and current moved to LevititeBlendFluidBehavior (Fabric fluid API).

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isEyeInFluid", at = @At("RETURN"), cancellable = true)
    private void aeronautics$eyeInLevitite(final TagKey<Fluid> fluidTag,
                                            final CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || !FluidTags.WATER.equals(fluidTag)) {
            return;
        }

        final Entity entity = (Entity) (Object) this;
        final double eyeY = entity.getEyeY();
        final BlockPos eyePos = BlockPos.containing(entity.getX(), eyeY, entity.getZ());
        final FluidState fluidState = entity.level().getFluidState(eyePos);
        if (fluidState.is(AeroTags.FluidTags.LEVITITE_BLEND)
                && eyeY < eyePos.getY() + fluidState.getHeight(entity.level(), eyePos)) {
            // Vanilla uses this query both for drowning and for the HUD air meter.
            cir.setReturnValue(true);
        }
    }
}
