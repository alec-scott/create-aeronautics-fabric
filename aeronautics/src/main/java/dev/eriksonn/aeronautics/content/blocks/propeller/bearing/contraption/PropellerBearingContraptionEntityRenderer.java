package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.content.contraptions.render.ContraptionEntityRenderer;
import com.zurrtum.create.client.content.contraptions.render.ContraptionEntityRenderer.AbstractContraptionState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * PORT-NOTE(mc26.1): Create Fly's contraption renderers take the contraption's local transform from
 * {@link #createTransform}, which is identity in the base renderer. Feed it the propeller's own spin
 * and tilt so the blades rotate.
 */
public class PropellerBearingContraptionEntityRenderer extends ContraptionEntityRenderer<PropellerBearingContraptionEntity, AbstractContraptionState> {

    public PropellerBearingContraptionEntityRenderer(final EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public PoseStack.Pose createTransform(final PropellerBearingContraptionEntity entity, final float tickProgress) {
        final PoseStack poseStack = new PoseStack();
        entity.applyLocalTransforms(poseStack, tickProgress);
        return poseStack.last();
    }
}
