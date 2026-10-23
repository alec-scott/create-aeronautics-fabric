package dev.ryanhcode.sable.mixin.sublevel_render.impl.sodium;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.api.client.SubLevelBlockEntityRenderRegistry;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.sublevel_render.SubLevelBlockEntityRenderExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.render.SubLevelImmediateMesh;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.SubLevelLightVertexConsumerProvider;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.SortedSet;

/**
 * Bridges Sable sub-levels into Sodium's terrain render phases.
 *
 * <p>The Iris/Sodium pipeline requires its own extended vertex format.
 * Sable's vanilla compiled section buffers are accepted by the GPU but discarded
 * by that shader pipeline. Render the sub-level blocks through Minecraft's
 * immediate block-model path instead; Iris decorates this path with the active
 * shader-pack vertex format just as it does for moving blocks.</p>
 */
@Mixin(value = SodiumWorldRenderer.class, remap = false)
public abstract class SodiumWorldRendererMixin {

    @Shadow
    private @Nullable ClientLevel level;

    @Unique
    private final Map<ClientSubLevel, SubLevelImmediateMesh> sable$meshes = new IdentityHashMap<>();

    // Sodium cancels LevelRenderer#extractVisibleBlockEntities at HEAD, which
    // bypasses Sable's vanilla RETURN hook. Append transformed plot block
    // entities after Sodium has extracted the ordinary world block entities.
    @Inject(method = "extractBlockEntities", at = @At("TAIL"))
    private void sable$extractSubLevelBlockEntities(
            final Camera camera,
            final float partialTick,
            final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress,
            final LevelRenderState levelRenderState,
            final CallbackInfo ci
    ) {
        final LevelRenderer levelRenderer = Minecraft.getInstance().levelRenderer;
        ((SubLevelBlockEntityRenderExtension) levelRenderer)
                .sable$extractSubLevelBlockEntities(camera, partialTick, levelRenderState);
    }

    // PORT-NOTE(mc26.1): Sodium 0.9 passes a Matrix4f here instead of ChunkRenderMatrices.
    @Inject(method = "setupTerrain", at = @At("TAIL"))
    private void sable$compileSubLevelSections(
            final Camera camera,
            final Viewport viewport,
            final FogParameters fogParameters,
            final boolean spectator,
            final boolean updateImmediately,
            final Matrix4f projectionMatrix,
            final CallbackInfo ci
    ) {
        if (this.level == null) {
            return;
        }

        final ClientSubLevelContainer container = SubLevelContainer.getContainer(this.level);
        if (container == null) {
            return;
        }

        final PrioritizeChunkUpdates chunkUpdates = Minecraft.getInstance().options.prioritizeChunkUpdates().get();
        final RenderRegionCache renderRegionCache = new RenderRegionCache();
        for (final ClientSubLevel subLevel : container.getAllSubLevels()) {
            subLevel.getRenderData().compileSections(chunkUpdates, renderRegionCache, camera);
        }
    }

    @Inject(method = "scheduleRebuildForChunk", at = @At("HEAD"), cancellable = true)
    private void sable$scheduleSubLevelRebuild(
            final int sectionX,
            final int sectionY,
            final int sectionZ,
            final boolean playerChanged,
            final CallbackInfo ci
    ) {
        if (this.level == null) {
            return;
        }

        final ClientSubLevelContainer container = SubLevelContainer.getContainer(this.level);
        if (container == null) {
            return;
        }

        final LevelPlot plot = container.getPlot(sectionX, sectionZ);
        if (plot == null) {
            return;
        }

        final ClientSubLevel subLevel = (ClientSubLevel) plot.getSubLevel();
        subLevel.getRenderData().setDirty(sectionX, sectionY, sectionZ, playerChanged);
        // Blocks or plot light changed; re-tesselate the immediate mesh on the next draw.
        this.sable$meshes.remove(subLevel);
        ci.cancel();
    }

    @Inject(method = "drawChunkLayer", at = @At("TAIL"))
    private void sable$drawSubLevelSections(
            final ChunkSectionLayerGroup group,
            final ChunkRenderMatrices matrices,
            final double cameraX,
            final double cameraY,
            final double cameraZ,
            final GpuSampler terrainSampler,
            final CallbackInfo ci
    ) {
        if (this.level == null || group != ChunkSectionLayerGroup.OPAQUE) {
            return;
        }

        final ClientSubLevelContainer container = SubLevelContainer.getContainer(this.level);
        if (container == null) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        // PORT-NOTE(mc26.1): BlockRenderDispatcher and the Fabric Renderer.render entry point are gone. Tesselate
        // like vanilla's section compiler (culled, smooth-lit) and emit quads the way moving blocks do.
        final ModelBlockRenderer blockRenderer = new ModelBlockRenderer(
                minecraft.options.ambientOcclusion().get(), true, minecraft.getBlockColors());
        final BlockStateModelSet blockModels = minecraft.getModelManager().getBlockStateModelSet();
        final boolean cutoutLeaves = minecraft.options.cutoutLeaves().get();
        final float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        // This injection runs from Sodium's opaque terrain draw, just before
        // LevelRenderer normally selects the world-lighting UBO. The immediate
        // block model pipeline consumes that UBO for directional face shading,
        // so bind it here as well instead of inheriting stale GUI/item lights.
        minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);

