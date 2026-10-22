package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.platform.SableLoaderPlatform;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;


@Mixin(value = LevelRenderer.class, priority = 1002)
public abstract class LevelRendererMixin {

    @Shadow
    private @Nullable ClientLevel level;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private @Nullable SectionRenderDispatcher sectionRenderDispatcher;

    @Inject(method = "compileSections", at = @At("TAIL"))
    private void sable$compileSections(final Camera camera, final CallbackInfo ci) {
        final Iterable<ClientSubLevel> sublevels = ((ClientSubLevelContainer) ((SubLevelContainerHolder) this.level).sable$getPlotContainer()).getAllSubLevels();
        final RenderRegionCache renderRegionCache = new RenderRegionCache();
        final PrioritizeChunkUpdates chunkUpdates = Minecraft.getInstance().options.prioritizeChunkUpdates().get();

        for (final ClientSubLevel sublevel : sublevels) {
            sublevel.getRenderData().compileSections(chunkUpdates, renderRegionCache, camera);
        }
    }

    @Inject(method = "cullTerrain", at = @At("HEAD"))
    public void sable$cull(final Camera camera, final Frustum frustum, final boolean spectator, final CallbackInfo ci) {
        final SubLevelRenderDispatcher dispatcher = SubLevelRenderDispatcher.get();
        dispatcher.preRenderChunks(camera);

        final ProfilerFiller profiler = net.minecraft.util.profiling.Profiler.get();
        profiler.push("sub_level_section_occlusion_graph");

        final Iterable<ClientSubLevel> sublevels = ((ClientSubLevelContainer) ((SubLevelContainerHolder) this.level).sable$getPlotContainer()).getAllSubLevels();
        final Vec3 cameraPosition = camera.position();
        dispatcher.updateCulling(sublevels, cameraPosition.x, cameraPosition.y, cameraPosition.z, frustum, spectator);

        profiler.pop();
    }

