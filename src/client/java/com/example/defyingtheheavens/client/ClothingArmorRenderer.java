package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ClothingItem;
import com.example.defyingtheheavens.ClothingStyle;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Cultivator clothing as worn (on players and NPCs alike, through Fabric's armor renderer), in the shapes of
 * {@link ClothingModels}: the robe with its skirt and hanging sleeves, the hair crown and pin, trousers, boots. Three greyscale
 * passes, tinted in turn with the path's cloth colour, its accent (lapels, sash, cuffs) and the grade's embroidery. The item
 * icons are tinted the same way (layer 0 cloth, 1 accent, 2 embroidery).
 */
public final class ClothingArmorRenderer implements ArmorRenderer {
	private static final ResourceLocation[][] TEXTURES = new ResourceLocation[2][3];

	static {
		String[] parts = {"cloth", "accent", "trim"};
		String[] layers = {"outer", "inner"};
		for (int layer = 0; layer < 2; layer++) {
			for (int part = 0; part < 3; part++) {
				TEXTURES[layer][part] = DefyingTheHeavens.id("textures/models/armor/cultivator_" + layers[layer] + "_" + parts[part] + ".png");
			}
		}
	}

	private HumanoidModel<LivingEntity> inner;
	private HumanoidModel<LivingEntity> outer;

	public static void register() {
		ClothingArmorRenderer renderer = new ClothingArmorRenderer();
		Item[] items = ModItems.allClothing().toArray(new Item[0]);
		ArmorRenderer.register(renderer, items);
		ColorProviderRegistry.ITEM.register((stack, tint) -> {
			if (!(stack.getItem() instanceof ClothingItem clothing)) return -1;
			ClothingStyle style = ClothingItem.style(stack);
			// The hair crown's icon starts with an untinted layer (the black topknot it sits on).
			int layer = clothing.getType() == net.minecraft.world.item.ArmorItem.Type.HELMET ? tint - 1 : tint;
			return switch (layer) {
				case 0 -> style.getCloth();
				case 1 -> style.getAccent();
				case 2 -> clothing.getTier().getTrimColor();
				default -> -1;
			};
		}, items);
	}

	@Override
	public void render(PoseStack matrices, MultiBufferSource buffers, ItemStack stack, LivingEntity entity, EquipmentSlot slot, int light,
			HumanoidModel<LivingEntity> contextModel) {
		if (!(stack.getItem() instanceof ClothingItem clothing)) return;
		if (inner == null) {
			inner = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ClothingModels.INNER));
			outer = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ClothingModels.OUTER));
		}
		boolean legs = slot == EquipmentSlot.LEGS;
		HumanoidModel<LivingEntity> model = legs ? inner : outer;
		contextModel.copyPropertiesTo(model);
		model.setAllVisible(false);
		switch (slot) {
			case HEAD -> {
				model.head.visible = true;
				model.hat.visible = true;
			}
			case CHEST -> {
				model.body.visible = true;
				model.rightArm.visible = true;
				model.leftArm.visible = true;
			}
			case LEGS -> {
				model.body.visible = true;
				model.rightLeg.visible = true;
				model.leftLeg.visible = true;
			}
			default -> {
				model.rightLeg.visible = true;
				model.leftLeg.visible = true;
			}
		}
		ClothingStyle style = ClothingItem.style(stack);
		int layer = legs ? 1 : 0;
		pass(matrices, buffers, model, TEXTURES[layer][0], style.getCloth(), light, stack.hasFoil());
		pass(matrices, buffers, model, TEXTURES[layer][1], style.getAccent(), light, false);
		pass(matrices, buffers, model, TEXTURES[layer][2], clothing.getTier().getTrimColor(), light, false);
	}

	private static void pass(PoseStack matrices, MultiBufferSource buffers, HumanoidModel<LivingEntity> model, ResourceLocation texture, int color,
			int light, boolean foil) {
		VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(buffers, RenderType.armorCutoutNoCull(texture), false, foil);
		float r = (color >> 16 & 0xFF) / 255f, g = (color >> 8 & 0xFF) / 255f, b = (color & 0xFF) / 255f;
		model.renderToBuffer(matrices, consumer, light, OverlayTexture.NO_OVERLAY, r, g, b, 1.0f);
	}
}
