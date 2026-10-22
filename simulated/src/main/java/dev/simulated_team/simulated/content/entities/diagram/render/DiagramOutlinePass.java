package dev.simulated_team.simulated.content.entities.diagram.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * The diagram's stylised look: palette-dithered shading, depth-edge outlines and the faded border.
 *
 * <p>A port of the Veil post pipeline {@code simulated:diagram} (one blit stage running
 * {@code contraption_diagram/outline_diagram}) to a vanilla 26.1 render pass. The fragment shader is unchanged apart
 * from moving its loose uniforms into the {@code DiagramConfig} uniform block.</p>
 */
public final class DiagramOutlinePass {
    private static final Identifier PALETTE = Simulated.path("textures/effects/diagram_palette.png");
    private static final Identifier DITHER = Simulated.path("textures/effects/dither.png");

    // std140: two vec4 (32 bytes), one vec2 (8) and two floats (8).
    private static final int CONFIG_SIZE = 48;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                    .withLocation(Simulated.path("pipeline/diagram_outline"))
                    .withVertexShader("core/screenquad")
                    .withFragmentShader(Simulated.path("core/diagram_outline"))
                    .withSampler("DiffuseSampler0")
                    .withSampler("DiffuseDepthSampler")
                    .withSampler("Palette")
                    .withSampler("Dither")
                    .withUniform("DiagramConfig", UniformType.UNIFORM_BUFFER)
                    .withColorTargetState(ColorTargetState.DEFAULT)
                    .withDepthStencilState(Optional.empty())
                    .build());

    private static MappableRingBuffer config;

    private DiagramOutlinePass() {
    }

    public static void run(final DiagramFramebuffers targets, final float inWidth, final float inHeight,
                           final float paletteOffset, final float fadeScale, final int lineColor,
                           final int lineShadowColor) {
        if (config == null) {
            // Same usage flags vanilla uses for its post-pass uniform ring buffer.
            config = new MappableRingBuffer(() -> "Simulated diagram outline config", 130, CONFIG_SIZE);
        }

        final CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView view = encoder.mapBuffer(config.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(view.data())
                    .putVec4(red(lineColor), green(lineColor), blue(lineColor), 1.0F)
                    .putVec4(red(lineShadowColor), green(lineShadowColor), blue(lineShadowColor), 1.0F)
                    .putVec2(inWidth, inHeight)
                    .putFloat(paletteOffset)
                    .putFloat(fadeScale);
        }

        final TextureManager textures = Minecraft.getInstance().getTextureManager();
        final GpuTextureView palette = textures.getTexture(PALETTE).getTextureView();
        final GpuTextureView dither = textures.getTexture(DITHER).getTextureView();
        final var samplers = RenderSystem.getSamplerCache();

        try (RenderPass pass = encoder.createRenderPass(() -> "Simulated diagram outline",
                targets.output().getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DiagramConfig", config.currentBuffer());
            pass.bindTexture("DiffuseSampler0", targets.scene().getColorTextureView(), samplers.getClampToEdge(FilterMode.NEAREST));
            pass.bindTexture("DiffuseDepthSampler", targets.scene().getDepthTextureView(), samplers.getClampToEdge(FilterMode.NEAREST));
            pass.bindTexture("Palette", palette, samplers.getClampToEdge(FilterMode.NEAREST));
            // The dither pattern tiles across the diagram.
            pass.bindTexture("Dither", dither,
                    samplers.getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.NEAREST, false));
            pass.draw(0, 3);
        }

        config.rotate();
    }

    private static float red(final int rgb) {
        return (rgb >> 16 & 0xFF) / 255.0F;
    }

    private static float green(final int rgb) {
        return (rgb >> 8 & 0xFF) / 255.0F;
    }

    private static float blue(final int rgb) {
        return (rgb & 0xFF) / 255.0F;
    }
}
