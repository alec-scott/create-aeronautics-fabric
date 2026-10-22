package dev.eriksonn.aeronautics.mixin.world_upgrade;

import com.mojang.datafixers.schemas.Schema;
import dev.ryanhcode.sable.util.LegacyWorldFileFixes;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.CrystallizationWorldSaveData;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonLevelSavedData;
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
    private void aeronautics$moveLegacyWorldData(final CallbackInfo ci) {
        LegacyWorldFileFixes.movePerDimensionSavedData(Aeronautics.MOD_ID, List.of(
                CrystallizationWorldSaveData.ID,
                BalloonLevelSavedData.ID
        )).forEach(this::addFileFixOperation);
    }
}
