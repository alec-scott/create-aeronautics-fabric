package dev.eriksonn.aeronautics.fabric;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.data.AeroAdvancementTriggers;
import dev.eriksonn.aeronautics.events.AeronauticsCommonEvents;
import dev.eriksonn.aeronautics.fabric.service.FabricAeroConfigService;
import dev.eriksonn.aeronautics.index.AeroAdvancements;
import dev.eriksonn.aeronautics.index.AeroArmInteractionPoints;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class AeronauticsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricAeroConfigService.register();
        Aeronautics.init();

        LevititeBlendFluidBehavior.register();
        FabricAeroFluids.init();
        FabricAeroParticleTypes.register();
        AeroArmInteractionPoints.init();
        AeroAdvancements.init();
        AeroAdvancementTriggers.register();

        Aeronautics.getRegistrate().register();

        ServerTickEvents.END_LEVEL_TICK.register(AeronauticsCommonEvents::onServerTickEnd);
        ServerLifecycleEvents.SERVER_STOPPED.register(AeronauticsCommonEvents::onServerStopped);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (Boolean.getBoolean("aeronautics.productionSmokeTest")) {
                SMOKE_COMMANDS.addAll(readSmokeCommands());
                smokeRunning = true;
                runSmokeCommands(server);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (smokeRunning) {
                runSmokeCommands(server);
            }
        });
    }

    private static final Deque<String> SMOKE_COMMANDS = new ArrayDeque<>();
    private static boolean smokeRunning;
    private static int smokeWaitTicks;

    /** Lines of {@code -Daeronautics.smokeCommandsFile}, if set (smoke tests only). */
    private static List<String> readSmokeCommands() {
        final String commandsFile = System.getProperty("aeronautics.smokeCommandsFile");
        if (commandsFile == null) {
            return List.of();
        }
        try {
            return Files.readAllLines(Path.of(commandsFile)).stream()
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .toList();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Runs queued smoke commands as the server; a {@code wait <ticks>} line lets the server tick before the next
     * command. Once the queue is empty, checks every mixin and stops the server.
     */
    private static void runSmokeCommands(final MinecraftServer server) {
        if (smokeWaitTicks > 0) {
            smokeWaitTicks--;
            return;
        }
        while (!SMOKE_COMMANDS.isEmpty()) {
            final String line = SMOKE_COMMANDS.poll();
            if (line.startsWith("wait ")) {
                smokeWaitTicks = Integer.parseInt(line.substring("wait ".length()).trim());
                Aeronautics.LOGGER.info("Smoke command: waiting {} ticks", smokeWaitTicks);
                return;
            }
            Aeronautics.LOGGER.info("Smoke command: {}", line);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), line);
        }
        smokeRunning = false;
        // Force every mixin target to load so mixins on classes not touched at startup are checked too.
        MixinEnvironment.getCurrentEnvironment().audit();
        Aeronautics.LOGGER.info("AERONAUTICS_PRODUCTION_SERVER_SMOKE_OK");
        server.halt(false);
    }
}
