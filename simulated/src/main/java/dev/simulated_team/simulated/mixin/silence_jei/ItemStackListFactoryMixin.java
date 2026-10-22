package dev.simulated_team.simulated.mixin.silence_jei;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.service.SimTabService;
import mezz.jei.library.plugins.vanilla.ingredients.ItemStackListFactory;
import net.minecraft.world.item.CreativeModeTab;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Silences JEI's error logging for Simulated's creative tab.
 *
 * <p>PORT-NOTE(mc26.1): JEI 29.x moved the "Item Group crashed" log into a lambda inside {@code create()}, so the
 * old {@code method = "create"} target crashed the game on world join. These wrappers now match every
 * {@code Logger.error} call in the class ({@code method = "*"}, {@code require = 0}) and recognise Simulated's tab
 * from the log arguments (the tab itself, or its display name). They only hide log lines, so no JEI update can turn
 * them into a crash: a missing method name fails mixin application even with {@code require = 0}, which is why
 * they don't name methods.</p>
 */
@Pseudo
@Mixin(value = ItemStackListFactory.class, remap = false)
public class ItemStackListFactoryMixin {

	@WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"), require = 0)
	private static void simulated$error(final Logger instance, final String string, final Object o, final Object o1, final Operation<Void> original) {
		if (!simulated$isSimulatedTab(o)) {
			original.call(instance, string, o, o1);
		}
	}

	@WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V"), require = 0)
	private static void simulated$error(final Logger instance, final String string, final Object o, final Object o1, final Object o2, final Operation<Void> original) {
		if (!simulated$isSimulatedTab(o)) {
			original.call(instance, string, o, o1, o2);
		}
	}

	@Unique
	private static boolean simulated$isSimulatedTab(final Object logArgument) {
		final CreativeModeTab tab = SimTabService.INSTANCE.getCreativeTab();
		return logArgument == tab || (logArgument instanceof final String name && name.equals(tab.getDisplayName().getString()));
	}
}
