package io.github.fabricators_of_create.porting_lib.models.generators;

import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.DatagenStubs;
import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import net.minecraft.resources.Identifier;

/** Datagen stub; see {@link io.github.fabricators_of_create.porting_lib.DatagenStubs}. */
public class ModelBuilder<T extends ModelBuilder<T>> extends ModelFile {
    protected ModelBuilder(final Identifier outputLocation, final ExistingFileHelper existingFileHelper) {
        super(outputLocation);
    }

    @Override
    protected boolean exists() {
        return true;
    }

    public T parent(final ModelFile parent) {
        throw DatagenStubs.unsupported();
    }

    public T texture(final String key, final String texture) {
        throw DatagenStubs.unsupported();
    }

    public T texture(final String key, final Identifier texture) {
        throw DatagenStubs.unsupported();
    }

    public T renderType(final String renderType) {
        throw DatagenStubs.unsupported();
    }

    public T renderType(final Identifier renderType) {
        throw DatagenStubs.unsupported();
    }

    public T ao(final boolean ao) {
        throw DatagenStubs.unsupported();
    }

    public T guiLight(final Object light) {
        throw DatagenStubs.unsupported();
    }

    public JsonObject toJson() {
        throw DatagenStubs.unsupported();
    }
}
