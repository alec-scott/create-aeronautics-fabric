package dev.simulated_team.simulated.mixin.end_sea;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.simulated_team.simulated.content.end_sea.EndSeaRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    // PORT-NOTE(mc26.1): renderLevel takes a CameraRenderState instead of the Camera and matrices.
    @Inject(method = "renderLevel", at = @At("RETURN"))
    public void renderLevel(final GraphicsResourceAllocator allocator, final DeltaTracker deltaTracker, final boolean renderBlockOutline,
                            final CameraRenderState cameraState, final Matrix4fc modelViewMatrix,
                            final GpuBufferSlice fogBuffer, final Vector4f fogColor, final boolean renderSky,
                            final ChunkSectionsToRender chunkSectionsToRender, final CallbackInfo ci) {
        final Minecraft minecraft = Minecraft.getInstance();
        EndSeaRenderer.render(minecraft.gameRenderer.getMainCamera(), minecraft.gameRenderer);
    }

}
