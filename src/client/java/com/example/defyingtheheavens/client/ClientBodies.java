package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.Possession;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Whose look each player wears on this client (the server's BODY_LOOK; Possession): while a nascent soul is in another player's
 * body, that player's skin, model, cape and name; in an NPC cultivator's, its skin and its nameplate. Its own client too: the
 * hands it sees in first person are the body's. Forgotten when the server stops telling it (once a second while it lasts).
 */
public final class ClientBodies {
	private static final long EXPIRY_MS = 3000;

	/** A look: another player's ({@code player}, their name and profile), or an NPC's (its entity id). */
	private record Look(UUID player, Component name, GameProfile profile, int npc, long at) {}

	private static final Map<Integer, Look> LOOKS = new HashMap<>();
	/**
	 * Each body player's skin, model and cape as last known: from the player list while they're in it, else (logged out, their body
	 * held on) from their profile by the skin manager, asked once.
	 */
	private static final Map<UUID, ResourceLocation> SKINS = new HashMap<>(), CAPES = new HashMap<>();
	private static final Map<UUID, String> MODELS = new HashMap<>();
	private static final java.util.Set<UUID> ASKED = new java.util.HashSet<>();

	public static void register() {
		Possession.clientName = ClientBodies::name;
	}

	/** BODY_LOOK: kind 0 its own again, 1 another player's (their name, their profile's textures if any), 2 an NPC's. */
	public static void set(int entityId, int kind, UUID player, Component name, String textures, String signature, int npcId) {
		if (kind == 0) {
			LOOKS.remove(entityId);
			return;
		}
		GameProfile profile = null;
		if (kind == 1) {
			profile = new GameProfile(player, name == null ? "" : name.getString());
			if (textures != null) profile.getProperties().put("textures", new Property("textures", textures, signature));
		}
		LOOKS.put(entityId, new Look(kind == 1 ? player : null, name, profile, kind == 2 ? npcId : -1, System.currentTimeMillis()));
	}

	/** The body player's skin as last known, kept from the list, or asked of the skin manager (once) from their profile. */
	private static ResourceLocation playerSkin(Look look) {
		UUID id = look.player();
		PlayerInfo info = info(id);
		if (info != null) {
			SKINS.put(id, info.getSkinLocation());
			MODELS.put(id, info.getModelName());
			if (info.getCapeLocation() != null) CAPES.put(id, info.getCapeLocation());
			else CAPES.remove(id);
			return info.getSkinLocation();
		}
		if (!SKINS.containsKey(id) && look.profile() != null && ASKED.add(id)) {
			Minecraft.getInstance().getSkinManager().registerSkins(look.profile(), (type, location, texture) -> {
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
		Look look = of(player);
		if (look == null) return null;
		if (look.player() != null) return playerSkin(look);
		CultivatorNpc npc = npc(player);
		return npc == null ? null : CultivatorNpcRenderer.skinFor(npc); // a demon's its race's
	}

	/** The model ("default", "slim") {@code player} is drawn with, or null for their own. NPC cultivators are broad-armed. */
	public static String model(AbstractClientPlayer player) {
		Look look = of(player);
		if (look == null) return null;
		if (look.player() == null) return npc(player) == null ? null : "default";
		PlayerInfo info = info(look.player());
		if (info != null) return info.getModelName();
		return MODELS.getOrDefault(look.player(), DefaultPlayerSkin.getSkinModelName(look.player()));
	}

	/** The player whose body {@code player} is in, or null: their own, or an NPC's (ClientBloodlines#owner: whose race it wears). */
	public static UUID bodyPlayer(AbstractClientPlayer player) {
		Look look = of(player);
		return look == null ? null : look.player();
	}

	/** Whether {@code player} wears another's look at all (their cape, then, is the body's or none). */
	public static boolean disguised(AbstractClientPlayer player) {
		return of(player) != null;
	}

	/** The cape of the body {@code player} is in (another player's, if they have one; an NPC has none). */
	public static ResourceLocation cape(AbstractClientPlayer player) {
		Look look = of(player);
		if (look == null || look.player() == null) return null;
		PlayerInfo info = info(look.player());
		return info != null ? info.getCapeLocation() : CAPES.get(look.player());
	}

	/** The name {@code player} goes by here: the body's, or null for their own (Possession#displayName on this side). */
	public static Component name(Player player) {
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
		SKINS.clear();
		CAPES.clear();
		MODELS.clear();
		ASKED.clear();
	}

	private ClientBodies() {}
}
