package dev.ryanhcode.sable.mixin.compatibility.scalablelux;

import ca.spottedleaf.starlight.common.light.StarLightInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// PORT-NOTE(mc26.1): ScalableLux 0.2 removed StarLightInterface.removeChunkTasks, so the queue is reached directly.
@Mixin(value = StarLightInterface.class, remap = false)
public interface StarLightInterfaceAccessor {

    @Accessor(value = "lightQueue", remap = false)
    StarLightInterface.LightQueue sable$getLightQueue();
}
