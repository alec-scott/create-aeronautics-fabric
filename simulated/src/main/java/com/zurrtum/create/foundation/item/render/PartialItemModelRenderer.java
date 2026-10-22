package com.zurrtum.create.foundation.item.render;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.SimpleModelWrapper;

public class PartialItemModelRenderer {
    public void render(SimpleModelWrapper model, int light) {
    }

    public void render(SimpleModelWrapper model, RenderType type, int light) {
    }

    public void render(PartialModel model, RenderType type, int light) {
    }

    // PORT-NOTE(mc26.1): Create-Fly's PartialModel.get() returns a BlockStateModel now.
    public void render(BlockStateModel model, RenderType type, int light) {
    }
}
