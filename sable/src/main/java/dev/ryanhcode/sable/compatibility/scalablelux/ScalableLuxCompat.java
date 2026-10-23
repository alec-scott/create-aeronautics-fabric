package dev.ryanhcode.sable.compatibility.scalablelux;

import ca.spottedleaf.starlight.common.light.StarLightEngine;
import ca.spottedleaf.starlight.common.light.StarLightInterface;
import ca.spottedleaf.starlight.common.light.StarLightLightingProvider;
import dev.ryanhcode.sable.mixin.compatibility.scalablelux.StarLightInterfaceAccessor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Direct bridge to ScalableLux 0.2/0.3's lighting implementation.
 *
 * <p>This class is only linked when Fabric Loader reports ScalableLux as
 * loaded. Keeping the optional types here allows the rest of Sable to load
 * normally without ScalableLux on the class path.</p>
 */
public final class ScalableLuxCompat {
    // ScalableLux 0.3 no longer mixes StarLightLightingProvider into every LevelLightEngine;
    // standalone engines must be this subclass instead. Null on 0.2, where the mixin still applies.
    private static final MethodHandle BASE_ENGINE_CONSTRUCTOR = findBaseEngineConstructor();

    private ScalableLuxCompat() {
    }

    private static MethodHandle findBaseEngineConstructor() {
        try {
            final Class<?> engineClass = Class.forName(
                    "ca.spottedleaf.starlight.common.light.vanillainterface.BaseLevelLightEngineVanillaInterface"
            );
            return MethodHandles.publicLookup().findConstructor(
                    engineClass,
                    MethodType.methodType(void.class, LightChunkGetter.class, boolean.class, boolean.class)
            );
        } catch (final ReflectiveOperationException e) {
            return null;
        }
    }

    public static LevelLightEngine createLightEngine(final LightChunkGetter chunkGetter, final boolean blockLight, final boolean skyLight) {
        if (BASE_ENGINE_CONSTRUCTOR == null) {
            return new LevelLightEngine(chunkGetter, blockLight, skyLight);
        }
        final LevelLightEngine engine;
        try {
            engine = (LevelLightEngine) BASE_ENGINE_CONSTRUCTOR.invoke(chunkGetter, blockLight, skyLight);
        } catch (final Throwable t) {
            throw new IllegalStateException("Failed to create ScalableLux light engine", t);
        }

        // Plots aren't ThreadedLevelLightEngines, so 0.3's concurrent queue can't schedule them;
        // process their light updates synchronously on the ticking thread instead.
        final StarLightInterface scalableLux = getLightEngine(engine);
        final StarLightInterfaceAccessor accessor = (StarLightInterfaceAccessor) (Object) scalableLux;
        if (!(accessor.sable$getLightQueue() instanceof StarLightInterface.SimpleLightQueue)) {
            accessor.sable$setLightQueue(new StarLightInterface.SimpleLightQueue(scalableLux));
        }
        return engine;
    }

    public static boolean hasBlockLight(final LevelLightEngine engine) {
        return getLightEngine(engine).hasBlockLight();
    }

    public static boolean hasSkyLight(final LevelLightEngine engine) {
        return getLightEngine(engine).hasSkyLight();
    }

    public static void lightChunk(final LevelLightEngine engine, final LevelChunk chunk) {
        final StarLightInterface scalableLux = getLightEngine(engine);
        chunk.setLightCorrect(false);
        scalableLux.lightChunk(chunk, StarLightEngine.getEmptySectionsForChunk(chunk));
        chunk.setLightCorrect(true);
        engine.setLightEnabled(chunk.getPos(), true);
    }

    public static void removeChunk(final LevelLightEngine engine, final ChunkPos pos) {
        // PORT-NOTE(mc26.1): removeChunkTasks is gone in ScalableLux 0.2; drop the queued tasks per queue type.
        final StarLightInterface.LightQueue queue = ((StarLightInterfaceAccessor) (Object) getLightEngine(engine)).sable$getLightQueue();
        if (queue instanceof final StarLightInterface.SimpleLightQueue simpleQueue) {
            simpleQueue.removeChunk(pos);
        } else if (queue instanceof final StarLightInterface.ConcurrentLightQueue concurrentQueue) {
            concurrentQueue.takeTask(pos.pack());
        }
        engine.setLightEnabled(pos, false);
    }

    private static StarLightInterface getLightEngine(final LevelLightEngine engine) {
        if (!(engine instanceof final StarLightLightingProvider provider)) {
            throw new IllegalStateException(
                    "ScalableLux is loaded but LevelLightEngine has no StarLightLightingProvider"
            );
        }
        return provider.getLightEngine();
    }
}
