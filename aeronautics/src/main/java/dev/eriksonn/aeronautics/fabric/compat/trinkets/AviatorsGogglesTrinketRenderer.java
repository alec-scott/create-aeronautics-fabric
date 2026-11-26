package dev.eriksonn.aeronautics.fabric.compat.trinkets;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.client.TrinketRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * PORT-NOTE(mc26.1): draws Aviator's Goggles worn in a Trinkets slot with their normal armour look (equipment asset
 * on the player-armour head model), as the helmet slot would. When a helmet is also worn only the strap and lenses
 * (outer layer) are drawn, around the helmet, instead of the full goggle cap.
 */
public class AviatorsGogglesTrinketRenderer implements TrinketRenderer {
    private static final float OVER_HELMET_SCALE = 1.02F; // a hair more so the strap never z-fights bulky helmets

    private final HumanoidModel<HumanoidRenderState> model;
    private final EquipmentLayerRenderer equipmentRenderer;

    public AviatorsGogglesTrinketRenderer(EntityRendererProvider.Context context) {
        this.model = ArmorModelSet.<HumanoidModel<HumanoidRenderState>>bake(
                ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new).head();
        this.equipmentRenderer = context.getEquipmentRenderer();
    }

    @Override
    public void submit(ItemStack stack, TrinketSlotAccess slotReference, EntityModel<? extends LivingEntityRenderState> contextModel,
                       PoseStack poseStack, SubmitNodeCollector queue, int light, LivingEntityRenderState state,
                       float limbAngle, float limbDistance) {
        if (!(state instanceof HumanoidRenderState humanoid)) {
            return;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.assetId().isEmpty()) {
            return;
        }

        this.model.setupAnim(humanoid);
        // Over a helmet, skip the capped inner head cube and draw only the outer "hat" layer, which in the goggle
        // texture is just the strap and lenses; it already sits half a pixel outside the helmet.
        boolean overHelmet = !humanoid.headEquipment.isEmpty();
        this.model.head.skipDraw = overHelmet;
        this.model.hat.xScale = this.model.hat.yScale = this.model.hat.zScale = overHelmet ? OVER_HELMET_SCALE : 1.0F;
        this.equipmentRenderer.renderLayers(EquipmentClientInfo.LayerType.HUMANOID, equippable.assetId().get(),
                this.model, humanoid, stack, poseStack, queue, light, humanoid.outlineColor);
        this.model.head.skipDraw = false;
        this.model.hat.xScale = this.model.hat.yScale = this.model.hat.zScale = 1.0F;
    }
}
