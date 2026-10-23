package dev.eriksonn.aeronautics.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.catnip.placement.PlacementClient;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.AeronauticsClient;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect.ClientBalloonEffectRenderer;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.IrisBurnerFlameRenderQueue;
import dev.eriksonn.aeronautics.events.AeronauticsClientEvents;
import foundry.veil.api.network.VeilPacketManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.concurrent.atomic.AtomicInteger;

public final class AeronauticsFabricClient implements ClientModInitializer {
    private static final int PRODUCTION_SMOKE_REQUIRED_TICKS = 20;
    private static final Identifier LEVITITE_BLEND_STILL = Aeronautics.path("fluid/levitite_blend_still");
    private static final Identifier LEVITITE_BLEND_FLOW = Aeronautics.path("fluid/levitite_blend_flow");
    private static final AtomicInteger PRODUCTION_SMOKE_TICKS = new AtomicInteger();

    @Override
    public void onInitializeClient() {
        VeilPacketManager.registerClientReceivers();
        FabricAeroParticleTypes.registerFactories();

        // PORT-NOTE(mc26.1): render layers are no longer registered; they come from each texture's transparency.
        // The levitite blend fluid is forced translucent here, levitite blocks through force_translucent in their
        // block models, and the burner's cutout comes from its texture alpha.
        FluidRenderingRegistry.register(
                FabricAeroFluids.LEVITITE_BLEND.getSource(),
                FabricAeroFluids.LEVITITE_BLEND.get(),
                new FluidModel.Unbaked(new Material(LEVITITE_BLEND_STILL, true), new Material(LEVITITE_BLEND_FLOW, true),
                        null, null));

        AeronauticsClient.init();

        LevelRenderEvents.START_MAIN.register(
                context -> IrisBurnerFlameRenderQueue.beginWorldFrame()
        );
        LevelRenderEvents.END_MAIN.register(
                context -> IrisBurnerFlameRenderQueue.finishWorldFrameCollection()
        );
        LevelRenderEvents.END_MAIN.register(context -> ClientBalloonEffectRenderer.render(
                new PoseStack(), Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)));
        ClientTickEvents.START_CLIENT_TICK.register(client -> AeronauticsClientEvents.clientLevelTick(false));
        ClientTickEvents.END_CLIENT_TICK.register(client -> AeronauticsClientEvents.clientLevelTick(true));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Only count ticks once the initial resource reload (loading overlay) has finished.
            if (Boolean.getBoolean("aeronautics.productionSmokeTest")
                    && client.getOverlay() == null
                    && PRODUCTION_SMOKE_TICKS.incrementAndGet() == PRODUCTION_SMOKE_REQUIRED_TICKS) {
                // Mixins apply lazily when their target class loads; force every target to load so in-world-only
                // classes (e.g. Create's BoilerData) are checked too.
                MixinEnvironment.getCurrentEnvironment().audit();
                // Create normally loads this only after joining a world. Exercise its mixins in production too.
                PlacementClient.tick(client);
                Aeronautics.LOGGER.info("AERONAUTICS_PRODUCTION_CLIENT_SMOKE_OK");
                client.stop();
            }
        });
    }
}
