package io.github.fabricators_of_create.porting_lib.models.generators;

import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.DatagenStubs;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.function.Function;

/** Datagen stub; see {@link io.github.fabricators_of_create.porting_lib.DatagenStubs}. */
public class VariantBlockStateBuilder implements IGeneratedBlockState {
    @Override
    public JsonObject toJson() {
        throw DatagenStubs.unsupported();
    }

    public Map<PartialBlockstate, BlockStateProvider.ConfiguredModelList> getModels() {
        throw DatagenStubs.unsupported();
    }

    public VariantBlockStateBuilder forAllStates(final Function<BlockState, ConfiguredModel[]> mapper) {
        throw DatagenStubs.unsupported();
    }

    public VariantBlockStateBuilder forAllStatesExcept(final Function<BlockState, ConfiguredModel[]> mapper, final Property<?>... ignored) {
        throw DatagenStubs.unsupported();
    }

    public PartialBlockstate partialState() {
        throw DatagenStubs.unsupported();
    }

    public VariantBlockStateBuilder addModels(final PartialBlockstate state, final ConfiguredModel... models) {
        throw DatagenStubs.unsupported();
    }

    public static class PartialBlockstate {
        public <T extends Comparable<T>> PartialBlockstate with(final Property<T> property, final T value) {
            throw DatagenStubs.unsupported();
        }

        public ConfiguredModel.Builder<VariantBlockStateBuilder> modelForState() {
            throw DatagenStubs.unsupported();
        }

        public PartialBlockstate addModels(final ConfiguredModel... models) {
            throw DatagenStubs.unsupported();
        }

        public VariantBlockStateBuilder setModels(final ConfiguredModel... models) {
            throw DatagenStubs.unsupported();
        }

        public PartialBlockstate partialState() {
            throw DatagenStubs.unsupported();
        }
    }
}
