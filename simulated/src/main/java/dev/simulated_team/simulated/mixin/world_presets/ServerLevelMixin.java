package dev.simulated_team.simulated.mixin.world_presets;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.index.SimWorldPresets;
import dev.simulated_team.simulated.mixin_interface.PrimaryLevelDataExtension;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Optional;

/**
 * End Sea worlds start with the dragon already defeated.
 *
 * <p>PORT-NOTE(mc26.1): the dragon fight used to be level data that {@code CreateWorldScreenMixin} set when the
 * world was created. It is now {@link EnderDragonFight} saved data in the End, created on first load, so the
 * defeated state is seeded here instead, and only when the End has no saved fight yet.</p>
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/SavedDataStorage;computeIfAbsent(Lnet/minecraft/world/level/saveddata/SavedDataType;)Lnet/minecraft/world/level/saveddata/SavedData;"))
    private <T extends SavedData> T simulated$seedEndSeaDragonFight(final SavedDataStorage storage, final SavedDataType<T> type,
                                                                     final Operation<T> original) {
        if (type == EnderDragonFight.TYPE && storage.get(type) == null && this.simulated$isEndSea()) {
            final EnderDragonFight fight = new EnderDragonFight(false, true, true, Optional.empty(), 0,
                    Optional.empty(), Optional.empty(), new ObjectArrayList<>(), List.of());
            storage.set(EnderDragonFight.TYPE, fight);
            @SuppressWarnings("unchecked") final T seeded = (T) fight;
            return seeded;
        }
        return original.call(storage, type);
    }

    private boolean simulated$isEndSea() {
        return ((ServerLevel) (Object) this).getServer().getWorldData() instanceof final PrimaryLevelDataExtension worldData
                && SimWorldPresets.END_SEA.id().equals(worldData.getPreset());
    }
}
