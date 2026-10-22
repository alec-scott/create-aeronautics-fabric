package dev.simulated_team.simulated.util;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.simulated_team.simulated.content.entities.diagram.render.DiagramFramebuffers;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.*;

import java.util.Collection;

public class SimpleSubLevelGroupRenderer {
    private static final ProjectionMatrixBuffer PROJECTION = new ProjectionMatrixBuffer("Simulated diagram projection");
    private static final Matrix4f TRANSFORM = new Matrix4f();
    public static boolean RENDERING_SIMPLE = false;

    /**
     * @return the chain of sub-levels that should render with a given sub-level into a diagram
     */
    public static Collection<ClientSubLevel> getRenderedChain(final ClientSubLevel subLevel) {
        final ObjectOpenHashSet<ClientSubLevel> visited = new ObjectOpenHashSet<>();
        final ObjectOpenHashSet<ClientSubLevel> frontier = new ObjectOpenHashSet<>();

        frontier.add(subLevel);

        while (!frontier.isEmpty()) {
            final ClientSubLevel current = frontier.iterator().next();

            frontier.remove(current);
            visited.add(current);

            final Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(current.getLevel(), new BoundingBox3d(current.boundingBox()));

            // Intersecting dependencies
            for (final SubLevel neighbor : intersecting) {
                final ClientSubLevel serverNeighbor = (ClientSubLevel) neighbor;

                if (!visited.contains(serverNeighbor)) {
                    frontier.add(serverNeighbor);
                }
            }
        }

        return visited;
    }

    public static void renderChain(final SubLevel subLevel, final DiagramFramebuffers targets, final Matrix4f modelView, final Matrix4f projectionMat, final Vector3d cameraPosition, final Quaternionf orientation, final float partialTicks) {
        final ClientSubLevel clientSubLevel = (ClientSubLevel) subLevel;
        final ClientLevel level = clientSubLevel.getLevel();
        final Collection<ClientSubLevel> subLevels = SimpleSubLevelGroupRenderer.getRenderedChain(clientSubLevel);

        renderGroup(level, subLevels, targets, modelView, projectionMat, cameraPosition, orientation, partialTicks, true);
    }

    public static void renderGroup(final ClientLevel level, final Collection<ClientSubLevel> subLevels, final DiagramFramebuffers targets, final Matrix4f modelView, final Matrix4f projectionMat, final Vector3d cameraPosition, final Quaternionf orientation, final float partialTicks, final boolean renderPlayers) {
        // Finish anything previously being rendered for safety
        final MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        bufferSource.endBatch();

        if (subLevels.isEmpty()) {
            DiagramFramebuffers.unbind();
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        // PORT-NOTE(mc26.1): BlockRenderDispatcher.renderSingleBlock is gone. Tesselate flat (no AO, no culling)
        // like vanilla moving blocks, and force full-bright light as renderSingleBlock(FULL_BRIGHT) did.
        final ModelBlockRenderer blockRenderer = new ModelBlockRenderer(false, false, minecraft.getBlockColors());
        final BlockStateModelSet blockModels = minecraft.getModelManager().getBlockStateModelSet();

        final PoseStack poseStack = new PoseStack();
        poseStack.mulPose(TRANSFORM.set(modelView));
        // PORT-NOTE(mc26.1): Veil's LevelPerspectiveCamera was only used for its rotation, which setup() sets to
        // the given orientation.
        poseStack.mulPose(orientation);

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(PROJECTION.getBuffer(projectionMat), ProjectionType.ORTHOGRAPHIC);

        final Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.identity();
        matrix4fstack.mul(poseStack.last().pose());

        // PORT-NOTE(mc26.1): draws go to the diagram scene target through vanilla's output texture override
        // (replaces binding a Veil AdvancedFbo).
        targets.bindScene();

        try {

            SimpleSubLevelGroupRenderer.RENDERING_SIMPLE = true;

            for (final ClientSubLevel renderedSubLevel : subLevels) {
                final SubLevelRenderData renderData = renderedSubLevel.getRenderData();
                final Vector3d chunkOffset = renderData.getChunkOffset();
                final PoseStack blockPoseStack = new PoseStack();
                blockPoseStack.mulPose(renderData.getTransformation(cameraPosition.x, cameraPosition.y, cameraPosition.z));
                blockPoseStack.translate(chunkOffset.x, chunkOffset.y, chunkOffset.z);

                final BlockQuadOutput output = (x, y, z, quad, instance) -> {
                    blockPoseStack.pushPose();
                    blockPoseStack.translate(x, y, z);
                    instance.setLightCoords(LightCoordsUtil.FULL_BRIGHT);
                    instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
                    bufferSource.getBuffer(movingBlockRenderType(quad.materialInfo().layer()))
                            .putBakedQuad(blockPoseStack.last(), quad, instance);
                    blockPoseStack.popPose();
                };

                final ClientLevel subLevelLevel = renderedSubLevel.getLevel();
                final var bounds = renderedSubLevel.getPlot().getBoundingBox();
                for (final BlockPos blockPos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
                    final BlockState blockState = subLevelLevel.getBlockState(blockPos);
                    if (blockState.isAir() || blockState.getRenderShape() != RenderShape.MODEL) {
                        continue;
                    }

                    blockPoseStack.pushPose();
                    blockPoseStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                    blockRenderer.tesselateBlock(output, 0.0F, 0.0F, 0.0F, subLevelLevel, blockPos, blockState,
                            blockModels.get(blockState), blockState.getSeed(blockPos));
                    blockPoseStack.popPose();
                }
            }

            bufferSource.endBatch();
            SimpleSubLevelGroupRenderer.RENDERING_SIMPLE = false;
        } finally {
            SimpleSubLevelGroupRenderer.RENDERING_SIMPLE = false;

            matrix4fstack.popMatrix();
            RenderSystem.restoreProjectionMatrix();
            DiagramFramebuffers.unbind();
            // PORT-NOTE(mc26.1): the old LightTexture refresh is gone; the lightmap is updated by the game renderer
            // and this pass never writes to it.
        }
    }

    private static RenderType movingBlockRenderType(final ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }
}
