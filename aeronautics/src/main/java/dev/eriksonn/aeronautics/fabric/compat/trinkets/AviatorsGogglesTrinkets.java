package dev.eriksonn.aeronautics.fabric.compat.trinkets;

import com.zurrtum.create.content.equipment.goggles.GogglesItem;
import dev.eriksonn.aeronautics.index.AeroItems;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.client.TrinketRendererRegistry;

/**
 * PORT-NOTE(mc26.1): Trinkets Updated support (not in upstream). Aviator's Goggles can be worn in the head/face
 * Trinkets slot (data/trinkets/tags/item/head/face.json), next to a helmet, and count as Engineer's Goggles there,
 * like Create Fly does for its own goggles. Only called when Trinkets is installed.
 */
public final class AviatorsGogglesTrinkets {
    private AviatorsGogglesTrinkets() {
    }

    public static void init() {
        GogglesItem.addIsWearingPredicate(player -> TrinketsApi.getAttachment(player)
                .isEquipped(stack -> AeroItems.AVIATORS_GOGGLES.isIn(stack), true));
    }

    public static void initClient() {
        TrinketRendererRegistry.registerRenderer(AeroItems.AVIATORS_GOGGLES.get(), AviatorsGogglesTrinketRenderer::new);
    }
}
