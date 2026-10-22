package io.github.fabricators_of_create.porting_lib.models.generators;

import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.DatagenStubs;
import net.minecraft.world.level.block.state.properties.Property;

/** Datagen stub; see {@link io.github.fabricators_of_create.porting_lib.DatagenStubs}. */
public class MultiPartBlockStateBuilder implements IGeneratedBlockState {
    @Override
    public JsonObject toJson() {
        throw DatagenStubs.unsupported();
    }

    public ConfiguredModel.Builder<PartBuilder> part() {
        throw DatagenStubs.unsupported();
    }

    public class PartBuilder {
        @SafeVarargs
        public final <T extends Comparable<T>> PartBuilder condition(final Property<T> property, final T... values) {
            throw DatagenStubs.unsupported();
        }

        public PartBuilder useOr() {
            throw DatagenStubs.unsupported();
        }

        public PartBuilder nestedGroup() {
            throw DatagenStubs.unsupported();
        }

        public PartBuilder endNestedGroup() {
            throw DatagenStubs.unsupported();
        }

        public MultiPartBlockStateBuilder end() {
            throw DatagenStubs.unsupported();
        }
    }
}
