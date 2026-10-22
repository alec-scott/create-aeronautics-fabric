package io.github.fabricators_of_create.porting_lib.models.generators;

import io.github.fabricators_of_create.porting_lib.DatagenStubs;
import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/** Datagen stub; see {@link io.github.fabricators_of_create.porting_lib.DatagenStubs}. */
public abstract class ModelProvider<T extends ModelBuilder<T>> implements DataProvider {
    public static final String BLOCK_FOLDER = "block";
    public static final String ITEM_FOLDER = "item";

    protected final String modid;
    protected final String folder;
    public final ExistingFileHelper existingFileHelper;
    protected final Map<Identifier, T> generatedModels = new HashMap<>();

    protected ModelProvider(final PackOutput output, final String modid, final String folder,
                            final BiFunction<Identifier, ExistingFileHelper, T> factory,
                            final ExistingFileHelper existingFileHelper) {
        this.modid = modid;
        this.folder = folder;
        this.existingFileHelper = existingFileHelper;
    }

    protected abstract void registerModels();

    public T getBuilder(final String path) {
        throw DatagenStubs.unsupported();
    }

    public Identifier modLoc(final String name) {
        return Identifier.fromNamespaceAndPath(this.modid, name);
    }

    public Identifier mcLoc(final String name) {
        return Identifier.withDefaultNamespace(name);
    }

    public T withExistingParent(final String name, final String parent) {
        throw DatagenStubs.unsupported();
    }

    public T withExistingParent(final String name, final Identifier parent) {
        throw DatagenStubs.unsupported();
    }

    public T cubeAll(final String name, final Identifier texture) {
        throw DatagenStubs.unsupported();
    }

    public T singleTexture(final String name, final Identifier parent, final Identifier texture) {
        throw DatagenStubs.unsupported();
    }

    public T singleTexture(final String name, final Identifier parent, final String textureKey, final Identifier texture) {
        throw DatagenStubs.unsupported();
    }

    public ModelFile.ExistingModelFile getExistingFile(final Identifier path) {
        throw DatagenStubs.unsupported();
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        throw DatagenStubs.unsupported();
    }

    @Override
    public String getName() {
        return "Models (stubbed)";
    }
}
