package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Bloodline;
import com.example.defyingtheheavens.CorpseBlock;
import com.example.defyingtheheavens.CorpseWalk;
import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.Possession;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Whose look each player wears on this client (the server's BODY_LOOK; Possession): while a nascent soul is in another player's
 * body, that player's skin, model, cape and name; in an NPC cultivator's, its skin and its nameplate; in a body of the dead (CorpseWalk,
 * 2026-11-13), the dead's own skin and name, paler fresh, grey-green rotting (as CorpseRenderer draws them lying), then a skeleton in its
 * shape (CorpseBonesRenderer). Its own client too: the hands it sees in first person are the body's. Forgotten when the server stops
 * telling it (once a second while it lasts).
 */
public final class ClientBodies {
	private static final long EXPIRY_MS = 3000;

	/** A look: another player's ({@code player}, their name and profile), or an NPC's (its entity id). */
	private record Look(UUID player, Component name, GameProfile profile, int npc, long at) {}

	/** A body of the dead's look: whose it was (a player's profile, a cultivator's skin), a demon's race, how far gone it is. */
	private record CorpseLook(String name, GameProfile profile, int skin, Bloodline race, int variant, int tails, CorpseBlock.Rot rot, long at) {}

	private static final Map<Integer, Look> LOOKS = new HashMap<>();
	private static final Map<Integer, CorpseLook> CORPSES = new HashMap<>();
	/**
	 * Each body player's skin, model and cape as last known: from the player list while they're in it, else (logged out, their body
	 * held on, or dead and walked in) from their profile by the skin manager, asked once.
	 */
	private static final Map<UUID, ResourceLocation> SKINS = new HashMap<>(), CAPES = new HashMap<>();
	private static final Map<UUID, String> MODELS = new HashMap<>();
	private static final java.util.Set<UUID> ASKED = new java.util.HashSet<>();
	/** How the dead are tinted, fresh and rotting (CorpseRenderer's own). */
	private static final float[] FRESH = {0.86f, 0.86f, 0.88f}, ROTTING = {0.52f, 0.58f, 0.45f};

	public static void register() {
		Possession.clientName = ClientBodies::name;
		CorpseWalk.clientWalking = player -> player instanceof AbstractClientPlayer client && corpse(client) != null;
	}

	/** BODY_LOOK: kind 0 its own again, 1 another player's (their name, their profile's textures if any), 2 an NPC's. */
	public static void set(int entityId, int kind, UUID player, Component name, String textures, String signature, int npcId) {
		if (kind == 0) {
			LOOKS.remove(entityId);
			CORPSES.remove(entityId);
			return;
		}
		GameProfile profile = null;
		if (kind == 1) {
			profile = new GameProfile(player, name == null ? "" : name.getString());
			if (textures != null) profile.getProperties().put("textures", new Property("textures", textures, signature));
		}
		CORPSES.remove(entityId);
		LOOKS.put(entityId, new Look(kind == 1 ? player : null, name, profile, kind == 2 ? npcId : -1, System.currentTimeMillis()));
	}

	/** BODY_LOOK kind 3: a body of the dead walked in (CorpseWalk), whose it was, its race ("" a man's), how far gone (CorpseBlock.Rot's ordinal). */
	public static void setCorpse(int entityId, String name, GameProfile profile, int skin, String race, int variant, int tails, int rot) {
		LOOKS.remove(entityId);
		CorpseBlock.Rot[] rots = CorpseBlock.Rot.values();
		Bloodline blood = race.isEmpty() ? null : Bloodline.byId(race);
		CORPSES.put(entityId, new CorpseLook(name, profile, skin, blood != null && blood.isBeast() ? blood : null, variant, Math.max(1, tails),
				rots[Math.max(0, Math.min(rots.length - 1, rot))], System.currentTimeMillis()));
	}

	/** A player's skin as last known, kept from the list, or asked of the skin manager (once) from their profile. */
	private static ResourceLocation playerSkin(UUID id, GameProfile profile) {
		PlayerInfo info = info(id);
		if (info != null) {
			SKINS.put(id, info.getSkinLocation());
			MODELS.put(id, info.getModelName());
			if (info.getCapeLocation() != null) CAPES.put(id, info.getCapeLocation());
			else CAPES.remove(id);
			return info.getSkinLocation();
		}
		if (!SKINS.containsKey(id) && profile != null && ASKED.add(id)) {
			Minecraft.getInstance().getSkinManager().registerSkins(profile, (type, location, texture) -> {
				if (type == MinecraftProfileTexture.Type.SKIN) {
					SKINS.put(id, location);
					String model = texture.getMetadata("model");
					MODELS.put(id, model == null ? "default" : model);
				} else if (type == MinecraftProfileTexture.Type.CAPE) {
					CAPES.put(id, location);
				}
			}, false);
		}
		return SKINS.getOrDefault(id, DefaultPlayerSkin.getDefaultSkin(id));
	}

	private static Look of(Player player) {
		if (LOOKS.isEmpty()) return null;
		Look look = LOOKS.get(player.getId());
		if (look != null && System.currentTimeMillis() - look.at() > EXPIRY_MS) {
			LOOKS.remove(player.getId());
			return null;
		}
		return look;
	}

	/** The body of the dead {@code player} walks in, as this client was last told, or null. */
	private static CorpseLook corpse(Player player) {
		if (CORPSES.isEmpty()) return null;
		CorpseLook look = CORPSES.get(player.getId());
		if (look != null && System.currentTimeMillis() - look.at() > EXPIRY_MS) {
			CORPSES.remove(player.getId());
			return null;
		}
		return look;
	}

	private static PlayerInfo info(UUID id) {
		Minecraft mc = Minecraft.getInstance();
		return id == null || mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(id);
	}

	/** The NPC whose body {@code player} is (this client knows it: set aside, it goes where its body goes), or null. */
	public static CultivatorNpc npc(Player player) {
		Look look = of(player);
		if (look == null || look.npc() < 0 || player.level() == null) return null;
		return player.level().getEntity(look.npc()) instanceof CultivatorNpc npc ? npc : null;
	}

	/** The skin {@code player} is drawn in, or null for their own. */
	public static ResourceLocation skin(AbstractClientPlayer player) {
		CorpseLook dead = corpse(player);
		if (dead != null) {
			if (dead.race() != null) return ClientBloodlines.skinOf(new ClientBloodlines.Look(dead.race(), dead.variant(), dead.tails()));
			if (dead.profile() != null && dead.profile().getId() != null) return playerSkin(dead.profile().getId(), dead.profile());
			return CultivatorNpcRenderer.skinOf(Math.max(0, dead.skin()));
		}
		Look look = of(player);
		if (look == null) return null;
		if (look.player() != null) return playerSkin(look.player(), look.profile());
		CultivatorNpc npc = npc(player);
		return npc == null ? null : CultivatorNpcRenderer.skinFor(npc); // a demon's its race's
	}

	/** The model ("default", "slim") {@code player} is drawn with, or null for their own. NPC cultivators are broad-armed. */
	public static String model(AbstractClientPlayer player) {
		CorpseLook dead = corpse(player);
		if (dead != null) {
			if (dead.race() != null || dead.profile() == null || dead.profile().getId() == null) return "default";
			UUID id = dead.profile().getId();
			PlayerInfo info = info(id);
			return info != null ? info.getModelName() : MODELS.getOrDefault(id, DefaultPlayerSkin.getSkinModelName(id));
		}
		Look look = of(player);
		if (look == null) return null;
		if (look.player() == null) return npc(player) == null ? null : "default";
		PlayerInfo info = info(look.player());
		if (info != null) return info.getModelName();
		return MODELS.getOrDefault(look.player(), DefaultPlayerSkin.getSkinModelName(look.player()));
	}

	/** The player whose body {@code player} is in, or null: their own, an NPC's, or the dead's (ClientBloodlines#owner: whose race it wears). */
	public static UUID bodyPlayer(AbstractClientPlayer player) {
		Look look = of(player);
		return look == null ? null : look.player();
	}

	/** Whether {@code player} wears another's look at all (their cape, then, is the body's or none). */
	public static boolean disguised(AbstractClientPlayer player) {
		return of(player) != null || corpse(player) != null;
	}

	/** The cape of the body {@code player} is in (another player's, if they have one; an NPC and the dead have none). */
	public static ResourceLocation cape(AbstractClientPlayer player) {
		Look look = of(player);
		if (look == null || look.player() == null) return null;
		PlayerInfo info = info(look.player());
		return info != null ? info.getCapeLocation() : CAPES.get(look.player());
	}

	/** The name {@code player} goes by here: the body's, or null for their own (Possession#displayName on this side). */
	public static Component name(Player player) {
		CorpseLook dead = corpse(player);
		if (dead != null) return Component.literal(dead.name());
		Look look = of(player);
		if (look == null) return null;
		if (look.player() != null) {
			if (look.name() != null && !look.name().getString().isEmpty()) return look.name(); // as the server names it, here or away
			PlayerInfo info = info(look.player());
			return info == null ? null : PlayerTeam.formatNameForTeam(info.getTeam(), Component.literal(info.getProfile().getName()));
		}
		CultivatorNpc npc = npc(player);
		return npc == null ? null : Possession.nameOf(npc);
	}

	// --- The dead walking (CorpseWalk) ---

	/** How far gone the body of the dead {@code player} walks in is, or null (in none). */
	public static CorpseBlock.Rot corpseRot(Player player) {
		CorpseLook dead = corpse(player);
		return dead == null ? null : dead.rot();
	}

	/** Whether {@code player} walks in a body of the dead (ClientBloodlines#look: its race then, or a man's look, not theirs). */
	public static boolean isCorpse(Player player) {
		return corpse(player) != null;
	}

	/** The race the body of the dead {@code player} walks in was of, or null (a man's, or none). */
	public static ClientBloodlines.Look corpseRace(Player player) {
		CorpseLook dead = corpse(player);
		return dead == null || dead.race() == null ? null : new ClientBloodlines.Look(dead.race(), dead.variant(), dead.tails());
	}

	/** The colour {@code player}'s body is drawn in, its skin multiplied by it: a body of the dead's, fresh or rotting; null for anyone else. */
	public static float[] tint(Player player) {
		CorpseBlock.Rot rot = corpseRot(player);
		return rot == CorpseBlock.Rot.FRESH ? FRESH : rot == CorpseBlock.Rot.ROTTING ? ROTTING : null;
	}

	/**
	 * Its own hand in first person, for a player walking in a body of the dead (PlayerRendererMixin, once the model is posed): the dead's skin
	 * paled or rotting, or a skeleton's arm once it is bones. False (nothing drawn) for anyone else.
	 */
	public static boolean drawCorpseArm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, ModelPart arm, ModelPart sleeve,
										boolean right) {
		CorpseBlock.Rot rot = corpseRot(player);
		if (rot == null) return false;
		if (rot == CorpseBlock.Rot.BONES) {
			HumanoidModel<AbstractClientPlayer> bones = CorpseBonesRenderer.bonesModel();
			if (bones == null) return false;
			ModelPart bone = right ? bones.rightArm : bones.leftArm;
			bone.copyFrom(arm);
			bone.xRot = 0;
			bone.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CorpseBonesRenderer.SKELETON)), light, OverlayTexture.NO_OVERLAY);
			return true;
		}
		float[] tint = rot == CorpseBlock.Rot.FRESH ? FRESH : ROTTING;
		ResourceLocation skin = player.getSkinTextureLocation();
		arm.xRot = 0;
		arm.render(pose, buffers.getBuffer(RenderType.entitySolid(skin)), light, OverlayTexture.NO_OVERLAY, tint[0], tint[1], tint[2], 1.0f);
		sleeve.xRot = 0;
		sleeve.render(pose, buffers.getBuffer(RenderType.entityTranslucent(skin)), light, OverlayTexture.NO_OVERLAY, tint[0], tint[1], tint[2], 1.0f);
		return true;
	}

	/**
	 * Over a player in an NPC's body, that NPC's plate in place of a name tag (PlayerRendererMixin): its name, title and path, with the
	 * body's health. False (nothing drawn) for anyone else.
	 */
	public static boolean drawPlate(AbstractClientPlayer player, PoseStack pose, MultiBufferSource buffers, int light) {
		CultivatorNpc npc = npc(player);
		if (npc == null) return false;
		float partial = Minecraft.getInstance().getFrameTime();
		CultivatorNpcRenderer.drawPlate(npc, player.getHealth(), player.getMaxHealth(), player.getBbHeight() + 0.5, 1.0f, pose, buffers, light, partial);
		return true;
	}

	/** Disconnect: nothing carries over. */
	public static void clear() {
		LOOKS.clear();
		CORPSES.clear();
		SKINS.clear();
		CAPES.clear();
		MODELS.clear();
		ASKED.clear();
	}

	private ClientBodies() {}
}
