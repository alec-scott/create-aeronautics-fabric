package dev.simulated_team.simulated.compat.create;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.zurrtum.create.client.catnip.lang.FontHelper;
import com.zurrtum.create.client.foundation.item.ItemDescription;
import com.zurrtum.create.client.foundation.item.KineticStats;
import com.zurrtum.create.client.foundation.item.TooltipHelper;
import com.zurrtum.create.client.foundation.item.TooltipModifier;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Restores the item-tooltip registration formerly supplied by CreateRegistrate.
 */
public final class LegacyItemTooltips {
    private LegacyItemTooltips() {
    }

    public static void register(final AbstractRegistrate<?> registrate) {
        for (final RegistryEntry<Item, Item> entry : registrate.<Item, Item>getAll(Registries.ITEM)) {
            final Item item = entry.get();
            if (TooltipModifier.REGISTRY.get(item) != null) {
                continue;
            }

            // PORT-NOTE(mc26.1): item components (rarity included) are only bound when data loads, not at client init,
            // so the epic palette is picked when the tooltip is shown rather than at registration.
            final TooltipModifier standard = new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE);
            final TooltipModifier epic = new ItemDescription.Modifier(item, new FontHelper.Palette(
                    TooltipHelper.styleFromColor(SimColors.EPIC_OURPLE),
                    TooltipHelper.styleFromColor(Rarity.EPIC.color())
            ));
            final TooltipModifier description = (tooltip, player) ->
                    (item.getDefaultInstance().getRarity() == Rarity.EPIC ? epic : standard).modify(tooltip, player);

            TooltipModifier.REGISTRY.register(
                    item,
                    description.andThen(TooltipModifier.mapNull(KineticStats.create(item)))
            );
        }
    }
}
