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
 * on the player-armour head model), as the helmet slot would. When a helmet is also worn the goggles are drawn
 * slightly larger so they sit on top of it instead of clipping into it.
 */
public class AviatorsGogglesTrinketRenderer implements TrinketRenderer {
    private static final float OVER_HELMET_SCALE = 1.1F;

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
        float scale = humanoid.headEquipment.isEmpty() ? 1.0F : OVER_HELMET_SCALE;
        this.model.head.xScale = scale;
        this.model.head.yScale = scale;
        this.model.head.zScale = scale;
        this.equipmentRenderer.renderLayers(EquipmentClientInfo.LayerType.HUMANOID, equippable.assetId().get(),
                this.model, humanoid, stack, poseStack, queue, light, humanoid.outlineColor);
        this.model.head.xScale = 1.0F;
        this.model.head.yScale = 1.0F;
        this.model.head.zScale = 1.0F;
    }
}
