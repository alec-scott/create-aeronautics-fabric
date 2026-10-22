package io.github.fabricators_of_create.porting_lib.models.generators;

import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import net.minecraft.resources.Identifier;

/** Datagen stub; see {@link io.github.fabricators_of_create.porting_lib.DatagenStubs}. */
public abstract class ModelFile {
    protected Identifier location;

    protected ModelFile(final Identifier location) {
        this.location = location;
    }

    protected abstract boolean exists();

    public Identifier getLocation() {
        return this.location;
    }

    public Identifier getUncheckedLocation() {
        return this.location;
    }

    public void assertExistence() {
    }

    public static class UncheckedModelFile extends ModelFile {
        public UncheckedModelFile(final String location) {
            this(Identifier.parse(location));
        }

        public UncheckedModelFile(final Identifier location) {
            super(location);
        }

        @Override
        protected boolean exists() {
            return true;
        }
    }

    public static class ExistingModelFile extends ModelFile {
        public ExistingModelFile(final Identifier location, final ExistingFileHelper existingHelper) {
            super(location);
        }

        @Override
        protected boolean exists() {
            return true;
        }
    }
}
