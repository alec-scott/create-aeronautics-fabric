package dev.eriksonn.aeronautics.fabric;

import dev.eriksonn.aeronautics.index.AeroTags;
import net.fabricmc.fabric.api.registry.fluid.EntityFluidInteractionRegistry;
import net.fabricmc.fabric.api.registry.fluid.FluidBehavior;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

/**
 * Entity physics for Levitite Blend: an upward current and buoyant swimming, matching the NeoForge fluid type.
 *
 * <p>PORT-NOTE(mc26.1): 26.1 tracks entity fluids through {@code EntityFluidInteraction}, so this replaces the
 * 1.21.11 mixins that pushed entities from {@code updateInWaterStateAndDoFluidPushing} and took over
 * {@code LivingEntity.travel}. Fabric calls {@link #travelInFluid} from vanilla's fluid travel path, which is only
 * reached when the entity is affected by fluids, cannot stand on the fluid and is not in water or lava: the same
 * conditions the old travel mixin checked. Jumping is applied by vanilla's {@code jumpInLiquid} for registered
 * fluids, so the old explicit +0.04 jump impulse is not repeated here.</p>
 */
public final class LevititeBlendFluidBehavior implements FluidBehavior {
    // Same scale as vanilla's non-ultra-warm lava current, in the opposite direction (upward fluid).
    private static final double CURRENT_SCALE = -0.0023333333333333335D;

    private LevititeBlendFluidBehavior() {
    }

    public static void register() {
        EntityFluidInteractionRegistry.register(AeroTags.FluidTags.LEVITITE_BLEND, new LevititeBlendFluidBehavior());
    }

    @Override
    public void handleFluidInteractionUpdate(final TagKey<Fluid> fluid, final Entity entity,
                                             final EntityFluidInteraction interaction, final boolean canPushEntity) {
        if (canPushEntity) {
            interaction.applyCurrentTo(fluid, entity, CURRENT_SCALE);
        }
    }

    @Override
    public void travelInFluid(final TagKey<Fluid> fluid, final LivingEntity entity, final Vec3 input,
                              final double baseGravity, final boolean isFalling, final double oldY) {
        final double fluidHeight = entity.getFluidHeight(fluid);
        double gravity = baseGravity;
        if (!entity.isCrouching()) {
            gravity = Math.clamp(gravity * (1.0D - fluidHeight), 0.0D, gravity);
        }

        entity.moveRelative(0.02F, input);
        entity.move(MoverType.SELF, entity.getDeltaMovement());

        if (fluidHeight <= entity.getFluidJumpThreshold()) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.5D, 0.7D, 0.5D));
            entity.setDeltaMovement(entity.getFluidFallingAdjustedMovement(gravity, isFalling, entity.getDeltaMovement()));
        } else {
            entity.setDeltaMovement(entity.getDeltaMovement().scale(0.5D));
        }

        if (gravity != 0.0D) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0D, -gravity / 4.0D, 0.0D));
        }

        final Vec3 movement = entity.getDeltaMovement();
        if (entity.horizontalCollision
                && entity.isFree(movement.x, movement.y + 0.6D - entity.getY() + oldY, movement.z)) {
            entity.setDeltaMovement(movement.x, 0.3D, movement.z);
        }
    }
}