        final boolean ambientOcclusion = minecraft.options.ambientOcclusion().get();
        final ReferenceOpenHashSet<ClientSubLevel> liveSubLevels = new ReferenceOpenHashSet<>();
        for (final ClientSubLevel subLevel : container.getAllSubLevels()) {
            liveSubLevels.add(subLevel);
            final SubLevelLightVertexConsumerProvider blockBuffers = new SubLevelLightVertexConsumerProvider(
                    this.level,
                    subLevel,
                    cameraX,
                    cameraY,
                    cameraZ,
                    bufferSource
            );
            final SubLevelRenderData renderData = subLevel.getRenderData();
            final Pose3dc renderPose = subLevel.renderPose(partialTick);
            final Vector3dc rotationPoint = renderPose.rotationPoint();
            final Matrix4f transformation = renderData.getTransformation(cameraX, cameraY, cameraZ);
            final BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();

            SubLevelImmediateMesh mesh = this.sable$meshes.get(subLevel);
            if (mesh == null || !mesh.matches(bounds, ambientOcclusion, cutoutLeaves, blockModels)) {
                mesh = this.sable$buildMesh(bounds, blockRenderer, blockModels, ambientOcclusion, cutoutLeaves);
                this.sable$meshes.put(subLevel, mesh);
            }
            mesh.replay(transformation, rotationPoint, blockBuffers);

            final PoseStack poseStack = new PoseStack();
            poseStack.mulPose(transformation);
            for (final PlotChunkHolder holder : subLevel.getPlot().getLoadedChunks()) {
                for (final BlockEntity blockEntity : holder.getChunk().getBlockEntities().values()) {
                    if (blockEntity.isRemoved()) {
                        continue;
                    }

                    final BlockPos blockPos = blockEntity.getBlockPos();
                    poseStack.pushPose();
                    // Subtract in double precision before storing the translation in
                    // the float pose matrix; plot coordinates are tens of millions of
                    // blocks away and two separate translations lose local precision.
                    poseStack.translate(
                            blockPos.getX() - rotationPoint.x(),
                            blockPos.getY() - rotationPoint.y(),
                            blockPos.getZ() - rotationPoint.z()
                    );
                    final int plotLight = LevelRenderer.getLightCoords(this.level, blockPos);
                    SubLevelBlockEntityRenderRegistry.render(
                            blockEntity,
                            partialTick,
                            poseStack,
                            blockBuffers,
                            plotLight,
                            OverlayTexture.NO_OVERLAY
                    );
                    poseStack.popPose();
                }
            }
        }
        this.sable$meshes.keySet().retainAll(liveSubLevels);

        bufferSource.endBatch();
    }

    /**
     * Tesselates every block model in the plot once, relative to the plot's minimum corner.
     */
    @Unique
    private SubLevelImmediateMesh sable$buildMesh(
            final BoundingBox3ic bounds,
            final ModelBlockRenderer blockRenderer,
            final BlockStateModelSet blockModels,
            final boolean ambientOcclusion,
            final boolean cutoutLeaves
    ) {
        final SubLevelImmediateMesh mesh = new SubLevelImmediateMesh(bounds, ambientOcclusion, cutoutLeaves, blockModels);
        final BlockPos origin = mesh.origin();
        final PoseStack poseStack = new PoseStack();
        final BlockQuadOutput quadOutput = (x, y, z, quad, instance) ->
                putBakedQuad(poseStack, mesh.recorder(quad.materialInfo().layer()), x, y, z, quad, instance);
        final BlockQuadOutput solidQuadOutput = (x, y, z, quad, instance) ->
                putBakedQuad(poseStack, mesh.recorder(ChunkSectionLayer.SOLID), x, y, z, quad, instance);

        for (final BlockPos blockPos : BlockPos.betweenClosed(
                bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            final BlockState blockState = this.level.getBlockState(blockPos);
            if (blockState.isAir() || blockState.getRenderShape() != RenderShape.MODEL) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(
                    blockPos.getX() - origin.getX(),
                    blockPos.getY() - origin.getY(),
                    blockPos.getZ() - origin.getZ()
            );
            // The terrain-like renderer computes AO and packed light for
            // every vertex. renderSingleBlock uses one light value for the
            // entire model, which makes nearby light sources visibly step
            // from one sub-level block to the next.
            blockRenderer.tesselateBlock(
                    ModelBlockRenderer.forceOpaque(cutoutLeaves, blockState) ? solidQuadOutput : quadOutput,
                    0.0F,
                    0.0F,
                    0.0F,
                    this.level,
                    blockPos,
                    blockState,
                    blockModels.get(blockState),
                    blockState.getSeed(blockPos)
            );
            poseStack.popPose();
        }
        return mesh;
    }

    private static void putBakedQuad(
            final PoseStack poseStack,
            final VertexConsumer buffer,
            final float x,
            final float y,
            final float z,
            final BakedQuad quad,
            final QuadInstance instance
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        buffer.putBakedQuad(poseStack.last(), quad, instance);
        poseStack.popPose();
    }

}
