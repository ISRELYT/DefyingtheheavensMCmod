package com.example.defyingtheheavens.client;


import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ModEntities;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public class DefyingTheHeavensClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// The Heaven Altar's offering screen.
		net.minecraft.client.gui.screens.MenuScreens.register(com.example.defyingtheheavens.HeavenAltarMenu.TYPE, HeavenAltarScreen::new);
		net.minecraft.client.gui.screens.MenuScreens.register(com.example.defyingtheheavens.VesselMenu.TYPE, VesselScreen::new);
		net.minecraft.client.gui.screens.MenuScreens.register(com.example.defyingtheheavens.StorageRingMenu.TYPE, StorageRingScreen::new);
		// A Storage Ring looks its grade: "defying-the-heavens:grade" 0 to 4 picks the model (models/item/storage_ring_*).
		net.minecraft.client.renderer.item.ItemProperties.register(com.example.defyingtheheavens.ModItems.STORAGE_RING, DefyingTheHeavens.id("grade"),
				(stack, level, entity, seed) -> com.example.defyingtheheavens.StorageRingItem.grade(stack).ordinal() / 4.0f);
		// A pill shows its grade: from Mid up "defying-the-heavens:grade" picks its "_glow" model, whose halo (layer 1) takes the
		// grade's colour (PillGrade); a Low-Grade pill has none.
		net.minecraft.world.item.Item[] pills = {com.example.defyingtheheavens.ModItems.MARROW_CLEANSING_ELIXIR,
				com.example.defyingtheheavens.ModItems.FOUNDATION_PILL, com.example.defyingtheheavens.ModItems.CORE_PILL,
				com.example.defyingtheheavens.ModItems.QI_GATHERING_PILL, com.example.defyingtheheavens.ModItems.CULTIVATION_PILL,
				com.example.defyingtheheavens.ModItems.YANG_PRESERVATION_PILL, com.example.defyingtheheavens.ModItems.YIN_GATHERING_PILL,
				com.example.defyingtheheavens.ModItems.LONGEVITY_PILL};
		for (net.minecraft.world.item.Item pill : pills) {
			net.minecraft.client.renderer.item.ItemProperties.register(pill, DefyingTheHeavens.id("grade"),
					(stack, level, entity, seed) -> com.example.defyingtheheavens.PillItem.grade(stack).ordinal() / 4.0f);
		}
		net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
			if (tintIndex != 1) return -1;
			Integer colour = com.example.defyingtheheavens.PillItem.grade(stack).getColor().getColor();
			return colour == null ? -1 : colour;
		}, pills);
		// A Jade Slip's writing (layer 1) is inked in its method's element.
		net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register(
				(stack, tintIndex) -> tintIndex == 1 ? slipInk(com.example.defyingtheheavens.JadeSlipItem.method(stack)) : -1,
				com.example.defyingtheheavens.ModItems.JADE_SLIP);
		// A Dao Fragment's light (layer 1) is its Law's colour.
		net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register(
				(stack, tintIndex) -> tintIndex == 1 ? com.example.defyingtheheavens.DaoFragmentItem.tint(stack) : -1,
				com.example.defyingtheheavens.ModItems.DAO_FRAGMENT);
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
				com.example.defyingtheheavens.ModBlocks.CULTIVATION_FRUIT, net.minecraft.client.renderer.RenderType.cutout());
		// The First Flame: a fire's crossed planes, the air between its tongues clear.
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
				com.example.defyingtheheavens.ModBlocks.FIRST_FLAME, net.minecraft.client.renderer.RenderType.cutout());
		// Like vanilla leaves: the gaps between blossoms are see-through (in Fast graphics, leaves draw solid anyway).
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
				com.example.defyingtheheavens.ModBlocks.WHITE_BLOSSOM_LEAVES, net.minecraft.client.renderer.RenderType.cutoutMipped());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(net.minecraft.client.renderer.RenderType.cutout(),
				com.example.defyingtheheavens.ModBlocks.GINSENG, com.example.defyingtheheavens.ModBlocks.SPIRIT_GINSENG,
				com.example.defyingtheheavens.ModBlocks.SPIRIT_PEACH_SAPLING, com.example.defyingtheheavens.ModBlocks.LINGZHI,
				com.example.defyingtheheavens.ModBlocks.PURPLE_LINGZHI, com.example.defyingtheheavens.ModBlocks.HUANGJING,
				com.example.defyingtheheavens.ModBlocks.OCHRE_HUANGJING, com.example.defyingtheheavens.ModBlocks.SPIRIT_LOTUS,
				com.example.defyingtheheavens.ModBlocks.SPIRIT_DEW_GRASS);
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
				com.example.defyingtheheavens.ModBlocks.SPIRIT_PEACH_LEAVES, net.minecraft.client.renderer.RenderType.cutoutMipped());
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.example.defyingtheheavens.ModBlockEntities.GINSENG, context -> new GinsengAuraRenderer());
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.example.defyingtheheavens.ModBlockEntities.CULTIVATION_FRUIT, context -> new CultivationFruitAuraRenderer());
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.example.defyingtheheavens.ModBlockEntities.SPIRIT_PEDESTAL, SpiritPedestalRenderer::new);
		// Items used on both sides (pills) read the local player's cultivation from the synced mirror.
		com.example.defyingtheheavens.CultivationManager.clientMirror = ClientCultivationData::get;
		// The Alchemy Cauldron's water: the biome's water colour, turning jade while a brew cooks.
		net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
			if (tintIndex != 0) return -1;
			if (state.getValue(com.example.defyingtheheavens.AlchemyCauldronBlock.BREWING)) return 0x4FD6A0;
			return world != null && pos != null ? net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(world, pos) : 0x3F76E4;
		}, com.example.defyingtheheavens.ModBlocks.ALCHEMY_CAULDRON);
		ModKeybinds.register();
		BaublePanel.register();
		ClientPacketHandlers.register();
		VitalsHud.register(); // the portrait, health and qi in the top-left corner (vanilla's hearts held to ten: GuiHeartsMixin)
		QiHud.register();
		QiSurgeHud.register();
		TribulationHud.register();
		TribulationAtmosphere.register();
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			SuppressionClient.renderHud(graphics); // under the Qi bar
			SoulHud.render(graphics); // under that: a projected soul's tether and body
			ConsciousnessRenderer.renderHud(graphics);
		});
		// Soul Projection: the soul's flight and state, its look (a core shader of its own), its body and seized bodies.
		ClientSoul.register();
		ClientBodies.register(); // a soul in another's body wears its look and name (Possession)
		SoulRenderer.register();
		SpellGhosts.register(); // the spirit shader the spells' White Impermanence, Black Impermanence and avatar are drawn in
		EntityRendererRegistry.register(ModEntities.SOUL_BODY, SoulBodyRenderer::new);
		// A clone standing (or the main body sitting) while its player plays the other (Clones).
		EntityRendererRegistry.register(ModEntities.CLONE_BODY, CloneBodyRenderer::new);
		ClientClone.register();
		EntityRendererRegistry.register(ModEntities.SOUL_VESSEL, SoulVesselRenderer::new);
		EntityRendererRegistry.register(ModEntities.TRIBULATION_LIGHTNING, TribulationLightningRenderer::new);
		EntityRendererRegistry.register(ModEntities.TRIBULATION_CLOUD, TribulationCloudRenderer::new);
		// Sects and NPC cultivators: the NPCs and their nameplates, clothing, formations and their seals.
		EntityRendererRegistry.register(ModEntities.CULTIVATOR, CultivatorNpcRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(com.example.defyingtheheavens.ModBlockEntities.SEAL, SealRenderer::new);
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(com.example.defyingtheheavens.ModBlockEntities.FORMATION_CORE,
				FormationCoreRenderer::new);
		FormationCoreRenderer.register();
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
				com.example.defyingtheheavens.ModBlocks.FORMATION_CORE, net.minecraft.client.renderer.RenderType.cutout());
		ClothingModels.register();
		ClothingArmorRenderer.register();
		// Beasts' heads worn in place of a man's (the Ghost Gate's Ox-Head and Horse-Face; see BeastHead).
		BeastHeadModels.register();
		BeastModels.register(); // the wild demon beasts, what they throw, and the demon cores' looks
		GhostKingCrown.register(); // a Ghost King's crown (GhostSystem#isKing)
		ClientFormations.register();
		com.example.defyingtheheavens.QiVeinBlock.clientEffects = QiSenseClientHandler::veinGathers;
		EntityRendererRegistry.register(ModEntities.INNER_BODY, InnerBodyRenderer::new);
		EntityRendererRegistry.register(ModEntities.HEART_DEMON, HeartDemonRenderer::new);
		// The Inner Realm's floor: see-through, and coloured by the viewer's realm (gold where a sequence lights it).
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
				com.example.defyingtheheavens.ModBlocks.SOUL_CRYSTAL, net.minecraft.client.renderer.RenderType.translucent());
		net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) ->
				SoulCrystalColours.of(state), com.example.defyingtheheavens.ModBlocks.SOUL_CRYSTAL);
		ParticleFactoryRegistry.getInstance().register(ModParticles.SUPPRESSION, SuppressionParticle.Provider::new);
		// The element spells: their missiles, what they leave standing, blocks they move, golems, the Dirt Tide; the auras they
		// leave on beings (on the body's model, and round it in the world); their passing moments.
		EntityRendererRegistry.register(ModEntities.SPELL_PROJECTILE, SpellProjectileRenderer::new);
		EntityRendererRegistry.register(ModEntities.SPELL_FIELD, SpellFieldRenderer::new);
		EntityRendererRegistry.register(ModEntities.SPELL_CONSTRUCT, SpellConstructRenderer::new); // the great spells' GeckoLib shapes
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> SpellConstructRenderer.preloadSoon());
		ClientTickEvents.END_CLIENT_TICK.register(SpellConstructRenderer::tick);
		EntityRendererRegistry.register(ModEntities.SPELL_BLOCK, SpellBlockRenderer::new);
		EntityRendererRegistry.register(ModEntities.EARTH_GOLEM, EarthGolemRenderer::new);
		EntityRendererRegistry.register(ModEntities.DIRT_TIDE, DirtTideRenderer::new);
		EntityRendererRegistry.register(ModEntities.SPATIAL_TEAR, SpatialTearRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer.getModel() instanceof net.minecraft.client.model.HumanoidModel<?>) registerAuraLayer(renderer, helper, context);
		});
		com.example.defyingtheheavens.SpellAuras.clientView = ClientSpellAuras::has;
		// The masteries as this client was told them (water's bears its own player up: HeavenPassives#masters).
		com.example.defyingtheheavens.HeavenPassives.clientView = (e, element) -> e == net.minecraft.client.Minecraft.getInstance().player
				&& com.example.defyingtheheavens.HeavenPassives.masters(ClientCultivationData.get(), element);
		WorldRenderEvents.AFTER_ENTITIES.register(SpellAuraWorld::render);
		WorldRenderEvents.AFTER_ENTITIES.register(ClientLawDomains::render); // the Laws' domains round their owners
		WorldRenderEvents.AFTER_TRANSLUCENT.register(ClientSpellFx::render);
		UpperRealmClient.register();
		NetherworldClient.register();
		AstralClient.register(); // space's sky, the planets' skies
		InnerRealmFade.register(); // last, so the fade covers the rest of the HUD

		WorldRenderEvents.AFTER_TRANSLUCENT.register(ClientSpatialStorms::render);
		GapLayers.register(); // the gap's layers made plain to one flying it freely: veils over the boundaries, the gauge
		WorldRenderEvents.AFTER_TRANSLUCENT.register(SuppressionClient::renderMarkers);
		// A struck barrier ripples, for everyone near (Qi Sense or not).
		WorldRenderEvents.AFTER_TRANSLUCENT.register(BarrierRipples::render);
		// Sword Flight: the sword under each rider's feet, and the trail of light behind it.
		WorldRenderEvents.AFTER_ENTITIES.register(SwordFlightClient::renderSwords);
		WorldRenderEvents.AFTER_TRANSLUCENT.register(SwordFlightClient::renderTrails);
		// Meteor Flight: the heat's glow, the trail of vapour or fire, the ring where the sound barrier broke.
		WorldRenderEvents.AFTER_TRANSLUCENT.register(MeteorFlightClient::render);
		// Under a shader pack, the realm's own sky laid over the pack's where the sky is all the mod's (space, the dead's).
		SkyLayer.register();
		WorldRenderEvents.END.register(SkyLayer::onWorldEnd);
		// Qi motes and consciousness domains, on the layer Qi Sense's monochrome leaves in colour.
		WorldRenderEvents.END.register(SenseOverlay::onWorldEnd);
		QiSenseTreasures.register();
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return DefyingTheHeavens.id("qi_sense_post_effect");
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				QiSenseClientHandler.closeMonochrome();
				ImpermanenceFigures.forget(); // built again from their models
			}
		});

		// When the connection drops, Fabric fires this on the network thread, while the render thread may be drawing from these
		// very lists (a ConcurrentModificationException crash): clear them on the client thread.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
			ClientTribulationData.clear();
			ClientCultivationData.clear();
			ClientClone.clear();
			ClientSect.clear();
			QuickAbilities.clear();
			SpatialGapAmbience.clear();
			ClientSpatialStorms.clear();
			MeditationFormation.clear();
			ClientMeditationTracker.clear();
			ClientStorageRing.clear();
			QiSenseClientHandler.clear();
			QiSenseTreasures.clear();
			ConsciousnessRenderer.clear();
			SuppressionClient.clear();
			ClientFormations.clear();
			BarrierRipples.clear();
			QiSurgeHud.clear();
			InnerRealmFade.clear();
			SwordFlightClient.clear();
			MeteorFlightClient.clear();
			ClientSoul.clear();
			ClientBodies.clear();
			ClientBloodlines.clear();
			ClientGestures.clear();
			ClientJournal.clear();
			ClientNpcSpeech.clear();
			NetherworldClient.clear();
			ClientSpellAuras.clear();
			ClientLawDomains.clear();
			ClientSpellFx.clear();
			ClientSpellTrails.clear();
			ClientSpellSounds.clear();
			BlockMasses.clear();
		}));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			SpatialGapAmbience.tick(client);
			ClientSpatialStorms.tick(client);
			MeditationFormation.tick(client);
			QiSenseClientHandler.tick(client);
			QiSenseTreasures.tick(client); // after the motes' own tick, which clears them when Qi Sense is off
			ConsciousnessRenderer.tick(client);
			SuppressionClient.tick(client);
			InnerRealmFade.tick(client);
			SwordFlightClient.tick(client);
			MeteorFlightClient.tick(client);
			ClientSoul.tick(client);
			ClientSpellTrails.tick(client);
			ClientSpellAuras.tick();
			ClientLawDomains.tick();
			ClientSpellFx.tick();
			ClientSpellSounds.tick();
			BlockMasses.tick(client);
			while (ModKeybinds.OPEN_MENU.consumeClick()) {
				if (client.player != null && client.screen == null) {
					client.setScreen(new CultivationScreen());
				}
			}
			while (ModKeybinds.ABILITY_WHEEL.consumeClick()) {
				if (client.player != null && client.screen == null) {
					// Nothing to switch before the first ability awakens: a word above the hotbar instead of an empty wheel.
					if (AbilityWheelScreen.canOpen(ClientCultivationData.get())) {
						client.setScreen(new AbilityWheelScreen());
					} else {
						client.gui.setOverlayMessage(Component.translatable(ModLang.ABILITIES_EMPTY), false);
					}
				}
			}
			QuickAbilities.tick(client);
			while (ModKeybinds.SPELL_WHEEL.consumeClick()) {
				if (client.player != null && client.screen == null) {
					// Likewise before the first spell is learned.
					if (ClientMethods.anyKnown()) client.setScreen(new SpellWheelScreen());
					else client.gui.setOverlayMessage(Component.translatable(ModLang.SPELLS_EMPTY), false);
				}
			}
			while (ModKeybinds.STORAGE_RING.consumeClick()) {
				if (client.player != null && client.screen == null) ClientPlayNetworking.send(ModPackets.STORAGE_RING_OPEN, PacketByteBufs.create());
			}
			while (ModKeybinds.CAST_SPELL.consumeClick()) {
				if (client.player != null && client.screen == null && ClientMethods.anyKnown()) ClientMethods.cast(null);
			}
			while (ModKeybinds.MEDITATE.consumeClick()) {
				if (client.player != null && client.screen == null) {
					ClientPlayNetworking.send(ModPackets.TOGGLE_MEDITATION, PacketByteBufs.create());
				}
			}
		});
	}

	/** The ink of a Jade Slip's writing: its method's element, gold for metal (or a slip holding none). */
	private static int slipInk(com.example.defyingtheheavens.HeavenMethod method) {
		if (method == null) return 0xD9A538;
		return switch (method.element()) {
			case WOOD -> 0x1FA34A;
			case WATER -> 0x2F7CF0;
			case FIRE -> 0xFF4020;
			case EARTH -> 0x9A6424;
			case YIN -> 0x3FC9CF;
			default -> 0xD9A538;
		};
	}

	/**
	 * The spells' auras on a renderer drawing a man's shape (players, cultivators...): SpellAuraLayer; and a demon's beast's head and
	 * tail (BeastLayer), before the auras so they lie over it.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void registerAuraLayer(net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?> renderer,
										  net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper helper,
										  net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
		helper.register(new BeastLayer(renderer));
		helper.register(new SpellAuraLayer(renderer, context));
	}
}