    // PORT-NOTE(mc26.1): prepareChunkRenders lost its camera parameters, and section geometry now lives in the
    // dispatcher's pooled buffers. Each draw is addressed by a slice offset instead of a per-section SectionBuffers.
    @Inject(method = "prepareChunkRenders", at = @At("RETURN"), cancellable = true)
    private void sable$appendSubLevelSections(final Matrix4fc baseModelView, final CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        // Sodium cancels the vanilla renderGroup implementation. Its dedicated
        // mixin renders sub-level meshes after each Sodium terrain group instead.
        if (SableLoaderPlatform.INSTANCE.isModLoaded("sodium")) {
            return;
        }

        if (this.level == null) {
            return;
        }

        final ClientSubLevelContainer container = SubLevelContainer.getContainer(this.level);
        if (container == null || this.sectionRenderDispatcher == null) {
            return;
        }

        final Vec3 cameraPosition = this.minecraft.gameRenderer.getMainCamera().position();
        final double cameraX = cameraPosition.x;
        final double cameraY = cameraPosition.y;
        final double cameraZ = cameraPosition.z;

        final ChunkSectionsToRender original = cir.getReturnValue();
        final EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> drawGroupsPerLayer = new EnumMap<>(ChunkSectionLayer.class);
        for (final ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            final Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>> groups = new Int2ObjectOpenHashMap<>();
            final Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>> existingGroups = original.drawGroupsPerLayer().get(layer);
            if (existingGroups != null) {
                existingGroups.forEach((key, draws) -> groups.put((int) key, new ArrayList<>(draws)));
            }
            drawGroupsPerLayer.put(layer, groups);
        }

        final int originalTransformCount = original.chunkSectionInfos().length;
        final List<DynamicUniforms.ChunkSectionInfo> transforms = new ArrayList<>();
        int maxIndicesRequired = original.maxIndicesRequired();

        this.sectionRenderDispatcher.lock();
        try {
            for (final ClientSubLevel subLevel : container.getAllSubLevels()) {
                if (!(subLevel.getRenderData() instanceof final VanillaChunkedSubLevelRenderData renderData)) {
                    continue;
                }

                final Matrix4f modelView = new Matrix4f(baseModelView).mul(renderData.getTransformation(cameraX, cameraY, cameraZ));
                final Vector3dc rotationPoint = subLevel.renderPose().rotationPoint();

                for (final SectionRenderDispatcher.RenderSection section : renderData.allRenderSections()) {
                    final SectionMesh mesh = section.getSectionMesh();
                    final BlockPos origin = section.getRenderOrigin();
                    final BlockPos worldOrigin = BlockPos.containing(subLevel.renderPose()
                            .transformPosition(Vec3.atLowerCornerOf(origin)));
                    // Terrain now subtracts the camera through Globals before applying ModelViewMat.
                    // Cancel that offset in local coordinates; keep ChunkPosition near the physical
                    // section origin so vanilla fog measures distance in world space rather than plot space.
                    final Matrix4f sectionModelView = new Matrix4f(modelView).translate(
                            (float) (origin.getX() - rotationPoint.x() + cameraX - worldOrigin.getX()),
                            (float) (origin.getY() - rotationPoint.y() + cameraY - worldOrigin.getY()),
                            (float) (origin.getZ() - rotationPoint.z() + cameraZ - worldOrigin.getZ()));

                    int transformIndex = -1;
                    for (final ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                        final SectionMesh.SectionDraw draw = mesh.getSectionDraw(layer);
                        final SectionRenderDispatcher.RenderSectionBufferSlice slice = this.sectionRenderDispatcher.getRenderSectionSlice(mesh, layer);
                        if (draw == null || slice == null || draw.hasCustomIndexBuffer() && slice.indexBuffer() == null) {
                            continue;
                        }

                        if (transformIndex == -1) {
                            transformIndex = originalTransformCount + transforms.size();
                            transforms.add(new DynamicUniforms.ChunkSectionInfo(
                                    sectionModelView,
                                    worldOrigin.getX(), worldOrigin.getY(), worldOrigin.getZ(), 1.0F,
                                    original.textureView().getWidth(0),
                                    original.textureView().getHeight(0)
                            ));
                        }

                        // Group by bound buffers the same way vanilla does, so draws sharing a pooled buffer batch together.
                        final GpuBuffer vertexBuffer = slice.vertexBuffer();
                        int groupKey = 173;
                        if (layer != ChunkSectionLayer.TRANSLUCENT) {
                            groupKey = 31 * groupKey + vertexBuffer.hashCode();
                        }

                        final GpuBuffer indexBuffer;
                        final VertexFormat.IndexType indexType;
                        int firstIndex = 0;
                        if (!draw.hasCustomIndexBuffer()) {
                            maxIndicesRequired = Math.max(maxIndicesRequired, draw.indexCount());
                            indexBuffer = null;
                            indexType = null;
                        } else {
                            indexBuffer = slice.indexBuffer();
                            indexType = draw.indexType();
                            if (layer != ChunkSectionLayer.TRANSLUCENT) {
                                groupKey = 31 * groupKey + indexBuffer.hashCode();
                                groupKey = 31 * groupKey + indexType.hashCode();
                            }
                            firstIndex = (int) (slice.indexBufferOffset() / indexType.bytes);
                        }

                        final int baseVertex = (int) (slice.vertexBufferOffset() / layer.pipeline().getVertexFormat().getVertexSize());
                        final int sectionTransformIndex = transformIndex;
                        drawGroupsPerLayer.get(layer).computeIfAbsent(groupKey, key -> new ArrayList<>()).add(new RenderPass.Draw<>(
                                0,
                                vertexBuffer,
                                indexBuffer,
                                indexType,
                                firstIndex,
                                draw.indexCount(),
                                baseVertex,
                                (chunkSectionInfos, uploader) -> uploader.upload("ChunkSection", chunkSectionInfos[sectionTransformIndex])
                        ));
                    }
                }
            }
        } finally {
            this.sectionRenderDispatcher.unlock();
        }

        if (transforms.isEmpty()) {
            return;
        }

        final GpuBufferSlice[] additionalTransforms = RenderSystem.getDynamicUniforms().writeChunkSections(transforms.toArray(DynamicUniforms.ChunkSectionInfo[]::new));
        final GpuBufferSlice[] combinedTransforms = Arrays.copyOf(original.chunkSectionInfos(), originalTransformCount + additionalTransforms.length);
        System.arraycopy(additionalTransforms, 0, combinedTransforms, originalTransformCount, additionalTransforms.length);
        cir.setReturnValue(new ChunkSectionsToRender(original.textureView(), drawGroupsPerLayer, maxIndicesRequired, combinedTransforms));
    }

    @Inject(method = "isSectionCompiledAndVisible", at = @At("HEAD"), cancellable = true)
    private void sable$isSectionCompiled(final BlockPos blockPos, final CallbackInfoReturnable<Boolean> cir) {
        final ClientSubLevelContainer container = SubLevelContainer.getContainer(this.level);

        if (container == null) {
            return;
        }

        if (container.inBounds(blockPos)) {
            final ClientSubLevel subLevel = (ClientSubLevel) Sable.HELPER.getContaining(this.level, blockPos);

            if (subLevel == null) {
                cir.setReturnValue(false);
            } else {
                final SubLevelRenderData renderData = subLevel.getRenderData();
                final SectionPos sectionPos = SectionPos.of(blockPos);
                cir.setReturnValue(renderData.isSectionCompiled(sectionPos.x(), sectionPos.y(), sectionPos.z()));
            }
        }
    }

}
