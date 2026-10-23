package dev.ryanhcode.sable.mixin.compatibility.scalablelux;

import ca.spottedleaf.starlight.common.light.StarLightInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

// PORT-NOTE(mc26.1): ScalableLux 0.2 removed StarLightInterface.removeChunkTasks, so the queue is reached directly.
@Mixin(value = StarLightInterface.class, remap = false)
public interface StarLightInterfaceAccessor {

    @Accessor(value = "lightQueue", remap = false)
    StarLightInterface.LightQueue sable$getLightQueue();

    // ScalableLux 0.3 gives server-world engines a ConcurrentLightQueue, which only works on a ThreadedLevelLightEngine.
    @Mutable
    @Accessor(value = "lightQueue", remap = false)
    void sable$setLightQueue(StarLightInterface.LightQueue queue);
}
