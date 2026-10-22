package io.github.fabricators_of_create.porting_lib.models.generators;

import io.github.fabricators_of_create.porting_lib.DatagenStubs;

import java.util.function.Function;

/** Datagen stub; see {@link io.github.fabricators_of_create.porting_lib.DatagenStubs}. */
public final class ConfiguredModel {
    public static final int DEFAULT_WEIGHT = 1;

    public final ModelFile model;
    public final int rotationX;
    public final int rotationY;
    public final boolean uvLock;
    public final int weight;

    public ConfiguredModel(final ModelFile model, final int rotationX, final int rotationY, final boolean uvLock, final int weight) {
        this.model = model;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.uvLock = uvLock;
        this.weight = weight;
    }

    public ConfiguredModel(final ModelFile model, final int rotationX, final int rotationY, final boolean uvLock) {
        this(model, rotationX, rotationY, uvLock, DEFAULT_WEIGHT);
    }

    public ConfiguredModel(final ModelFile model) {
        this(model, 0, 0, false);
    }

    public static ConfiguredModel[] allYRotations(final ModelFile model, final int x, final boolean uvlock) {
        throw DatagenStubs.unsupported();
    }

    public static ConfiguredModel[] allRotations(final ModelFile model, final boolean uvlock) {
        throw DatagenStubs.unsupported();
    }

    public static Builder<?> builder() {
        return new Builder<>(null);
    }

    public static final class Builder<T> {
        private final Function<ConfiguredModel[], T> callback;

        Builder(final Function<ConfiguredModel[], T> callback) {
            this.callback = callback;
        }

        public Builder<T> modelFile(final ModelFile model) {
            throw DatagenStubs.unsupported();
        }

        public Builder<T> rotationX(final int value) {
            throw DatagenStubs.unsupported();
        }

        public Builder<T> rotationY(final int value) {
            throw DatagenStubs.unsupported();
        }

        public Builder<T> uvLock(final boolean value) {
            throw DatagenStubs.unsupported();
        }

        public Builder<T> weight(final int value) {
            throw DatagenStubs.unsupported();
        }

        public Builder<T> nextModel() {
            throw DatagenStubs.unsupported();
        }

        public ConfiguredModel[] build() {
            throw DatagenStubs.unsupported();
        }

        public T addModel() {
            throw DatagenStubs.unsupported();
        }
    }
}
