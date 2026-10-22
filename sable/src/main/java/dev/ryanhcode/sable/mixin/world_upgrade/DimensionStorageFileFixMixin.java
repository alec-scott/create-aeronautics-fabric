package dev.ryanhcode.sable.mixin.world_upgrade;

import com.mojang.datafixers.schemas.Schema;
import dev.ryanhcode.sable.util.LegacyWorldFileFixes;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.storage.SubLevelOccupancySavedData;
import dev.ryanhcode.sable.sublevel.storage.SubLevelTicketsSavedData;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
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
    private void sable$moveLegacyWorldData(final CallbackInfo ci) {
        // Sub-level (ship) storage lives in each dimension's folder; see SubLevelHoldingChunkMap.
        this.addFileFixOperation(LegacyWorldFileFixes.movePerDimensionFolder("sublevels"));
        LegacyWorldFileFixes.movePerDimensionSavedData(Sable.MOD_ID, List.of(
                SubLevelOccupancySavedData.FILE_ID,
                SubLevelTicketsSavedData.FILE_ID,
                SubLevelTrackingPointSavedData.FILE_ID
        )).forEach(this::addFileFixOperation);
    }
}
