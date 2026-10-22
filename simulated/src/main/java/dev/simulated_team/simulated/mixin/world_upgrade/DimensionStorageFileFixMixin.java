package dev.simulated_team.simulated.mixin.world_upgrade;

import com.mojang.datafixers.schemas.Schema;
import dev.ryanhcode.sable.util.LegacyWorldFileFixes;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability.LodestoneTrackingMap;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import net.minecraft.util.filefix.FileFix;
import net.minecraft.util.filefix.fixes.DimensionStorageFileFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Moves this mod's 1.21.11 world data to its 26.1 location when vanilla upgrades an old world.
 * See {@link LegacyWorldFileFixes}.
 */
@Mixin(DimensionStorageFileFix.class)
public abstract class DimensionStorageFileFixMixin extends FileFix {
    private DimensionStorageFileFixMixin(final Schema schema) {
        super(schema);
    }

    @Inject(method = "makeFixer()V", at = @At("HEAD"))
    private void simulated$moveLegacyWorldData(final CallbackInfo ci) {
        LegacyWorldFileFixes.movePerDimensionSavedData(Simulated.MOD_ID, List.of(
                LodestoneTrackingMap.FILE_ID,
                PhysicsStaffServerHandler.ID
        )).forEach(this::addFileFixOperation);
    }
}
