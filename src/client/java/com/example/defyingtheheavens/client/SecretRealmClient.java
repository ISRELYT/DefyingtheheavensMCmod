package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.LawSealBlock;
import com.example.defyingtheheavens.MirageBlock;
import com.example.defyingtheheavens.ModBlockEntities;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.ModEntities;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.SecretRealm;
import com.example.defyingtheheavens.SecretRealms;
import com.example.defyingtheheavens.TrialBarrierBlock;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * The secret realms' look and their row on the HUD (SecretRealms).
 * <ul>
 *   <li><b>Its row</b> (the top left, under the bars, as the mockups' "G" draws its rows: HallArt): inside a realm, its name and the time left,
 *   red as it quakes at the end; near a Seal while its realm stirs, how long till it opens; while its gate stands open, how long it stays
 *   so. Counted down here between the server's words (ModPackets#sendSecretRealm), once a second.</li>
 *   <li><b>Its quake</b>: the view shaken as it closes (ClientSpellFx#shakeAtLeast).</li>
 *   <li><b>Its sky</b>: a held morning, clouds low among its peaks; each realm's own mist ({@link #fog}: UpperRealmClient#fogFor), its colour
 *   its biome's leaning toward the realm's own (the Valley's pale, the Tomb's steel-grey, the Battlefield's rust-red).</li>
 *   <li>Its Seal's light (SecretRealmSealRenderer), its killing formations' rings under Qi Sense (KillingFormationRenderer), its guardians
 *   (RealmGuardianRenderer); the gate's light and the trial's barrier drawn see-through, and the barrier let by as the server says
 *   (TrialBarrierBlock#CLIENT).</li>
 * </ul>
 */
public final class SecretRealmClient {
	/**
	 * Each realm's mist (by SecretRealm#ordinal): the Valley's from an eighth of the render distance, thick by three quarters; the Tomb's
	 * steel-grey and a little closer; the Battlefield's rust-red haze closer still; the Graveyard's steaming green haze; the Cave Abode's
	 * thin and bright (its mountain seen from afar); the Hollow's dark and close (its nearer islands still seen through it); the Mirage's
	 * pale jade and far (the lake seen across); the Ruins' violet void about its shards; the Tomb's gold over its sea of cloud.
	 */
	private static final float[][] FOG = {{0.12f, 0.78f}, {0.1f, 0.7f}, {0.05f, 0.6f}, {0.06f, 0.62f}, {0.2f, 0.92f}, {0.08f, 0.8f},
			{0.18f, 0.9f}, {0.12f, 0.82f}, {0.15f, 0.88f}};
	private static final Vec3[] MIST = {new Vec3(0.86, 0.9, 0.88), new Vec3(0.7, 0.74, 0.78), new Vec3(0.62, 0.42, 0.36), new Vec3(0.66, 0.76, 0.58),
			new Vec3(0.86, 0.9, 0.96), new Vec3(0.24, 0.21, 0.12), new Vec3(0.86, 0.94, 0.9), new Vec3(0.29, 0.24, 0.4), new Vec3(0.96, 0.88, 0.72)};

	/** The realm the camera is in (by its slot), its ordinal; the first's where there is none. */
	private static int viewed() {
		Vec3 at = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		SecretRealm realm = SecretRealm.at(at.x, at.z);
		return realm == null ? 0 : Math.min(realm.ordinal(), FOG.length - 1);
	}

	/** The mist's start and end (shares of the render distance) where the camera is. */
	static float[] fog() {
		return FOG[viewed()];
	}

	/** The row as the server last told it: which realm (-1 none), what it shows, the ticks it counts down, when told, the barrier passed. */
	private static int realm = -1, kind, ticks;
	private static long told;
	private static boolean passed;

	public static void register() {
		DimensionRenderingRegistry.registerDimensionEffects(ModDimensions.SECRET_REALM_EFFECTS, new Effects());
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SECRET_REALM, (client, handler, buf, sender) -> {
			int r = buf.readVarInt() - 1, k = buf.readVarInt(), t = buf.readVarInt();
			boolean p = buf.readBoolean();
			client.execute(() -> {
				realm = r;
				kind = k;
				ticks = t;
				passed = p;
				told = Util.getMillis();
			});
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> realm = -1);
		TrialBarrierBlock.CLIENT = entity -> entity == Minecraft.getInstance().player && passed && realm >= 0;
		// A Law's seal lets its own player by if they hold its Law (their Laws are theirs to know); a mirage shimmers under their Qi Sense.
		LawSealBlock.CLIENT_HOLDS = (entity, law) -> entity == Minecraft.getInstance().player && ClientCultivationData.get().laws().level(law) >= 1;
		MirageBlock.SENSING = QiSenseClientHandler::isActive;
		HudRenderCallback.EVENT.register((graphics, delta) -> renderHud(graphics));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Its last minutes: the land quakes under one inside, now and then a harder jolt.
			if (realm >= 0 && kind == SecretRealms.HUD_QUAKING && client.level != null && ModDimensions.isSecretRealm(client.level.dimension())) {
				ClientSpellFx.shakeAtLeast(client.level.getGameTime() % 40 < 6 ? 0.9f : 0.22f);
			}
		});
		BlockEntityRenderers.register(ModBlockEntities.SECRET_REALM_SEAL, context -> new SecretRealmSealRenderer());
		BlockEntityRenderers.register(ModBlockEntities.KILLING_FORMATION, context -> new KillingFormationRenderer());
		EntityRendererRegistry.register(ModEntities.VINE_KEEPER, context -> new RealmGuardianRenderer(context, "vine_keeper", 0.8f));
		EntityRendererRegistry.register(ModEntities.BRONZE_PUPPET, context -> new RealmGuardianRenderer(context, "bronze_puppet", 0.7f));
		EntityRendererRegistry.register(ModEntities.SWORD_WRAITH, context -> new RealmGuardianRenderer(context, "sword_wraith", 0.35f));
		EntityRendererRegistry.register(ModEntities.BONE_SOLDIER, context -> new RealmGuardianRenderer(context, "bone_soldier", 0.5f));
		EntityRendererRegistry.register(ModEntities.BATTLE_SPIRIT, context -> new RealmGuardianRenderer(context, "battle_spirit", 0.3f, 1.0f, true));
		EntityRendererRegistry.register(ModEntities.GENERAL_SPIRIT, context -> new RealmGuardianRenderer(context, "general_spirit", 0.6f, 1.35f, true));
		EntityRendererRegistry.register(ModEntities.ARMY_SPIRIT, ArmySpiritRenderer::new);
		EntityRendererRegistry.register(ModEntities.DRAGON_ECHO, context -> new RealmGuardianRenderer(context, "dragon_echo", 0.8f, 1.0f, true));
		EntityRendererRegistry.register(ModEntities.MASTER_PUPPET, context -> new RealmGuardianRenderer(context, "master_puppet", 0.75f, 1.1f, false));
		EntityRendererRegistry.register(ModEntities.DROWNED_SHADE, context -> new RealmGuardianRenderer(context, "drowned_shade", 0.3f, 1.0f, true));
		EntityRendererRegistry.register(ModEntities.FERRYMAN, context -> new RealmGuardianRenderer(context, "ferryman", 0.6f, 1.0f, false));
		EntityRendererRegistry.register(ModEntities.MIRROR_SHADE, context -> new RealmGuardianRenderer(context, "mirror_shade", 0.3f, 1.0f, true));
		EntityRendererRegistry.register(ModEntities.MIRROR_REFLECTION, MirrorReflectionRenderer::new);
		EntityRendererRegistry.register(ModEntities.HOLY_REMNANT, context -> new RealmGuardianRenderer(context, "holy_remnant", 0.5f, 1.0f, false));
		EntityRendererRegistry.register(ModEntities.DECAYED_GUARD, context -> new RealmGuardianRenderer(context, "decayed_guard", 0.5f, 1.0f, false));
		EntityRendererRegistry.register(ModEntities.DECAYED_IMMORTAL, context -> new RealmGuardianRenderer(context, "decayed_immortal", 0.7f, 1.35f, false));
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SECRET_REALM_GATE, RenderType.translucent());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TRIAL_BARRIER, RenderType.translucent());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PILL_SAGE_CAULDRON, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BURIED_SWORD, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MIRAGE_LOTUS, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.MIRAGE_CASKET, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.JADE_MIRROR, RenderType.translucent());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LAW_SEAL, RenderType.translucent());
		// Each Law's seal in its Law's colour (its model's faces take tint 0).
		net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
				(state, level, pos, tint) -> LawSealBlock.law(state).color(), ModBlocks.LAW_SEAL);
	}

	/** The ticks the row counts down now (the server's last word, less the time since). */
	private static long left() {
		return Math.max(0, ticks - (Util.getMillis() - told) / 50);
	}

	/** Its row under the bars down the left (and under a pressed one's line, and a soul's), a diamond and its words. */
	private static void renderHud(GuiGraphics graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (realm < 0 || realm >= SecretRealm.values().length || mc.player == null || mc.options.hideGui || mc.options.renderDebug) return;
		// Its word stale (a lost packet, gone out of reach): it waits no more than a few seconds for the next.
		if (Util.getMillis() - told > 4000) return;
		String name = SecretRealm.values()[realm].displayName().getString();
		String time = SecretRealms.clock(left());
		String key = switch (kind) {
			case SecretRealms.HUD_INSIDE -> ModLang.HUD_SECRET_INSIDE;
			case SecretRealms.HUD_QUAKING -> ModLang.HUD_SECRET_QUAKING;
			case SecretRealms.HUD_STIRRING -> ModLang.HUD_SECRET_STIRRING;
			case SecretRealms.HUD_GATE -> ModLang.HUD_SECRET_GATE;
			default -> null;
		};
		if (key == null) return;
		boolean quaking = kind == SecretRealms.HUD_QUAKING;
		HallArt.beginHud(graphics);
		try {
			float y = Math.max(QiHud.stackBottom() + 2 + SuppressionClient.lineHeight(), SoulHud.bottom() + 4);
			int colour = quaking ? 0xFFFF6A5A : 0xFF8EE6C0;
			HallArt.diamond(graphics, 17, y + 7, 10, colour);
			String text = Component.translatable(key, name, time).getString();
			HallArt.text(graphics, text, 34, y, 16, 22, (quaking ? 0xFF8A7A : 0xCFF5E2), true);
			HallArt.corner(y, y + 22, 34 + HallArt.width(text, 16) + 2);
		} finally {
			HallArt.end(graphics);
		}
	}

	/** Its sky: the vanilla day held still, clouds low among its peaks; its fog leaning pale toward the mist. */
	private static final class Effects extends DimensionSpecialEffects {
		Effects() {
			super(150.0f, true, SkyType.NORMAL, false, false);
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
			Vec3 mist = MIST[Math.min(viewed(), MIST.length - 1)];
			return fogColor.multiply(brightness * 0.9 + 0.1, brightness * 0.9 + 0.1, brightness * 0.88 + 0.12).lerp(mist.scale(brightness * 0.8 + 0.2), 0.35);
		}

		@Override
		public boolean isFoggyAt(int x, int y) {
			return false;
		}
	}

	private SecretRealmClient() {}
}
