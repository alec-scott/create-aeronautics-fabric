package io.github.fabricators_of_create.porting_lib.models.generators;

import com.google.gson.JsonElement;
import io.github.fabricators_of_create.porting_lib.DatagenStubs;
import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Datagen stub; see {@link io.github.fabricators_of_create.porting_lib.DatagenStubs}. */
public abstract class BlockStateProvider implements DataProvider {
    protected final Map<Block, IGeneratedBlockState> registeredBlocks = new LinkedHashMap<>();

    private final String modid;
    private final BlockModelProvider blockModels;
    private final ItemModelProvider itemModels;

    public BlockStateProvider(final PackOutput output, final String modid, final ExistingFileHelper exFileHelper) {
        this.modid = modid;
        this.blockModels = new BlockModelProvider(output, modid, exFileHelper) {
            @Override
            protected void registerModels() {
            }
        };
        this.itemModels = new ItemModelProvider(output, modid, exFileHelper) {
            @Override
            protected void registerModels() {
            }
        };
    }

    protected abstract void registerStatesAndModels();

    public static class ConfiguredModelList {
        public JsonElement toJSON() {
            throw DatagenStubs.unsupported();
        }
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        throw DatagenStubs.unsupported();
    }

    public BlockModelProvider models() {
        return this.blockModels;
    }

    public ItemModelProvider itemModels() {
        return this.itemModels;
    }

    public Identifier modLoc(final String name) {
        return Identifier.fromNamespaceAndPath(this.modid, name);
    }

    public Identifier mcLoc(final String name) {
        return Identifier.withDefaultNamespace(name);
    }

    public Identifier blockTexture(final Block block) {
        throw DatagenStubs.unsupported();
    }

    public ModelFile cubeAll(final Block block) {
        throw DatagenStubs.unsupported();
    }

    public VariantBlockStateBuilder getVariantBuilder(final Block block) {
        throw DatagenStubs.unsupported();
    }

    public MultiPartBlockStateBuilder getMultipartBuilder(final Block block) {
        throw DatagenStubs.unsupported();
    }

    public void simpleBlock(final Block block) {
        throw DatagenStubs.unsupported();
    }

    public void simpleBlock(final Block block, final ModelFile model) {
        throw DatagenStubs.unsupported();
    }

    public void simpleBlock(final Block block, final ConfiguredModel... models) {
        throw DatagenStubs.unsupported();
    }

    public void simpleBlock(final Block block, final Function<ModelFile, ConfiguredModel[]> expander) {
        throw DatagenStubs.unsupported();
    }

    public void simpleBlockItem(final Block block, final ModelFile model) {
        throw DatagenStubs.unsupported();
    }

    public void simpleBlockWithItem(final Block block, final ModelFile model) {
        throw DatagenStubs.unsupported();
    }

    public void axisBlock(final RotatedPillarBlock block, final ModelFile vertical, final ModelFile horizontal) {
        throw DatagenStubs.unsupported();
    }

    public void horizontalBlock(final Block block, final ModelFile model) {
        throw DatagenStubs.unsupported();
    }

    public void horizontalBlock(final Block block, final ModelFile model, final int angleOffset) {
        throw DatagenStubs.unsupported();
    }

    public void horizontalBlock(final Block block, final Function<BlockState, ModelFile> modelFunc) {
        throw DatagenStubs.unsupported();
    }

    public void horizontalBlock(final Block block, final Function<BlockState, ModelFile> modelFunc, final int angleOffset) {
        throw DatagenStubs.unsupported();
    }

    public void horizontalFaceBlock(final Block block, final ModelFile model) {
        throw DatagenStubs.unsupported();
    }

    public void horizontalFaceBlock(final Block block, final ModelFile model, final int angleOffset) {
        throw DatagenStubs.unsupported();
    }

    public void horizontalFaceBlock(final Block block, final Function<BlockState, ModelFile> modelFunc) {
        throw DatagenStubs.unsupported();
    }

    public void horizontalFaceBlock(final Block block, final Function<BlockState, ModelFile> modelFunc, final int angleOffset) {
        throw DatagenStubs.unsupported();
    }

    public void directionalBlock(final Block block, final ModelFile model) {
        throw DatagenStubs.unsupported();
    }

    public void directionalBlock(final Block block, final ModelFile model, final int angleOffset) {
        throw DatagenStubs.unsupported();
    }

    public void directionalBlock(final Block block, final Function<BlockState, ModelFile> modelFunc) {
        throw DatagenStubs.unsupported();
    }

    public void directionalBlock(final Block block, final Function<BlockState, ModelFile> modelFunc, final int angleOffset) {
        throw DatagenStubs.unsupported();
    }
}
