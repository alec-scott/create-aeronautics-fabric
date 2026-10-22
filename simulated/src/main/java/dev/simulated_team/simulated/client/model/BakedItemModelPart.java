package dev.simulated_team.simulated.client.model;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;

import java.util.List;
import java.util.function.Supplier;

public record BakedItemModelPart(
        List<BakedQuad> quads,
        ModelRenderProperties properties,
        Supplier<Vector3fc[]> extents
) {
    public static BakedItemModelPart bake(final ModelBaker baker, final Identifier id) {
        final ResolvedModel model = baker.getModel(id);
        final TextureSlots textures = model.getTopTextureSlots();
        final List<BakedQuad> quads = model.bakeTopGeometry(textures, baker, BlockModelRotation.IDENTITY).getAll();
        return new BakedItemModelPart(
                quads,
                ModelRenderProperties.fromResolvedModel(baker, model, textures),
                Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(quads))
        );
    }

    /**
     * Submits item quads with an explicit render type.
     *
     * <p>PORT-NOTE(mc26.1): item layers no longer carry a render type (each quad picks one from its material), so
     * models that need a specific type, such as the glowing staff core, draw through custom geometry instead of
     * {@code submitItem}. Untinted, matching the empty tint layers these models used before.</p>
     */
    public static void submit(final SubmitNodeCollector collector, final PoseStack poseStack, final RenderType renderType,
                              final List<BakedQuad> quads, final int light, final int overlay,
                              final ItemStackRenderState.FoilType foil) {
        submitQuads(collector, poseStack, renderType, quads, light, overlay);
        if (foil != ItemStackRenderState.FoilType.NONE) {
            submitQuads(collector, poseStack, ItemFeatureRenderer.getFoilRenderType(renderType, true), quads, light, overlay);
        }
    }

    private static void submitQuads(final SubmitNodeCollector collector, final PoseStack poseStack,
                                    final RenderType renderType, final List<BakedQuad> quads, final int light,
                                    final int overlay) {
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            final QuadInstance instance = new QuadInstance();
            instance.setLightCoords(light);
            instance.setOverlayCoords(overlay);
            for (final BakedQuad quad : quads) {
                buffer.putBakedQuad(pose, quad, instance);
            }
        });
    }
}
