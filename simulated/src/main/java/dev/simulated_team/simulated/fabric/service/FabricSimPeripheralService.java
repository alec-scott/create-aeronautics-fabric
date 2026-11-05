package dev.simulated_team.simulated.fabric.service;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredElementLookup;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * Only loaded through ComputerCraftPeripherals, so CC classes are touched only when CC is installed.
 * The block entity types are registered after the compat services run (Registrate registers at the end of init),
 * so these are fallback providers that resolve the type at lookup time.
 */
public final class FabricSimPeripheralService implements SimPeripheralService {
    @Override
    public <T extends BlockEntity> void addPeripheral(final Supplier<BlockEntityType<T>> typeSupplier,
                                                      final CapabilityGetter<T, IPeripheral> getter) {
        register(PeripheralLookup.get(), typeSupplier, getter);
    }

    @Override
    public <T extends BlockEntity> void addWired(final Supplier<BlockEntityType<T>> typeSupplier,
                                                 final CapabilityGetter<T, WiredElement> getter) {
        register(WiredElementLookup.get(), typeSupplier, getter);
    }

    @SuppressWarnings("unchecked")
    private static <A, T extends BlockEntity> void register(final BlockApiLookup<A, Direction> lookup,
                                                            final Supplier<BlockEntityType<T>> typeSupplier,
                                                            final CapabilityGetter<T, A> getter) {
        lookup.registerFallback((level, pos, state, blockEntity, direction) -> {
            if (blockEntity == null || blockEntity.getType() != typeSupplier.get()) {
                return null;
            }
            return getter.get((T) blockEntity, direction);
        });
    }
}
