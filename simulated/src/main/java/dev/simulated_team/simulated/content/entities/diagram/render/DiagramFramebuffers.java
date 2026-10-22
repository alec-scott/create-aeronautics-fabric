package dev.simulated_team.simulated.content.entities.diagram.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Off-screen targets for one rendered contraption diagram: the sub-level scene (colour and depth) and the output
 * of the outline pass that the GUI draws.
 *
 * <p>Replaces the Veil {@code AdvancedFbo}s used before 26.1. Rendering is redirected into the scene target through
 * vanilla's {@link RenderSystem#outputColorTextureOverride}, the same hook GUI picture-in-picture renderers use.</p>
 */
public final class DiagramFramebuffers implements AutoCloseable {
    private final TextureTarget scene;
    private final TextureTarget output;

    public DiagramFramebuffers(final String label, final int width, final int height) {
        this.scene = new TextureTarget("Diagram " + label + " scene", width, height, true);
        this.output = new TextureTarget("Diagram " + label + " output", width, height, false);
    }

    public TextureTarget scene() {
        return this.scene;
    }

    public TextureTarget output() {
        return this.output;
    }

    /** Clears the scene to transparent with the far depth, which the outline pass reads as "no geometry". */
    public void clearScene() {
        RenderSystem.getDevice().createCommandEncoder()
                .clearColorAndDepthTextures(this.scene.getColorTexture(), 0, this.scene.getDepthTexture(), 1.0);
    }

    /** Sends subsequent draws to the scene target until {@link #unbind()}. */
    public void bindScene() {
        RenderSystem.outputColorTextureOverride = this.scene.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = this.scene.getDepthTextureView();
    }

    public static void unbind() {
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
    }

    @Override
    public void close() {
        this.scene.destroyBuffers();
        this.output.destroyBuffers();
    }
}
