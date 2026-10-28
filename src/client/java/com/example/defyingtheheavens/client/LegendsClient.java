package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Legend;
import com.example.defyingtheheavens.ModBlockEntities;
import com.example.defyingtheheavens.ModEntities;
import com.example.defyingtheheavens.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * The legendary weapons' looks (Legends; phase 3 of the Artifact Forge): Heaven's Tribulation Spear drawn as a bolt of tribulation
 * (TribulationSpearRenderer), the sword spirits (SwordSpiritRenderer), the sword resting in a legend's stone (LegendStoneRenderer), and a
 * legend made vast by its art (ColossusRenderer). Ganjiang, Moye, the Xuanyuan Sword, the Sea-Fixing Needle and the Sun-Shooting Bow are
 * models of their own (models/item/*.json: tools/beast-art LegendArt), the bow's drawn by its pull as vanilla's bow is.
 */
public final class LegendsClient {
	public static void register() {
		BuiltinItemRendererRegistry.INSTANCE.register(ModItems.legend(Legend.TRIBULATION_SPEAR), new TribulationSpearRenderer());
		EntityRendererRegistry.register(ModEntities.SWORD_SPIRIT, SwordSpiritRenderer::new);
		EntityRendererRegistry.register(ModEntities.COLOSSUS, ColossusRenderer::new);
		BlockEntityRenderers.register(ModBlockEntities.LEGEND_STONE, LegendStoneRenderer::new);
		Item bow = ModItems.legend(Legend.SUN_BOW);
		ItemProperties.register(bow, new ResourceLocation("pull"), (stack, level, entity, seed) ->
				entity == null || entity.getUseItem() != stack ? 0 : (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0f);
		ItemProperties.register(bow, new ResourceLocation("pulling"), (stack, level, entity, seed) ->
				entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1 : 0);
	}

	private LegendsClient() {}
}
